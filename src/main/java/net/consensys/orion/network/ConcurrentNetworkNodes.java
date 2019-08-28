/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.orion.network;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.orion.config.Config;
import net.consensys.orion.enclave.sodium.serialization.PublicKeyMapKeyDeserializer;
import net.consensys.orion.enclave.sodium.serialization.PublicKeyMapKeySerializer;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class ConcurrentNetworkNodes implements NetworkNodes {
  private URL url;
  private final CopyOnWriteArrayList<URL> nodeURLs;
  @JsonSerialize(keyUsing = PublicKeyMapKeySerializer.class)
  private final ConcurrentHashMap<Box.PublicKey, URL> nodePKs;

  public ConcurrentNetworkNodes(Config config, Box.PublicKey[] publicKeys) {
    nodeURLs = new CopyOnWriteArrayList<>(config.otherNodes());
    nodePKs = new ConcurrentHashMap<>();
    config.nodeUrl().ifPresent(url -> setNodeUrl(url, publicKeys));
  }

  /**
   * Set the url of the node we are running on. This is useful to do when we are running using default ports, and the
   * construction of this class will be performed before we have settled on what port Orion will be running on.
   * 
   * @param url URL of the node.
   * @param publicKeys PublicKeys used by the node.
   */
  public void setNodeUrl(final URL url, Box.PublicKey[] publicKeys) {
    this.url = url;
    // adding my publickey(s) so /partyinfo returns my info when called.
    addNodeUrl(url);
    for (Box.PublicKey publicKey : publicKeys) {
      nodePKs.put(publicKey, url);
    }
  }

  @JsonCreator
  public ConcurrentNetworkNodes(
      @JsonProperty("url") URL url,
      @JsonProperty("nodeURLs") List<URL> nodeURLs,
      @JsonProperty("nodePKs") @JsonDeserialize(
          keyUsing = PublicKeyMapKeyDeserializer.class) Map<Box.PublicKey, URL> nodePKs) {
    this.url = url;
    this.nodeURLs = new CopyOnWriteArrayList<>(nodeURLs);
    this.nodePKs = new ConcurrentHashMap<>(nodePKs);
  }

  public ConcurrentNetworkNodes(URL url) {
    this(url, new CopyOnWriteArrayList<>(), new ConcurrentHashMap<>());
  }

  /**
   * Add a node's URL and PublcKey to the nodeURLs and nodePKs lists
   *
   * @param nodePk PublicKey of new node
   * @param node URL of new node
   */
  public void addNode(Box.PublicKey nodePk, URL node) {
    this.nodeURLs.add(node);
    this.nodePKs.put(nodePk, node);
  }

  /**
   * Add a url of a node to perform discovery using that node.
   * 
   * @param node URL of new node
   */
  public void addNodeUrl(URL node) {
    this.nodeURLs.add(node);
  }

  @Override
  @JsonProperty("url")
  public URL url() {
    return url;
  }

  @Override
  @JsonProperty("nodeURLs")
  public Collection<URL> nodeURLs() {
    return nodeURLs;
  }

  @Override
  public URL urlForRecipient(Box.PublicKey recipient) {
    return nodePKs.get(recipient);
  }

  @Override
  @JsonProperty("nodePKs")
  public Map<Box.PublicKey, URL> nodePKs() {
    return nodePKs;
  }

  @Override
  public boolean merge(ConcurrentNetworkNodes other) {
    // note; not using map.putAll() as we don't want a malicious peer to overwrite ours nodes.
    boolean thisChanged = false;

    for (Map.Entry<Box.PublicKey, URL> entry : other.nodePKs().entrySet()) {
      if (nodePKs.putIfAbsent(entry.getKey(), entry.getValue()) == null) {
        // putIfAbsent returns null if there was no mapping associated with the provided key
        thisChanged = true;
        nodeURLs.add(entry.getValue());
      }
    }

    return thisChanged;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ConcurrentNetworkNodes that = (ConcurrentNetworkNodes) o;

    return Objects.equals(that.url, url)
        && Objects.equals(nodeURLs, that.nodeURLs)
        && Objects.equals(nodePKs, that.nodePKs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url, nodeURLs, nodePKs);
  }
}

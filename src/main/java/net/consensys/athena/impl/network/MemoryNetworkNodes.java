package net.consensys.athena.impl.network;

import net.consensys.athena.api.config.Config;
import net.consensys.athena.api.network.NetworkNodes;
import net.consensys.athena.impl.enclave.sodium.SodiumPublicKey;
import net.consensys.athena.impl.enclave.sodium.SodiumPublicKeyDeserializer;

import java.net.URL;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class MemoryNetworkNodes implements NetworkNodes {

  private URL url;
  private CopyOnWriteArraySet<URL> nodeURLs;
  private ConcurrentHashMap<PublicKey, URL> nodePKs;

  public MemoryNetworkNodes() {
    nodeURLs = new CopyOnWriteArraySet<>();
    nodePKs = new ConcurrentHashMap<>();
  }

  public MemoryNetworkNodes(Config config) {
    url = config.url();
    if (config.otherNodes().length > 0) {
      nodeURLs = new CopyOnWriteArraySet<>(Arrays.asList(config.otherNodes()));
    } else {
      nodeURLs = new CopyOnWriteArraySet<>();
    }

    nodePKs = new ConcurrentHashMap<>();
  }

  @JsonCreator
  public MemoryNetworkNodes(
      @JsonProperty("url") URL url,
      @JsonProperty("nodeURLs") Set<URL> nodeURLs,
      @JsonProperty("nodePKs") @JsonDeserialize(keyUsing = SodiumPublicKeyDeserializer.class)
          Map<SodiumPublicKey, URL> nodePKs) {
    this.url = url;
    this.nodeURLs = new CopyOnWriteArraySet<>(nodeURLs);
    this.nodePKs = new ConcurrentHashMap<>(nodePKs);
  }

  /**
   * Add a node's URL and PublcKey to the nodeURLs and nodePKs lists
   *
   * @param nodePk PublicKey of new node
   * @param node URL of new node
   */
  public void addNode(PublicKey nodePk, URL node) {
    this.nodeURLs.add(node);
    this.nodePKs.put(nodePk, node);
  }

  @Override
  public URL url() {
    return url;
  }

  @Override
  public Set<URL> nodeURLs() {
    return nodeURLs;
  }

  @Override
  public URL urlForRecipient(PublicKey recipient) {
    return nodePKs.get(recipient);
  }

  @Override
  public Map<PublicKey, URL> nodePKs() {
    return nodePKs;
  }

  @Override
  public boolean merge(NetworkNodes other) {
    // TODO If we don't have the public key of the node, we shouldn't add it, should we ?
    //    for (URL nodeUrl: other.nodeURLs()) {
    //      if (!this.nodeURLs.contains(nodeUrl)) {
    //        nodeURLs.add(nodeUrl);
    //      }
    //    }
    // note; not using map.putAll() as we don't want a malicious peer to overwrite ours nodes.
    // TODO ; how do we manage a node on the network that updates its URL?  do we check certs ?
    boolean thisChanged = false;
    Iterator it = other.nodePKs().entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry pair = (Map.Entry) it.next();
      if (!nodePKs.containsKey(pair.getKey())) {
        thisChanged = true;
        addNode((PublicKey) pair.getKey(), (URL) pair.getValue());
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

    MemoryNetworkNodes that = (MemoryNetworkNodes) o;

    if (url != null ? !url.equals(that.url) : that.url != null) {
      return false;
    }
    if (nodeURLs != null ? !nodeURLs.equals(that.nodeURLs) : that.nodeURLs != null) {
      return false;
    }
    return nodePKs != null ? nodePKs.equals(that.nodePKs) : that.nodePKs == null;
  }

  @Override
  public int hashCode() {
    int result = url != null ? url.hashCode() : 0;
    result = 31 * result + (nodeURLs != null ? nodeURLs.hashCode() : 0);
    result = 31 * result + (nodePKs != null ? nodePKs.hashCode() : 0);
    return result;
  }
}

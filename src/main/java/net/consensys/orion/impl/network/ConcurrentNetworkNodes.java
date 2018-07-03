package net.consensys.orion.impl.network;

import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.network.NetworkNodes;
import net.consensys.orion.impl.enclave.sodium.SodiumPublicKey;
import net.consensys.orion.impl.enclave.sodium.SodiumPublicKeyDeserializer;

import java.net.URL;
import java.security.PublicKey;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class ConcurrentNetworkNodes implements NetworkNodes {

  private final URL url;
  private final CopyOnWriteArrayList<URL> nodeURLs;
  private final ConcurrentHashMap<PublicKey, URL> nodePKs;

  public ConcurrentNetworkNodes(Config config, PublicKey[] publicKeys) {
    url = config.nodeUrl();
    nodeURLs = new CopyOnWriteArrayList<>(config.otherNodes());
    nodePKs = new ConcurrentHashMap<>();

    // adding my publickey(s) so /partyinfo returns my info when called.
    for (PublicKey publicKey : publicKeys) {
      nodePKs.put(publicKey, url);
    }
  }

  @JsonCreator
  public ConcurrentNetworkNodes(
      @JsonProperty("url") URL url,
      @JsonProperty("nodeURLs") List<URL> nodeURLs,
      @JsonProperty("nodePKs") @JsonDeserialize(
          keyUsing = SodiumPublicKeyDeserializer.class) Map<SodiumPublicKey, URL> nodePKs) {
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
  public void addNode(PublicKey nodePk, URL node) {
    this.nodeURLs.add(node);
    this.nodePKs.put(nodePk, node);
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
  public URL urlForRecipient(PublicKey recipient) {
    return nodePKs.get(recipient);
  }

  @Override
  @JsonProperty("nodePKs")
  public Map<PublicKey, URL> nodePKs() {
    return nodePKs;
  }

  @Override
  public boolean merge(ConcurrentNetworkNodes other) {
    // note; not using map.putAll() as we don't want a malicious peer to overwrite ours nodes.
    boolean thisChanged = false;

    for (Map.Entry<PublicKey, URL> entry : other.nodePKs().entrySet()) {
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

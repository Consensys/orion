package net.consensys.athena.impl.network;

import net.consensys.athena.api.config.Config;
import net.consensys.athena.api.network.NetworkNodes;

import java.net.URL;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class MemoryNetworkNodes implements NetworkNodes {

  private URL url;
  private HashSet<URL> nodeURLs;
  private HashMap<URL, PublicKey> nodePKs;

  public MemoryNetworkNodes() {
    nodeURLs = new HashSet<>();
    nodePKs = new HashMap<>();
  }

  public MemoryNetworkNodes(Config config) {
    url = config.url();
    if (config.otherNodes().length > 0) {
      nodeURLs = new HashSet<>(Arrays.asList(config.otherNodes()));
    } else {
      nodeURLs = new HashSet<>();
    }

    nodePKs = new HashMap<>();
  }

  /**
   * Add the URL of a node to the nodeURLs list.
   *
   * @param node URL of new node
   */
  public void addNodeURL(URL node) {
    this.nodeURLs.add(node);
  }

  /**
   * Add a node's URL and PublcKey to the nodeURLs and nodePKs lists
   *
   * @param node URL of new node
   * @param nodePk PublicKey of new node
   */
  public void addNode(URL node, PublicKey nodePk) {
    this.nodeURLs.add(node);
    this.nodePKs.put(node, nodePk);
  }

  @Override
  public URL url() {
    return url;
  }

  @Override
  public HashSet<URL> nodeURLs() {
    return nodeURLs;
  }

  public HashMap<URL, PublicKey> nodePKs() {
    return nodePKs;
  }
}

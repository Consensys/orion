package net.consensys.athena.impl.network;

import net.consensys.athena.api.network.PartyInfo;

import java.net.URL;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.HashSet;

public class MemoryPartyInfo implements PartyInfo {

  private URL url;
  private HashSet<URL> parties = new HashSet<>();
  private HashMap<URL, PublicKey> receipts = new HashMap<>();

  public void setUrl(URL url) {
    this.url = url;
  }

  public void setParties(HashSet<URL> parties) {
    this.parties = parties;
  }

  public void setReceipts(HashMap<URL, PublicKey> receipts) {
    this.receipts = receipts;
  }

  /**
   * Add the URL of a node to the parties list.
   *
   * @param node
   */
  public void addNodeURL(URL node) {
    this.parties.add(node);
  }

  /**
   * Add a node's URL and PublcKey to the parties and receipts lists
   *
   * @param node
   * @param nodePk
   */
  public void addNode(URL node, PublicKey nodePk) {
    this.parties.add(node);
    this.receipts.put(node, nodePk);
  }

  @Override
  public URL url() {
    return url;
  }

  @Override
  public HashSet<URL> parties() {
    return parties;
  }

  public HashMap<URL, PublicKey> receipts() {
    return receipts;
  }
}

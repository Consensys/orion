package net.consensys.athena.api.network;

import java.net.URL;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.HashSet;

/** Details of other nodes on the network */
public interface PartyInfo {

  /**
   * URL of this node
   *
   * @return URL of node
   */
  URL url();

  /**
   * List of URLs of other nodes on the network
   *
   * @return
   */
  HashSet<URL> parties();

  /**
   * List of public keys for other nodes on the network???
   *
   * @return
   */
  HashMap<URL, PublicKey> receipts();
}

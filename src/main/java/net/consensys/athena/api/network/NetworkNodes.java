package net.consensys.athena.api.network;

import java.net.URL;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.HashSet;

/** Details of other nodes on the network */
public interface NetworkNodes {

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
  HashSet<URL> nodeURLs();

  /**
   * Map from URL to public key for all discovered nodes.
   *
   * @return
   */
  HashMap<URL, PublicKey> nodePKs();
}

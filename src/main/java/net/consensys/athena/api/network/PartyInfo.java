package net.consensys.athena.api.network;

import java.net.URL;
import java.util.HashSet;

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
}

package net.consensys.orion.api.network;

import java.net.URL;
import java.security.PublicKey;
import java.util.Map;
import java.util.Set;

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
  Set<URL> nodeURLs();

  URL urlForRecipient(PublicKey recipient);

  /**
   * Map from public key to node for all discovered nodes.
   *
   * @return
   */
  Map<PublicKey, URL> nodePKs();

  boolean merge(NetworkNodes other);
}

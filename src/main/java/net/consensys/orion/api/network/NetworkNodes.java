package net.consensys.orion.api.network;

import net.consensys.orion.impl.network.ConcurrentNetworkNodes;

import java.net.URL;
import java.security.PublicKey;
import java.util.Collection;
import java.util.Map;

/** Details of other nodes on the network */
public interface NetworkNodes {

  /**
   * @return URL of node
   */
  URL url();

  /**
   * @return List of URLs of other nodes on the network
   */
  Collection<URL> nodeURLs();

  URL urlForRecipient(PublicKey recipient);

  /**
   * @return Map from public key to node for all discovered nodes.
   */
  Map<PublicKey, URL> nodePKs();

  boolean merge(ConcurrentNetworkNodes other);
}

package net.consensys.athena.api.network;

import java.io.IOException;

public interface NetworkDiscovery {
  NetworkNodesStatus getNetworkNodeStatuses();

  void doDiscover() throws IOException;

  void doDiscover(long timeout) throws IOException;
}

package net.consensys.athena.api.network;

import java.io.IOException;

public interface NetworkDiscovery {
  NetworkNodes doDiscover() throws IOException;

  NetworkNodes doDiscover(long timeout) throws IOException;
}

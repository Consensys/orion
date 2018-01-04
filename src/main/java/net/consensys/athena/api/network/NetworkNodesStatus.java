package net.consensys.athena.api.network;

import java.net.URL;

public interface NetworkNodesStatus {
  long getLastSeen(URL node);

  void Update(URL node);
}

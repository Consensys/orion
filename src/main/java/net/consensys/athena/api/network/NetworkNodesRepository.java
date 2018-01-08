package net.consensys.athena.api.network;

import java.net.URL;
import java.security.PublicKey;

public interface NetworkNodesRepository extends NetworkNodes {
  void addNodeURL(URL node);

  void addNode(PublicKey nodePk, URL node);
}

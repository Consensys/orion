package net.consensys.athena.api.config;

import java.security.PublicKey;

public interface Node {
  PublicKey defaultKey();

  //  PublicKey selfSendingPublickKey();

  PublicKey[] alwaysSendTo();
}

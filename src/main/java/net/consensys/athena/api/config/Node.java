package net.consensys.athena.api.config;

import java.security.PublicKey;

public interface Node {
  PublicKey defaultPublicKey();

  PublicKey selfSendingPublickKey();

  PublicKey[] alwaysSendTo();
}

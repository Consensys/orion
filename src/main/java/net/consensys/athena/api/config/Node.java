package net.consensys.athena.api.config;

import java.security.PublicKey;

import com.sun.tools.javac.util.Pair;

public interface Node {
  PublicKey defaultPublicKey();

  PublicKey selfSendingPublickKey();

  PublicKey[] alwaysSendTo();

  Pair<String, PublicKey> peers(); // haskell rcpts + parties info
}

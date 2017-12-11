package net.consensys.athena.impl.config;

import net.consensys.athena.api.config.Node;

import java.security.PublicKey;

public class NodeImpl implements Node {

  @Override
  public PublicKey defaultPublicKey() {
    return null;
  }

  @Override
  public PublicKey selfSendingPublickKey() {
    return null;
  }

  @Override
  public PublicKey[] alwaysSendTo() {
    return new PublicKey[0];
  }
}

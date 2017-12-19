package net.consensys.athena.impl.config;

import net.consensys.athena.api.config.Node;

import java.security.PublicKey;

public class NodeImpl implements Node {

  private final PublicKey[] publicKeys;
  private final PublicKey[] alwaysSendTo;

  public NodeImpl(PublicKey[] publicKeys, PublicKey[] alwaysSendTo) {
    this.publicKeys = publicKeys;
    this.alwaysSendTo = alwaysSendTo;
  }

  @Override
  public PublicKey defaultKey() {
    return publicKeys[0];
  }

  @Override
  public PublicKey[] alwaysSendTo() {
    return alwaysSendTo;
  }
}

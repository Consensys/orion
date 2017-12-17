package net.consensys.athena.impl.enclave.sodium;

import java.security.PublicKey;

public class SodiumPublicKey implements PublicKey {

  private byte[] publicKey;

  public SodiumPublicKey(byte[] publicKey) {
    this.publicKey = publicKey;
  }

  @Override
  public String getAlgorithm() {
    return null;
  }

  @Override
  public String getFormat() {
    return null;
  }

  @Override
  public byte[] getEncoded() {
    return publicKey;
  }
}

package net.consensys.athena.impl.enclave.sodium;

import java.security.PublicKey;
import java.util.Arrays;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SodiumPublicKey publicKey1 = (SodiumPublicKey) o;

    return Arrays.equals(publicKey, publicKey1.publicKey);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(publicKey);
  }
}

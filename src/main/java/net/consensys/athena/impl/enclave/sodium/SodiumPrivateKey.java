package net.consensys.athena.impl.enclave.sodium;

import java.security.PrivateKey;
import java.util.Arrays;

public class SodiumPrivateKey implements PrivateKey {

  private byte[] privateKey;

  public SodiumPrivateKey(byte[] privateKey) {

    this.privateKey = privateKey;
  }

  @Override
  public String getAlgorithm() {
    return "sodium";
  }

  @Override
  public String getFormat() {
    return "raw";
  }

  @Override
  public byte[] getEncoded() {
    return privateKey;
  }

  @Override
  public String toString() {
    return "SodiumPrivateKey{" + "privateKey=" + Arrays.toString(privateKey) + '}';
  }
}

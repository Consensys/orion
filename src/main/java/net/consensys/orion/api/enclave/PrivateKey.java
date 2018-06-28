package net.consensys.orion.api.enclave;

import net.consensys.orion.impl.utils.Base64;

import java.util.Arrays;

public class PrivateKey {

  private final byte[] keyBytes;

  public PrivateKey(byte[] keyBytes) {
    this.keyBytes = keyBytes;
  }

  public byte[] toBytes() {
    return keyBytes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PrivateKey that = (PrivateKey) o;

    return Arrays.equals(keyBytes, that.keyBytes);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(keyBytes);
  }

  @Override
  public String toString() {
    return "PrivateKey{" + Base64.encode(keyBytes) + '}';
  }
}

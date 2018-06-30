package net.consensys.orion.api.enclave;

import net.consensys.orion.impl.utils.Base64;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PublicKey {

  private final byte[] keyBytes;

  public PublicKey(String base64) {
    this.keyBytes = Base64.decode(base64);
  }

  @JsonCreator
  public PublicKey(@JsonProperty("encoded") byte[] keyBytes) {
    this.keyBytes = keyBytes;
  }

  @JsonProperty("encoded")
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

    PublicKey publicKey1 = (PublicKey) o;

    return Arrays.equals(keyBytes, publicKey1.keyBytes);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(keyBytes);
  }

  @Override
  public String toString() {
    return Base64.encode(keyBytes);
  }
}

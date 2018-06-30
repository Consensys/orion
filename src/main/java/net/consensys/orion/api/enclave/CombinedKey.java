package net.consensys.orion.api.enclave;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CombinedKey {

  private final byte[] encoded;

  @JsonCreator
  public CombinedKey(@JsonProperty("encoded") byte[] encoded) {
    this.encoded = encoded;
  }

  public byte[] getEncoded() {
    return encoded;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CombinedKey that = (CombinedKey) o;

    return Arrays.equals(encoded, that.encoded);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(encoded);
  }
}

package net.consensys.orion.impl.enclave.sodium;

import net.consensys.orion.api.enclave.CombinedKey;

import java.io.Serializable;
import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SodiumCombinedKey implements CombinedKey, Serializable {

  private final byte[] encoded;

  @JsonCreator
  public SodiumCombinedKey(@JsonProperty("encoded") byte[] encoded) {
    this.encoded = encoded;
  }

  @Override
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

    SodiumCombinedKey that = (SodiumCombinedKey) o;

    return Arrays.equals(encoded, that.encoded);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(encoded);
  }
}

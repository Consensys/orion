package net.consensys.athena.impl.enclave.sodium;

import net.consensys.athena.api.enclave.CombinedKey;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SodiumCombinedKey implements CombinedKey {

  byte[] encoded;

  @JsonCreator
  public SodiumCombinedKey(@JsonProperty("encoded") byte[] encoded) {
    this.encoded = encoded;
  }

  @Override
  public byte[] getEncoded() {
    return encoded;
  }
}

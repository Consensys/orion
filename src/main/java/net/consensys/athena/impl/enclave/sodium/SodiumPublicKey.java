package net.consensys.athena.impl.enclave.sodium;

import java.security.PublicKey;
import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SodiumPublicKey implements PublicKey {

  private byte[] encoded;

  @JsonCreator
  public SodiumPublicKey(@JsonProperty("publicKey") byte[] encoded) {
    this.encoded = encoded;
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

    SodiumPublicKey publicKey1 = (SodiumPublicKey) o;

    return Arrays.equals(encoded, publicKey1.encoded);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(encoded);
  }
}

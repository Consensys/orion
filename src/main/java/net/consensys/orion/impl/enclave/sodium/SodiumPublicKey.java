package net.consensys.orion.impl.enclave.sodium;

import net.consensys.orion.impl.utils.Base64;

import java.security.PublicKey;
import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SodiumPublicKey implements PublicKey {

  private final byte[] encoded;

  public SodiumPublicKey(String base64) {
    this.encoded = Base64.decode(base64);
  }

  @JsonCreator
  public SodiumPublicKey(@JsonProperty("encoded") byte[] encoded) {
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
  public String toString() {
    return Base64.encode(encoded);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(encoded);
  }
}

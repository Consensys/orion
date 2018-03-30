package net.consensys.orion.impl.enclave.sodium.storage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class StoredPrivateKey {
  public static final String UNLOCKED = "unlocked";
  public static final String ARGON2_SBOX = "sodiumargon2sbox";

  private final PrivateKeyData data;
  private final String type;

  @JsonCreator
  public StoredPrivateKey(@JsonProperty("data") PrivateKeyData data, @JsonProperty("type") String type) {
    this.data = data;
    this.type = type;
  }

  @JsonProperty("data")
  public PrivateKeyData data() {
    return data;
  }

  @JsonProperty("type")
  public String type() {
    return type;
  }
}

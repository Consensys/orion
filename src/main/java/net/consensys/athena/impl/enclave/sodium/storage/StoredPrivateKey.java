package net.consensys.athena.impl.enclave.sodium.storage;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StoredPrivateKey {
  public static final String UNLOCKED = "unlocked";
  public static final String ARGON2_SBOX = "sodiumargon2sbox";

  private PrivateKeyData data;
  private String type = UNLOCKED;

  public StoredPrivateKey(PrivateKeyData data) {
    this.data = data;
  }

  public StoredPrivateKey() {}

  @JsonProperty("data")
  public PrivateKeyData data() {
    return data;
  }

  @JsonProperty("data")
  public void data(PrivateKeyData data) {
    this.data = data;
  }

  @JsonProperty("type")
  public String type() {
    return type;
  }

  @JsonProperty("type")
  public void type(String type) {
    this.type = type;
  }
}

package net.consensys.athena.impl.enclave.sodium.storage;

public class StoredPrivateKey {
  public static final String UNLOCKED = "unlocked";

  private PrivateKeyData data;
  private String type = UNLOCKED;

  public StoredPrivateKey(PrivateKeyData data) {
    this.data = data;
  }

  public StoredPrivateKey() {}

  public void setData(PrivateKeyData data) {
    this.data = data;
  }

  public void setType(String type) {
    this.type = type;
  }

  public PrivateKeyData getData() {
    return data;
  }

  public String getType() {
    return type;
  }
}

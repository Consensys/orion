package net.consensys.athena.impl.enclave.sodium.storage;

public class PrivateKeyData {
  private String bytes;

  public PrivateKeyData() {}

  public PrivateKeyData(String bytes) {
    this.bytes = bytes;
  }

  /**
   * Base64 encoded bytes
   *
   * @param bytes Base64 encoded bytes
   */
  public void setBytes(String bytes) {
    this.bytes = bytes;
  }

  /**
   * Base64 encoded bytes
   *
   * @return Base64 encoded bytes
   */
  public String getBytes() {
    return bytes;
  }
}

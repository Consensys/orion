package net.consensys.athena.impl.enclave.sodium.storage;

public class PrivateKeyData {
  private String bytes;
  private String asalt;
  private ArgonOptions aopts;
  private String snonce;
  private String sbox;

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

  public String getAsalt() {
    return asalt;
  }

  public void setAsalt(String asalt) {
    this.asalt = asalt;
  }

  public ArgonOptions getAopts() {
    return aopts;
  }

  public void setAopts(ArgonOptions aopts) {
    this.aopts = aopts;
  }

  public String getSnonce() {
    return snonce;
  }

  public void setSnonce(String snonce) {
    this.snonce = snonce;
  }

  public String getSbox() {
    return sbox;
  }

  public void setSbox(String sbox) {
    this.sbox = sbox;
  }
}

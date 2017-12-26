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

  /** @return The base64 encoded salt to use with Argon */
  public String getAsalt() {
    return asalt;
  }

  public void setAsalt(String asalt) {
    this.asalt = asalt;
  }

  /** @return General options to use with Argon */
  public ArgonOptions getAopts() {
    return aopts;
  }

  public void setAopts(ArgonOptions aopts) {
    this.aopts = aopts;
  }

  /**
   * @return base64 encoded nonce to use with the secret box encryption, using the password
   *     generated key.
   */
  public String getSnonce() {
    return snonce;
  }

  public void setSnonce(String snonce) {
    this.snonce = snonce;
  }

  /** @return base64 encoded Secret box containing the password protected private key. */
  public String getSbox() {
    return sbox;
  }

  public void setSbox(String sbox) {
    this.sbox = sbox;
  }
}

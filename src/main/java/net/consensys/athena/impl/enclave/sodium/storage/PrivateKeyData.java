package net.consensys.athena.impl.enclave.sodium.storage;

import com.fasterxml.jackson.annotation.JsonProperty;

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
  @JsonProperty("bytes")
  public void bytes(String bytes) {
    this.bytes = bytes;
  }

  /**
   * Base64 encoded bytes
   *
   * @return Base64 encoded bytes
   */
  @JsonProperty("bytes")
  public String bytes() {
    return bytes;
  }

  /** @return The base64 encoded salt to use with Argon */
  @JsonProperty("asalt")
  public String asalt() {
    return asalt;
  }

  @JsonProperty("asalt")
  public void asalt(String asalt) {
    this.asalt = asalt;
  }

  /** @return General options to use with Argon */
  @JsonProperty("aopts")
  public ArgonOptions aopts() {
    return aopts;
  }

  @JsonProperty("aopts")
  public void aopts(ArgonOptions aopts) {
    this.aopts = aopts;
  }

  /**
   * @return base64 encoded nonce to use with the secret box encryption, using the password
   *     generated key.
   */
  @JsonProperty("snonce")
  public String snonce() {
    return snonce;
  }

  @JsonProperty("snonce")
  public void snonce(String snonce) {
    this.snonce = snonce;
  }

  /** @return base64 encoded Secret box containing the password protected private key. */
  @JsonProperty("sbox")
  public String sbox() {
    return sbox;
  }

  @JsonProperty("sbox")
  public void sbox(String sbox) {
    this.sbox = sbox;
  }
}

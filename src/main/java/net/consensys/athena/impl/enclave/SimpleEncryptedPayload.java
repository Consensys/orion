package net.consensys.athena.impl.enclave;

import net.consensys.athena.api.enclave.CombinedKey;
import net.consensys.athena.api.enclave.EncryptedPayload;

import java.io.Serializable;
import java.security.PublicKey;

public class SimpleEncryptedPayload implements EncryptedPayload, Serializable {

  private byte[] combinedKeyNonce;
  private PublicKey sender;
  private byte[] cipherText;
  private byte[] nonce;
  private CombinedKey[] combinedKeys;

  public SimpleEncryptedPayload(
      PublicKey sender,
      byte[] nonce,
      byte[] combinedKeyNonce,
      CombinedKey[] combinedKeys,
      byte[] cipherText) {
    this.combinedKeyNonce = combinedKeyNonce;
    this.sender = sender;
    this.cipherText = cipherText;
    this.nonce = nonce;
    this.combinedKeys = combinedKeys;
  }

  @Override
  public PublicKey getSender() {
    return sender;
  }

  @Override
  public byte[] getCipherText() {
    return cipherText;
  }

  @Override
  public byte[] getNonce() {
    return nonce;
  }

  @Override
  public CombinedKey[] getCombinedKeys() {
    return combinedKeys;
  }

  @Override
  public byte[] getCombinedKeyNonce() {
    return combinedKeyNonce;
  }
}

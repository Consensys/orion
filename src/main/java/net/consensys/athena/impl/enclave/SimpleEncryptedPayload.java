package net.consensys.athena.impl.enclave;

import net.consensys.athena.api.enclave.CombinedKey;
import net.consensys.athena.api.enclave.EnclaveException;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.impl.enclave.sodium.SodiumCombinedKey;
import net.consensys.athena.impl.enclave.sodium.SodiumPublicKey;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SimpleEncryptedPayload implements EncryptedPayload, Serializable {

  private byte[] combinedKeyNonce;
  private SodiumPublicKey sender;
  private byte[] cipherText;
  private byte[] nonce;
  private SodiumCombinedKey[] combinedKeys;

  @JsonIgnore private Map<PublicKey, Integer> combinedKeysOwners;

  @JsonCreator
  public SimpleEncryptedPayload(
      @JsonProperty("sender") SodiumPublicKey sender,
      @JsonProperty("nonce") byte[] nonce,
      @JsonProperty("combinedKeyNonce") byte[] combinedKeyNonce,
      @JsonProperty("combinedKeys") SodiumCombinedKey[] combinedKeys,
      @JsonProperty("cipherText") byte[] cipherText) {
    this.combinedKeyNonce = combinedKeyNonce;
    this.sender = sender;
    this.cipherText = cipherText;
    this.nonce = nonce;
    this.combinedKeys = combinedKeys;
    this.combinedKeysOwners = new HashMap<>();
  }

  public SimpleEncryptedPayload(
      SodiumPublicKey sender,
      byte[] nonce,
      byte[] combinedKeyNonce,
      SodiumCombinedKey[] combinedKeys,
      byte[] cipherText,
      Map<PublicKey, Integer> combinedKeysOwners) {
    this.combinedKeyNonce = combinedKeyNonce;
    this.sender = sender;
    this.cipherText = cipherText;
    this.nonce = nonce;
    this.combinedKeys = combinedKeys;
    this.combinedKeysOwners = combinedKeysOwners;
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

  @Override
  public EncryptedPayload stripFor(PublicKey key) {
    Integer toKeepIdx = combinedKeysOwners.get(key);
    if (toKeepIdx == null || toKeepIdx < 0 || toKeepIdx >= combinedKeys.length) {
      throw new EnclaveException("can't strip encrypted payload for provided key");
    }

    return new SimpleEncryptedPayload(
        sender,
        nonce,
        combinedKeyNonce,
        new SodiumCombinedKey[] {combinedKeys[toKeepIdx]},
        cipherText);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SimpleEncryptedPayload that = (SimpleEncryptedPayload) o;
    return Arrays.equals(combinedKeyNonce, that.combinedKeyNonce)
        && Objects.equals(sender, that.sender)
        && Arrays.equals(cipherText, that.cipherText)
        && Arrays.equals(nonce, that.nonce)
        && Arrays.equals(combinedKeys, that.combinedKeys);
  }
}

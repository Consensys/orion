package net.consensys.orion.impl.helpers;

import net.consensys.orion.api.enclave.Enclave;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.enclave.HashAlgorithm;
import net.consensys.orion.impl.enclave.sodium.SodiumCombinedKey;
import net.consensys.orion.impl.enclave.sodium.SodiumEncryptedPayload;
import net.consensys.orion.impl.enclave.sodium.SodiumPublicKey;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/*
 * A very simple test class that implements the enclave interface and does minimal encryption operations that do not do
 * much at all.
 */
public class StubEnclave implements Enclave {

  private final SodiumPublicKey[] alwaysSendTo;
  private final SodiumPublicKey[] nodeKeys;

  public StubEnclave(SodiumPublicKey[] alwaysSendTo, SodiumPublicKey[] nodeKeys) {
    this.alwaysSendTo = alwaysSendTo;
    this.nodeKeys = nodeKeys;
  }

  public StubEnclave() {
    this.alwaysSendTo = new SodiumPublicKey[0];
    this.nodeKeys = new SodiumPublicKey[0];
  }

  @Override
  public PublicKey[] alwaysSendTo() {
    return alwaysSendTo;
  }

  @Override
  public PublicKey[] nodeKeys() {
    return nodeKeys;
  }

  @Override
  public byte[] digest(HashAlgorithm algorithm, byte[] input) {
    byte[] hash = new byte[256];
    for (int i = 0; i < input.length && i < 256; i++) {
      hash[i] = input[i];
    }
    return hash;
  }

  @Override
  public byte[] decrypt(EncryptedPayload ciphertextAndMetadata, PublicKey publicKey) {
    byte[] cipherText = ciphertextAndMetadata.cipherText();
    byte[] plainText = new byte[cipherText.length];
    for (int i = 0; i < cipherText.length; i++) {
      byte b = cipherText[i];
      plainText[i] = (byte) (b - 10);
    }
    return plainText;
  }

  @Override
  public PublicKey readKey(String b64) {
    return new SodiumPublicKey(Base64.getDecoder().decode(b64.getBytes(StandardCharsets.UTF_8)));
  }

  @Override
  public EncryptedPayload encrypt(byte[] plaintext, PublicKey senderKey, PublicKey[] recipients) {

    byte[] ciphterText = new byte[plaintext.length];
    for (int i = 0; i < plaintext.length; i++) {
      byte b = plaintext[i];
      ciphterText[i] = (byte) (b + 10);
    }

    byte[] combinedKeyNonce = {};
    byte[] nonce = {};

    SodiumCombinedKey[] combinedKeys;
    Map<SodiumPublicKey, Integer> combinedKeysOwners = new HashMap<>();

    if (recipients != null && recipients.length > 0) {
      combinedKeys = new SodiumCombinedKey[recipients.length];
      for (int i = 0; i < recipients.length; i++) {
        combinedKeysOwners.put((SodiumPublicKey) recipients[i], i);
        combinedKeys[i] = new SodiumCombinedKey(recipients[i].getEncoded());
      }

    } else {
      combinedKeys = new SodiumCombinedKey[0];
    }
    return new SodiumEncryptedPayload(
        (SodiumPublicKey) senderKey,
        nonce,
        combinedKeyNonce,
        combinedKeys,
        ciphterText,
        Optional.of(combinedKeysOwners));
  }
}

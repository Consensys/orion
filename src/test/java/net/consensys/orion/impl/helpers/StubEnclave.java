package net.consensys.orion.impl.helpers;

import net.consensys.orion.api.enclave.CombinedKey;
import net.consensys.orion.api.enclave.Enclave;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.enclave.PublicKey;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/*
 * A very simple test class that implements the enclave interface and does minimal encryption operations that do not do
 * much at all.
 */
public class StubEnclave implements Enclave {

  private final PublicKey[] alwaysSendTo;
  private final PublicKey[] nodeKeys;

  public StubEnclave() {
    this.alwaysSendTo = new PublicKey[0];
    this.nodeKeys = new PublicKey[0];
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
    return new PublicKey(Base64.getDecoder().decode(b64.getBytes(StandardCharsets.UTF_8)));
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

    CombinedKey[] combinedKeys;
    Map<PublicKey, Integer> combinedKeysOwners = new HashMap<>();

    if (recipients != null && recipients.length > 0) {
      combinedKeys = new CombinedKey[recipients.length];
      for (int i = 0; i < recipients.length; i++) {
        combinedKeysOwners.put(recipients[i], i);
        combinedKeys[i] = new CombinedKey(recipients[i].toBytes());
      }
    } else {
      combinedKeys = new CombinedKey[0];
    }
    return new EncryptedPayload(senderKey, nonce, combinedKeyNonce, combinedKeys, ciphterText, combinedKeysOwners);
  }
}

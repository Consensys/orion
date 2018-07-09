/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package net.consensys.orion.impl.enclave.sodium;

import static java.nio.charset.StandardCharsets.UTF_8;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.crypto.sodium.Box.SecretKey;
import net.consensys.cava.crypto.sodium.SecretBox;
import net.consensys.cava.crypto.sodium.SodiumException;
import net.consensys.orion.api.enclave.CombinedKey;
import net.consensys.orion.api.enclave.Enclave;
import net.consensys.orion.api.enclave.EnclaveException;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.enclave.KeyStore;
import net.consensys.orion.api.exception.OrionErrorCode;

import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;

public class SodiumEnclave implements Enclave {

  private KeyStore keyStore;

  private final Box.PublicKey[] alwaysSendTo;
  private final Box.PublicKey[] nodeKeys;

  public SodiumEnclave(KeyStore keyStore) {
    this.keyStore = keyStore;
    this.alwaysSendTo = keyStore.alwaysSendTo();
    this.nodeKeys = keyStore.nodeKeys();
  }

  @Override
  public EncryptedPayload encrypt(byte[] plaintext, Box.PublicKey senderKey, Box.PublicKey[] recipients) {
    // encrypt plaintext with a random key & nonce
    SecretBox.Key payloadKey = SecretBox.Key.random();
    SecretBox.Nonce secretNonce = SecretBox.Nonce.random();
    byte[] cipherText = SecretBox.encrypt(plaintext, payloadKey, secretNonce);

    // encrypt payloadKey with public key of each recipient
    Box.SecretKey senderSecretKey = privateKey(senderKey);
    final Box.PublicKey[] recipientsAndSender = addSenderToRecipients(recipients, senderKey);
    Box.Nonce nonce = Box.Nonce.random();
    final CombinedKey[] combinedKeys =
        encryptPayloadKeyForRecipients(payloadKey, recipientsAndSender, senderSecretKey, nonce);

    return new EncryptedPayload(
        senderKey,
        secretNonce.bytesArray(),
        nonce.bytesArray(),
        combinedKeys,
        cipherText,
        combinedKeysMapping(recipientsAndSender));
  }

  @Override
  public byte[] decrypt(EncryptedPayload ciphertextAndMetadata, Box.PublicKey identity) {
    Box.SecretKey secretKey = privateKey(identity);
    SecretBox.Key key = decryptPayloadKey(ciphertextAndMetadata, secretKey);
    SecretBox.Nonce nonce = SecretBox.Nonce.fromBytes(ciphertextAndMetadata.nonce());
    return SecretBox.decrypt(ciphertextAndMetadata.cipherText(), key, nonce);
  }

  @Override
  public Box.PublicKey[] alwaysSendTo() {
    return alwaysSendTo;
  }

  @Override
  public Box.PublicKey[] nodeKeys() {
    return nodeKeys;
  }

  @Override
  public Box.PublicKey readKey(String b64) {
    try {
      return Box.PublicKey.fromBytes(Base64.getDecoder().decode(b64.getBytes(UTF_8)));
    } catch (final IllegalArgumentException e) {
      throw new EnclaveException(OrionErrorCode.ENCLAVE_DECODE_PUBLIC_KEY, e);
    }
  }

  private Box.PublicKey[] addSenderToRecipients(final Box.PublicKey[] recipients, final Box.PublicKey sender) {
    final Box.PublicKey[] recipientsAndSender = Arrays.copyOf(recipients, recipients.length + 1);
    recipientsAndSender[recipients.length] = sender;
    return recipientsAndSender;
  }

  private Box.SecretKey privateKey(Box.PublicKey identity) {
    SecretKey secretKey = keyStore.privateKey(identity);
    if (secretKey == null) {
      throw new EnclaveException(
          OrionErrorCode.ENCLAVE_NO_MATCHING_PRIVATE_KEY,
          "No StoredPrivateKey found in keystore");
    }
    return secretKey;
  }

  // Iterate through the combined keys to find one that decrypts successfully using our secret key.
  private SecretBox.Key decryptPayloadKey(EncryptedPayload ciphertextAndMetadata, Box.SecretKey secretKey) {
    SodiumException problem = null;

    // Try each key until we find one that successfully decrypts or we run out of keys
    for (final CombinedKey key : ciphertextAndMetadata.combinedKeys()) {
      byte[] clearText;
      try {
        Box.PublicKey senderPublicKey = ciphertextAndMetadata.sender();
        Box.Nonce nonce = Box.Nonce.fromBytes(ciphertextAndMetadata.combinedKeyNonce());

        // When decryption with the combined fails, SodiumLibrary exceptions
        clearText = Box.decrypt(key.getEncoded(), senderPublicKey, secretKey, nonce);
      } catch (final SodiumException e) {
        // The next next key might be the lucky one, so don't propagate just yet
        problem = e;
        continue;
      }
      if (clearText != null) {
        return SecretBox.Key.fromBytes(clearText);
      }
    }

    // No more keys left to try, finally propagate the issue
    throw new EnclaveException(OrionErrorCode.ENCLAVE_DECRYPT_WRONG_PRIVATE_KEY, problem);
  }


  /** Create mapping between combined keys and recipients */
  private HashMap<Box.PublicKey, Integer> combinedKeysMapping(Box.PublicKey[] recipients) {
    final HashMap<Box.PublicKey, Integer> combinedKeysMapping = new HashMap<>();
    for (int i = 0; i < recipients.length; i++) {
      combinedKeysMapping.put(recipients[i], i);
    }
    return combinedKeysMapping;
  }

  private CombinedKey[] encryptPayloadKeyForRecipients(
      SecretBox.Key payloadKey,
      Box.PublicKey[] recipients,
      Box.SecretKey senderSecretKey,
      Box.Nonce nonce) {
    byte[] message = payloadKey.bytesArray();

    final CombinedKey[] combinedKeys = new CombinedKey[recipients.length];
    for (int i = 0; i < recipients.length; i++) {
      Box.PublicKey recipientPublicKey = recipients[i];

      byte[] encryptedKey;
      try {
        encryptedKey = Box.encrypt(message, recipientPublicKey, senderSecretKey, nonce);
      } catch (SodiumException e) {
        throw new EnclaveException(OrionErrorCode.ENCLAVE_ENCRYPT_COMBINE_KEYS, e);
      }

      combinedKeys[i] = new CombinedKey(encryptedKey);
    }

    return combinedKeys;
  }
}

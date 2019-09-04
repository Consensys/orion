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
package net.consensys.orion.enclave.sodium;

import static java.nio.charset.StandardCharsets.UTF_8;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.crypto.Hash;
import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.crypto.sodium.Box.SecretKey;
import net.consensys.cava.crypto.sodium.SecretBox;
import net.consensys.cava.crypto.sodium.SecretBox.Nonce;
import net.consensys.cava.crypto.sodium.SodiumException;
import net.consensys.cava.rlp.RLP;
import net.consensys.orion.enclave.Enclave;
import net.consensys.orion.enclave.EnclaveException;
import net.consensys.orion.enclave.EncryptedKey;
import net.consensys.orion.enclave.EncryptedPayload;
import net.consensys.orion.enclave.KeyStore;
import net.consensys.orion.enclave.PrivacyGroupPayload;
import net.consensys.orion.exception.OrionErrorCode;

import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class SodiumEnclave implements Enclave {
  private static final Nonce ZERO_NONCE = Nonce.fromBytes(new byte[Nonce.length()]);

  private final KeyStore keyStore;

  private final Box.PublicKey[] alwaysSendTo;
  private final Box.PublicKey[] nodeKeys;

  public SodiumEnclave(final KeyStore keyStore) {
    this.keyStore = keyStore;
    this.alwaysSendTo = keyStore.alwaysSendTo();
    this.nodeKeys = keyStore.nodeKeys();
  }

  @Override
  public EncryptedPayload encrypt(
      final byte[] plaintext,
      final Box.PublicKey senderKey,
      final Box.PublicKey[] recipients,
      final byte[] seed) {
    // encrypt plaintext with a random key
    final SecretBox.Key payloadKey = SecretBox.Key.random();
    // use a zero nonce, as the key is random
    final byte[] cipherText = SecretBox.encrypt(plaintext, payloadKey, ZERO_NONCE);

    // encrypt payloadKey with public key of each recipient
    final Box.SecretKey senderSecretKey = privateKey(senderKey);
    final Box.PublicKey[] recipientsAndSender = addSenderToRecipients(recipients, senderKey);
    final Box.Nonce nonce = Box.Nonce.random();
    final EncryptedKey[] encryptedKeys =
        encryptPayloadKeyForRecipients(payloadKey, recipientsAndSender, senderSecretKey, nonce);

    final byte[] privacyGroupId = generatePrivacyGroupId(recipientsAndSender, seed, PrivacyGroupPayload.Type.PANTHEON);

    return new EncryptedPayload(
        senderKey,
        nonce.bytesArray(),
        encryptedKeys,
        cipherText,
        encryptedKeysMapping(recipientsAndSender),
        privacyGroupId);
  }

  @Override
  public byte[] generatePrivacyGroupId(
      final Box.PublicKey[] recipientsAndSender,
      final byte[] seed,
      final PrivacyGroupPayload.Type type) {
    final List<byte[]> recipientsAndSenderList = Arrays
        .stream(recipientsAndSender)
        .distinct()
        .sorted(Comparator.comparing(Box.PublicKey::hashCode))
        .map(Box.PublicKey::bytesArray)
        .collect(Collectors.toList());


    if (seed != null && type.equals(PrivacyGroupPayload.Type.PANTHEON)) {
      recipientsAndSenderList.add(seed);
    }

    final Bytes rlpEncoded = RLP.encodeList(listWriter -> recipientsAndSenderList.forEach(listWriter::writeByteArray));

    return Hash.keccak256(rlpEncoded).toArray();
  }

  @Override
  public byte[] decrypt(final EncryptedPayload ciphertextAndMetadata, final Box.PublicKey identity) {
    final Box.SecretKey secretKey = privateKey(identity);
    final SecretBox.Key key = decryptPayloadKey(ciphertextAndMetadata, secretKey);
    return SecretBox.decrypt(ciphertextAndMetadata.cipherText(), key, ZERO_NONCE);
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
  public Box.PublicKey readKey(final String b64) {
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

  private Box.SecretKey privateKey(final Box.PublicKey identity) {
    final SecretKey secretKey = keyStore.privateKey(identity);
    if (secretKey == null) {
      throw new EnclaveException(
          OrionErrorCode.ENCLAVE_NO_MATCHING_PRIVATE_KEY,
          "No StoredPrivateKey found in keystore");
    }
    return secretKey;
  }

  // Iterate through the encrypted keys to find one that decrypts successfully using our secret key.
  private SecretBox.Key decryptPayloadKey(final EncryptedPayload ciphertextAndMetadata, final Box.SecretKey secretKey) {
    SodiumException problem = null;

    // Try each key until we find one that successfully decrypts or we run out of keys
    for (final EncryptedKey key : ciphertextAndMetadata.encryptedKeys()) {
      final byte[] clearText;
      try {
        final Box.PublicKey senderPublicKey = ciphertextAndMetadata.sender();
        final Box.Nonce nonce = Box.Nonce.fromBytes(ciphertextAndMetadata.nonce());
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

  /** Create mapping between encrypted keys and recipients */
  private HashMap<Box.PublicKey, Integer> encryptedKeysMapping(final Box.PublicKey[] recipients) {
    final HashMap<Box.PublicKey, Integer> encryptedKeysMapping = new HashMap<>();
    for (int i = 0; i < recipients.length; i++) {
      encryptedKeysMapping.put(recipients[i], i);
    }
    return encryptedKeysMapping;
  }

  private EncryptedKey[] encryptPayloadKeyForRecipients(
      final SecretBox.Key payloadKey,
      final Box.PublicKey[] recipients,
      final Box.SecretKey senderSecretKey,
      final Box.Nonce nonce) {
    final byte[] message = payloadKey.bytesArray();
    try {
      final EncryptedKey[] encryptedKeys = new EncryptedKey[recipients.length];
      for (int i = 0; i < recipients.length; i++) {
        final Box.PublicKey recipientPublicKey = recipients[i];

        final byte[] encryptedKey;
        try {
          encryptedKey = Box.encrypt(message, recipientPublicKey, senderSecretKey, nonce);
        } catch (final SodiumException e) {
          throw new EnclaveException(OrionErrorCode.ENCLAVE_ENCRYPT_COMBINE_KEYS, e);
        }

        encryptedKeys[i] = new EncryptedKey(encryptedKey);
      }
      return encryptedKeys;
    } finally {
      // ensure key material is overwritten
      Arrays.fill(message, (byte) 0);
    }
  }
}

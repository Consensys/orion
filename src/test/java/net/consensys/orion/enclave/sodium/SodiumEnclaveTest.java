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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.orion.enclave.EnclaveException;
import net.consensys.orion.enclave.EncryptedKey;
import net.consensys.orion.enclave.EncryptedPayload;
import net.consensys.orion.exception.OrionErrorCode;

import java.security.Security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SodiumEnclaveTest {

  private final MemoryKeyStore keyStore = new MemoryKeyStore();
  private SodiumEnclave enclave;

  static {
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
  }

  @BeforeEach
  void setUp() {
    enclave = new SodiumEnclave(keyStore);
  }

  @Test
  void recipientEncryptDecrypt() {
    final Box.PublicKey senderKey = keyStore.generateKeyPair();
    final Box.PublicKey recipientKey = keyStore.generateKeyPair();
    final String plaintext = "hello again";

    final EncryptedPayload encryptedPayload = encrypt(plaintext, senderKey, recipientKey);
    final String decrypted = decrypt(encryptedPayload, recipientKey);

    assertEquals(plaintext, decrypted);
  }

  @Test
  /* Sender can decrypt the cipher text for their encrypted plaint text. */
  void senderEncryptDecrypt() {
    final Box.PublicKey senderKey = keyStore.generateKeyPair();
    final String plaintext = "the original message";

    final EncryptedPayload encryptedPayload = encrypt(plaintext, senderKey);
    final String decryptedPlainText = decrypt(encryptedPayload, senderKey);

    assertEquals(plaintext, decryptedPlainText);
  }

  @Test
  /* Sender decryption must not be affected by the presence of other encrypted keys (recipients) */
  void senderEncryptDecryptWithRecipients() {
    final Box.PublicKey senderKey = keyStore.generateKeyPair();
    final Box.PublicKey recipientAKey = keyStore.generateKeyPair();
    final Box.PublicKey recipientBKey = keyStore.generateKeyPair();
    final String plaintext = "the other original message";

    final EncryptedPayload encryptedPayload = encrypt(plaintext, senderKey, recipientAKey, recipientBKey);
    final String decryptedPlainText = decrypt(encryptedPayload, senderKey);

    assertEquals(plaintext, decryptedPlainText);
  }

  @Test
  void encryptThrowsExceptionWhenMissingKey() {
    final Box.PublicKey fake = Box.KeyPair.random().publicKey();
    final Box.PublicKey recipientKey = keyStore.generateKeyPair();

    EnclaveException e = assertThrows(EnclaveException.class, () -> encrypt("plaintext", fake, recipientKey));
    assertEquals("No StoredPrivateKey found in keystore", e.getMessage());
  }

  @Test
  void decryptThrowsExceptionWhenMissingKey() {
    final Box.PublicKey fake = Box.KeyPair.random().publicKey();
    final Box.PublicKey sender = keyStore.generateKeyPair();

    EnclaveException e = assertThrows(EnclaveException.class, () -> {
      final EncryptedPayload payload =
          new EncryptedPayload(sender, new byte[] {}, new EncryptedKey[] {}, new byte[] {}, new byte[0]);
      enclave.decrypt(payload, fake);
    });
    assertEquals("No StoredPrivateKey found in keystore", e.getMessage());
  }

  @Test
  void encryptDecryptNoEncryptedKeys() {
    final Box.PublicKey senderKey = keyStore.generateKeyPair();
    final Box.PublicKey recipientKey = keyStore.generateKeyPair();

    final EncryptedPayload encryptedPayload = encrypt("hello", senderKey, recipientKey);

    final EncryptedPayload payload = new EncryptedPayload(
        encryptedPayload.sender(),
        encryptedPayload.nonce(),
        new EncryptedKey[] {},
        encryptedPayload.cipherText(),
        new byte[0]);

    assertThrows(EnclaveException.class, () -> decrypt(payload, recipientKey));
  }

  @Test
  void encryptDecryptBadNonce() {
    final Box.PublicKey senderKey = keyStore.generateKeyPair();
    final Box.PublicKey recipientKey = keyStore.generateKeyPair();

    final EncryptedPayload encryptedPayload = encrypt("hello", senderKey, recipientKey);

    final EncryptedPayload payload = new EncryptedPayload(
        encryptedPayload.sender(),
        new byte[0],
        encryptedPayload.encryptedKeys(),
        encryptedPayload.cipherText(),
        new byte[0]);

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> decrypt(payload, recipientKey));
    assertEquals("nonce must be 24 bytes, got 0", e.getMessage());
  }

  @Test
  void payloadCanOnlyBeDecryptedByItsKey() {
    final Box.PublicKey senderKey = keyStore.generateKeyPair();
    final Box.PublicKey recipientKey1 = keyStore.generateKeyPair();
    final Box.PublicKey recipientKey2 = keyStore.generateKeyPair();
    final String plaintext = "hello";

    final EncryptedPayload encryptedPayload1 = encrypt(plaintext, senderKey, recipientKey1);

    // trying to decrypt payload1 with recipient2 key
    EnclaveException e = assertThrows(EnclaveException.class, () -> decrypt(encryptedPayload1, recipientKey2));
    assertEquals(OrionErrorCode.ENCLAVE_DECRYPT_WRONG_PRIVATE_KEY, e.code());
  }

  @Test
  void encryptGeneratesDifferentCipherForSamePayloadAndKey() {
    final Box.PublicKey senderKey = keyStore.generateKeyPair();
    final Box.PublicKey recipientKey = keyStore.generateKeyPair();
    final String plaintext = "hello";

    final EncryptedPayload encryptedPayload1 = encrypt(plaintext, senderKey, recipientKey);
    final EncryptedPayload encryptedPayload2 = encrypt(plaintext, senderKey, recipientKey);

    assertNotEquals(encryptedPayload1.cipherText(), encryptedPayload2.cipherText());
  }

  private String decrypt(EncryptedPayload encryptedPayload, Box.PublicKey senderKey) {
    return new String(enclave.decrypt(encryptedPayload, senderKey), UTF_8);
  }

  private EncryptedPayload encrypt(String plaintext, Box.PublicKey senderKey, Box.PublicKey... recipientKey) {
    return enclave.encrypt(plaintext.getBytes(UTF_8), senderKey, recipientKey, null);
  }
}

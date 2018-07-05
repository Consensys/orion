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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.api.enclave.EnclaveException;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.enclave.KeyConfig;
import net.consensys.orion.api.enclave.KeyStore;
import net.consensys.orion.api.exception.OrionErrorCode;

import java.nio.file.Path;
import java.security.PublicKey;
import java.util.Optional;

import com.muquit.libsodiumjna.SodiumKeyPair;
import com.muquit.libsodiumjna.SodiumLibrary;
import com.muquit.libsodiumjna.exceptions.SodiumLibraryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class LibSodiumEnclaveTest {

  private final KeyStore memoryKeyStore = new SodiumMemoryKeyStore();
  private LibSodiumEnclave enclave;

  @BeforeEach
  void setUp() {
    SodiumLibrary.setLibraryPath(LibSodiumSettings.defaultLibSodiumPath());
    enclave = new LibSodiumEnclave(memoryKeyStore);
  }

  @Test
  void version() {
    assertTrue(!SodiumLibrary.libsodiumVersionString().isEmpty());
  }

  @Test
  void sodiumLoads() throws SodiumLibraryException {
    final int nonceBytesLength = SodiumLibrary.cryptoBoxNonceBytes().intValue();
    final byte[] nonce = SodiumLibrary.randomBytes(nonceBytesLength);
    final SodiumKeyPair senderPair = SodiumLibrary.cryptoBoxKeyPair();
    final SodiumKeyPair recipientPair = SodiumLibrary.cryptoBoxKeyPair();

    final byte[] message = "hello".getBytes(UTF_8);
    assertEncryptDecrypt(nonce, senderPair, recipientPair, message);

    final byte[] secretKey = SodiumLibrary.randomBytes(SodiumLibrary.cryptoSecretBoxKeyBytes().intValue());
    assertEncryptDecrypt(nonce, senderPair, recipientPair, secretKey);
  }

  @Test
  void recipientEncryptDecrypt(@TempDirectory Path tempDir) {
    final PublicKey senderKey = generateKey(tempDir);
    final PublicKey recipientKey = generateKey(tempDir);
    final String plaintext = "hello again";

    final EncryptedPayload encryptedPayload = encrypt(plaintext, senderKey, recipientKey);
    final String decrypted = decrypt(encryptedPayload, recipientKey);

    assertEquals(plaintext, decrypted);
  }

  @Test
  /* Sender can decrypt the cipher text for their encrypted plaint text. */
  void senderEncryptDecrypt(@TempDirectory Path tempDir) {
    final PublicKey senderKey = generateKey(tempDir);
    final String plaintext = "the original message";

    final EncryptedPayload encryptedPayload = encrypt(plaintext, senderKey);
    final String decryptedPlainText = decrypt(encryptedPayload, senderKey);

    assertEquals(plaintext, decryptedPlainText);
  }

  @Test
  /* Sender decryption must not be affected by the presence of other combined keys (recipients) */
  void senderEncryptDecryptWithRecipients(@TempDirectory Path tempDir) {
    final PublicKey senderKey = generateKey(tempDir);
    final PublicKey recipientAKey = generateKey(tempDir);
    final PublicKey recipientBKey = generateKey(tempDir);
    final String plaintext = "the other original message";

    final EncryptedPayload encryptedPayload = encrypt(plaintext, senderKey, recipientAKey, recipientBKey);
    final String decryptedPlainText = decrypt(encryptedPayload, senderKey);

    assertEquals(plaintext, decryptedPlainText);
  }

  @Test
  void encryptThrowsExceptionWhenMissingKey(@TempDirectory Path tempDir) {
    final PublicKey fake = new SodiumPublicKey("fake".getBytes(UTF_8));
    final PublicKey recipientKey = generateKey(tempDir);

    EnclaveException e = assertThrows(EnclaveException.class, () -> encrypt("plaintext", fake, recipientKey));
    assertEquals("No StoredPrivateKey found in keystore", e.getMessage());
  }

  @Test
  void decryptThrowsExceptionWhenMissingKey(@TempDirectory Path tempDir) {
    final PublicKey fake = new SodiumPublicKey("fake".getBytes(UTF_8));
    final SodiumPublicKey sender = generateKey(tempDir);

    EnclaveException e = assertThrows(EnclaveException.class, () -> {
      final EncryptedPayload payload =
          new SodiumEncryptedPayload(sender, new byte[] {}, new byte[] {}, new SodiumCombinedKey[] {}, new byte[] {});
      enclave.decrypt(payload, fake);
    });
    assertEquals("No StoredPrivateKey found in keystore", e.getMessage());
  }

  @Test
  void encryptDecryptNoCombinedKeys(@TempDirectory Path tempDir) {
    final PublicKey senderKey = generateKey(tempDir);
    final PublicKey recipientKey = generateKey(tempDir);

    final EncryptedPayload encryptedPayload = encrypt("hello", senderKey, recipientKey);

    final EncryptedPayload payload = new SodiumEncryptedPayload(
        (SodiumPublicKey) encryptedPayload.sender(),
        encryptedPayload.nonce(),
        encryptedPayload.combinedKeyNonce(),
        new SodiumCombinedKey[] {},
        encryptedPayload.cipherText());

    assertThrows(EnclaveException.class, () -> decrypt(payload, recipientKey));
  }

  @Test
  void invalidSenderKeyType() {
    final PublicKey senderKey = generateNonSodiumKey();

    EnclaveException e =
        assertThrows(EnclaveException.class, () -> encrypt("a message that never gets seen", senderKey));
    assertEquals("SodiumEnclave needs SodiumPublicKey", e.getMessage());
  }

  @Test
  void encryptDecryptBadCombinedKeyNonce(@TempDirectory Path tempDir) {
    final PublicKey senderKey = generateKey(tempDir);
    final PublicKey recipientKey = generateKey(tempDir);

    final EncryptedPayload encryptedPayload = encrypt("hello", senderKey, recipientKey);

    final EncryptedPayload payload = new SodiumEncryptedPayload(
        (SodiumPublicKey) encryptedPayload.sender(),
        encryptedPayload.nonce(),
        new byte[0],
        (SodiumCombinedKey[]) encryptedPayload.combinedKeys(),
        encryptedPayload.cipherText());

    EnclaveException e = assertThrows(EnclaveException.class, () -> decrypt(payload, recipientKey));
    assertEquals(
        "com.muquit.libsodiumjna.exceptions.SodiumLibraryException: nonce is 0bytes, it must be24 bytes",
        e.getMessage());
  }

  @Test
  void encryptDecryptBadNonce(@TempDirectory Path tempDir) {
    final PublicKey senderKey = generateKey(tempDir);
    final PublicKey recipientKey = generateKey(tempDir);

    final EncryptedPayload encryptedPayload = encrypt("hello", senderKey, recipientKey);

    final EncryptedPayload payload = new SodiumEncryptedPayload(
        (SodiumPublicKey) encryptedPayload.sender(),
        new byte[0],
        encryptedPayload.combinedKeyNonce(),
        (SodiumCombinedKey[]) encryptedPayload.combinedKeys(),
        encryptedPayload.cipherText());

    EnclaveException e = assertThrows(EnclaveException.class, () -> decrypt(payload, recipientKey));
    assertEquals(
        "com.muquit.libsodiumjna.exceptions.SodiumLibraryException: invalid nonce length 0 bytes",
        e.getMessage());
  }

  @Test
  void payloadCanOnlyBeDecryptedByItsKey(@TempDirectory Path tempDir) {
    final PublicKey senderKey = generateKey(tempDir);
    final PublicKey recipientKey1 = generateKey(tempDir);
    final PublicKey recipientKey2 = generateKey(tempDir);
    final String plaintext = "hello";

    final EncryptedPayload encryptedPayload1 = encrypt(plaintext, senderKey, recipientKey1);

    // trying to decrypt payload1 with recipient2 key
    EnclaveException e = assertThrows(EnclaveException.class, () -> decrypt(encryptedPayload1, recipientKey2));
    assertEquals(OrionErrorCode.ENCLAVE_DECRYPT_WRONG_PRIVATE_KEY, e.code());
  }

  @Test
  void encryptGeneratesDifferentCipherForSamePayloadAndKey(@TempDirectory Path tempDir) {
    final PublicKey senderKey = generateKey(tempDir);
    final PublicKey recipientKey = generateKey(tempDir);
    final String plaintext = "hello";

    final EncryptedPayload encryptedPayload1 = encrypt(plaintext, senderKey, recipientKey);
    final EncryptedPayload encryptedPayload2 = encrypt(plaintext, senderKey, recipientKey);

    assertNotEquals(encryptedPayload1.cipherText(), encryptedPayload2.cipherText());
  }

  private String decrypt(EncryptedPayload encryptedPayload, PublicKey senderKey) {
    return new String(enclave.decrypt(encryptedPayload, senderKey), UTF_8);
  }

  private EncryptedPayload encrypt(String plaintext, PublicKey senderKey, PublicKey... recipientKey) {
    return enclave.encrypt(plaintext.getBytes(UTF_8), senderKey, recipientKey);
  }

  private void assertEncryptDecrypt(byte[] nonce, SodiumKeyPair senderPair, SodiumKeyPair recipientPair, byte[] message)
      throws SodiumLibraryException {
    final byte[] ciphertext =
        SodiumLibrary.cryptoBoxEasy(message, nonce, recipientPair.getPublicKey(), senderPair.getPrivateKey());
    final byte[] decrypted =
        SodiumLibrary.cryptoBoxOpenEasy(ciphertext, nonce, senderPair.getPublicKey(), recipientPair.getPrivateKey());

    assertArrayEquals(message, decrypted);
  }

  private SodiumPublicKey generateKey(Path tempDir) {
    return (SodiumPublicKey) memoryKeyStore.generateKeyPair(new KeyConfig(tempDir.resolve("ignore"), Optional.empty()));
  }

  private PublicKey generateNonSodiumKey() {
    return new PublicKey() {

      private static final long serialVersionUID = 1L;

      @Override
      public String getFormat() {
        return null;
      }

      @Override
      public byte[] getEncoded() {
        return new byte[0];
      }

      @Override
      public String getAlgorithm() {
        return null;
      }
    };
  }
}

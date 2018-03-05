package net.consensys.orion.impl.enclave.sodium;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import net.consensys.orion.api.enclave.EnclaveException;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.enclave.KeyConfig;
import net.consensys.orion.api.enclave.KeyStore;
import net.consensys.orion.impl.config.MemoryConfig;

import java.security.PublicKey;
import java.util.Optional;

import com.muquit.libsodiumjna.SodiumKeyPair;
import com.muquit.libsodiumjna.SodiumLibrary;
import com.muquit.libsodiumjna.exceptions.SodiumLibraryException;
import org.junit.Before;
import org.junit.Test;

public class LibSodiumEnclaveTest {

  private final MemoryConfig config = new MemoryConfig();
  private final KeyStore memoryKeyStore = new SodiumMemoryKeyStore(config);
  private LibSodiumEnclave enclave;

  @Before
  public void setUp() throws Exception {
    config.setLibSodiumPath(LibSodiumSettings.defaultLibSodiumPath());
    enclave = new LibSodiumEnclave(config, memoryKeyStore);
  }

  @Test
  public void version() {
    assertTrue(!SodiumLibrary.libsodiumVersionString().isEmpty());
  }

  @Test
  public void sodiumLoads() throws SodiumLibraryException {
    final int nonceBytesLength = SodiumLibrary.cryptoBoxNonceBytes().intValue();
    final byte[] nonce = SodiumLibrary.randomBytes((int) nonceBytesLength);
    final SodiumKeyPair senderPair = SodiumLibrary.cryptoBoxKeyPair();
    final SodiumKeyPair recipientPair = SodiumLibrary.cryptoBoxKeyPair();

    final byte[] message = "hello".getBytes();
    assertEncryptDecrypt(nonce, senderPair, recipientPair, message);

    final byte[] secretKey =
        SodiumLibrary.randomBytes(SodiumLibrary.cryptoSecretBoxKeyBytes().intValue());
    assertEncryptDecrypt(nonce, senderPair, recipientPair, secretKey);
  }

  @Test
  public void recipienEncryptDecrypt() throws SodiumLibraryException {
    final PublicKey senderKey = generateKey();
    final PublicKey recipientKey = generateKey();
    final String plaintext = "hello again";

    final EncryptedPayload encryptedPayload = encrypt(plaintext, senderKey, recipientKey);
    final String decrypted = decrypt(encryptedPayload, recipientKey);

    assertEquals(plaintext, decrypted);
  }

  @Test
  /** Sender can decrpt the cipher text for their encrypted plaint text. */
  public void senderEncryptDecrypt() {
    final PublicKey senderKey = generateKey();
    final String plaintext = "the original message";

    final EncryptedPayload encryptedPayload = encrypt(plaintext, senderKey);
    final String decrptedPlainText = decrypt(encryptedPayload, senderKey);

    assertEquals(plaintext, decrptedPlainText);
  }

  @Test
  /** Sender decryption must not be affected by the presence of other combined keys (recipients) */
  public void senderEncryptDecryptWithRecipients() {
    final PublicKey senderKey = generateKey();
    final PublicKey recipientAKey = generateKey();
    final PublicKey recipientBKey = generateKey();
    final String plaintext = "the other original message";

    final EncryptedPayload encryptedPayload =
        encrypt(plaintext, senderKey, recipientAKey, recipientBKey);
    final String decrptedPlainText = decrypt(encryptedPayload, senderKey);

    assertEquals(plaintext, decrptedPlainText);
  }

  @Test
  public void encryptThrowsExceptionWhenMissingKey() throws Exception {
    final PublicKey fake = new SodiumPublicKey("fake".getBytes());
    final PublicKey recipientKey = generateKey();

    try {
      encrypt("plaintext", fake, recipientKey);
      fail("Should have thrown an Enclave Exception");
    } catch (EnclaveException e) {
      assertEquals("No StoredPrivateKey found in keystore", e.getMessage());
    }
  }

  @Test
  public void decryptThrowsExceptionWhnMissingKey() throws Exception {
    final PublicKey fake = new SodiumPublicKey("fake".getBytes());
    final SodiumPublicKey sender = generateKey();

    try {
      final EncryptedPayload payload =
          new SodiumEncryptedPayload(
              sender, new byte[] {}, new byte[] {}, new SodiumCombinedKey[] {}, new byte[] {});
      enclave.decrypt(payload, fake);
      fail("Should have thrown an Enclave Exception");
    } catch (EnclaveException e) {
      assertEquals("No StoredPrivateKey found in keystore", e.getMessage());
    }
  }

  @Test(expected = EnclaveException.class)
  public void encryptDecryptNoCombinedKeys() {
    final PublicKey senderKey = generateKey();
    final PublicKey recipientKey = generateKey();

    final EncryptedPayload encryptedPayload = encrypt("hello", senderKey, recipientKey);

    final EncryptedPayload payload =
        new SodiumEncryptedPayload(
            (SodiumPublicKey) encryptedPayload.sender(),
            encryptedPayload.nonce(),
            encryptedPayload.combinedKeyNonce(),
            new SodiumCombinedKey[] {},
            encryptedPayload.cipherText());

    decrypt(payload, recipientKey);
  }

  @Test
  public void invalidSenderKeyType() {
    final PublicKey senderKey = generateNonSodiumKey();

    try {
      encrypt("a message that never gets seen", senderKey);
      fail("Expecting an exception from key type validation");
    } catch (final EnclaveException e) {
      assertEquals("SodiumEnclave needs SodiumPublicKey", e.getMessage());
    }
  }

  @Test
  public void encryptDecryptBadCombinedKeyNonce() {
    final PublicKey senderKey = generateKey();
    final PublicKey recipientKey = generateKey();

    final EncryptedPayload encryptedPayload = encrypt("hello", senderKey, recipientKey);

    final EncryptedPayload payload =
        new SodiumEncryptedPayload(
            (SodiumPublicKey) encryptedPayload.sender(),
            encryptedPayload.nonce(),
            new byte[0],
            (SodiumCombinedKey[]) encryptedPayload.combinedKeys(),
            encryptedPayload.cipherText());

    try {
      decrypt(payload, recipientKey);
      fail("Expecting an exceptional combined key nonce");
    } catch (final EnclaveException e) {
      assertEquals(
          "com.muquit.libsodiumjna.exceptions.SodiumLibraryException: nonce is 0bytes, it must be24 bytes",
          e.getMessage());
    }
  }

  @Test
  public void encryptDecryptBadNonce() {
    final PublicKey senderKey = generateKey();
    final PublicKey recipientKey = generateKey();

    final EncryptedPayload encryptedPayload = encrypt("hello", senderKey, recipientKey);

    final EncryptedPayload payload =
        new SodiumEncryptedPayload(
            (SodiumPublicKey) encryptedPayload.sender(),
            new byte[0],
            encryptedPayload.combinedKeyNonce(),
            (SodiumCombinedKey[]) encryptedPayload.combinedKeys(),
            encryptedPayload.cipherText());

    try {
      decrypt(payload, recipientKey);
      fail("Expecting an exceptional nonce");
    } catch (final EnclaveException e) {
      assertEquals(
          "com.muquit.libsodiumjna.exceptions.SodiumLibraryException: invalid nonce length 0 bytes",
          e.getMessage());
    }
  }

  private String decrypt(EncryptedPayload encryptedPayload, PublicKey senderKey) {
    return new String(enclave.decrypt(encryptedPayload, senderKey));
  }

  private EncryptedPayload encrypt(
      String plaintext, PublicKey senderKey, PublicKey... recipientKey) {
    return enclave.encrypt(plaintext.getBytes(), senderKey, recipientKey);
  }

  private void assertEncryptDecrypt(
      byte[] nonce, SodiumKeyPair senderPair, SodiumKeyPair recipientPair, byte[] message)
      throws SodiumLibraryException {
    final byte[] ciphertext =
        SodiumLibrary.cryptoBoxEasy(
            message, nonce, recipientPair.getPublicKey(), senderPair.getPrivateKey());
    final byte[] decrypted =
        SodiumLibrary.cryptoBoxOpenEasy(
            ciphertext, nonce, senderPair.getPublicKey(), recipientPair.getPrivateKey());

    assertArrayEquals(message, decrypted);
  }

  private SodiumPublicKey generateKey() {
    return (SodiumPublicKey)
        memoryKeyStore.generateKeyPair(new KeyConfig("ignore", Optional.empty()));
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

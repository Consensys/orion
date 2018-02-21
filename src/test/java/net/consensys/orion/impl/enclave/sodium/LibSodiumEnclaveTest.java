package net.consensys.orion.impl.enclave.sodium;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
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
  MemoryConfig config = new MemoryConfig();
  KeyStore memoryKeyStore = new SodiumMemoryKeyStore(config);
  LibSodiumEnclave enclave;

  @Before
  public void setUp() throws Exception {
    config.setLibSodiumPath(LibSodiumSettings.defaultLibSodiumPath());
    enclave = new LibSodiumEnclave(config, memoryKeyStore);
  }

  @Test
  public void testVersion() {
    System.out.println(SodiumLibrary.libsodiumVersionString());
  }

  @Test
  public void testSodium() throws SodiumLibraryException {
    int nonceBytesLength = SodiumLibrary.cryptoBoxNonceBytes().intValue();
    byte[] nonce = SodiumLibrary.randomBytes((int) nonceBytesLength);
    SodiumKeyPair senderPair = SodiumLibrary.cryptoBoxKeyPair();
    SodiumKeyPair recipientPair = SodiumLibrary.cryptoBoxKeyPair();

    byte[] message = "hello".getBytes();
    checkEncryptDecrypt(nonce, senderPair, recipientPair, message);

    byte[] secretKey =
        SodiumLibrary.randomBytes(SodiumLibrary.cryptoSecretBoxKeyBytes().intValue());
    checkEncryptDecrypt(nonce, senderPair, recipientPair, secretKey);
  }

  private void checkEncryptDecrypt(
      byte[] nonce, SodiumKeyPair senderPair, SodiumKeyPair recipientPair, byte[] message)
      throws SodiumLibraryException {
    byte[] ciphertext =
        SodiumLibrary.cryptoBoxEasy(
            message, nonce, recipientPair.getPublicKey(), senderPair.getPrivateKey());

    byte[] decrypted =
        SodiumLibrary.cryptoBoxOpenEasy(
            ciphertext, nonce, senderPair.getPublicKey(), recipientPair.getPrivateKey());
    assertArrayEquals(message, decrypted);
  }

  @Test
  public void testEncryptDecrypt() throws SodiumLibraryException {
    PublicKey senderKey = generateKey();
    PublicKey recipientKey = generateKey();

    String plaintext = "hello";
    EncryptedPayload encryptedPayload =
        enclave.encrypt(plaintext.getBytes(), senderKey, new PublicKey[] {recipientKey});
    byte[] bytes = enclave.decrypt(encryptedPayload, recipientKey);
    String decrypted = new String(bytes);
    assertEquals(plaintext, decrypted);
  }

  @Test
  public void testEncryptThrowsExceptionWhenMissingKey() throws Exception {
    PublicKey fake = new SodiumPublicKey("fake".getBytes());
    PublicKey recipientKey = generateKey();
    try {
      enclave.encrypt("plaintext".getBytes(), fake, new PublicKey[] {recipientKey});
      fail("Should have thrown an Enclave Exception");
    } catch (EnclaveException e) {
      assertEquals("No StoredPrivateKey found in keystore", e.getMessage());
    }
  }

  private SodiumPublicKey generateKey() {
    return (SodiumPublicKey)
        memoryKeyStore.generateKeyPair(new KeyConfig("ignore", Optional.empty()));
  }

  @Test
  public void testDecryptThrowsExceptionWhnMissingKey() throws Exception {
    PublicKey fake = new SodiumPublicKey("fake".getBytes());
    SodiumPublicKey sender = generateKey();
    try {
      EncryptedPayload payload =
          new SodiumEncryptedPayload(
              sender, new byte[] {}, new byte[] {}, new SodiumCombinedKey[] {}, new byte[] {});
      enclave.decrypt(payload, fake);
      fail("Should have thrown an Enclave Exception");
    } catch (EnclaveException e) {
      assertEquals("No StoredPrivateKey found in keystore", e.getMessage());
    }
  }
}

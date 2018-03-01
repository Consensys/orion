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
    System.out.println(SodiumLibrary.libsodiumVersionString());
  }

  @Test
  public void sodium() throws SodiumLibraryException {
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
  public void encryptDecrypt() throws SodiumLibraryException {
    final PublicKey senderKey = generateKey();
    final PublicKey recipientKey = generateKey();

    final String plaintext = "hello";
    final EncryptedPayload encryptedPayload =
        enclave.encrypt(plaintext.getBytes(), senderKey, new PublicKey[] {recipientKey});
    final byte[] bytes = enclave.decrypt(encryptedPayload, recipientKey);
    final String decrypted = new String(bytes);
    assertEquals(plaintext, decrypted);
  }

  @Test
  public void encryptThrowsExceptionWhenMissingKey() throws Exception {
    final PublicKey fake = new SodiumPublicKey("fake".getBytes());
    final PublicKey recipientKey = generateKey();

    try {
      enclave.encrypt("plaintext".getBytes(), fake, new PublicKey[] {recipientKey});
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
      EncryptedPayload payload =
          new SodiumEncryptedPayload(
              sender, new byte[] {}, new byte[] {}, new SodiumCombinedKey[] {}, new byte[] {});
      enclave.decrypt(payload, fake);
      fail("Should have thrown an Enclave Exception");
    } catch (EnclaveException e) {
      assertEquals("No StoredPrivateKey found in keystore", e.getMessage());
    }
  }

  // TODO multiple combined keys

  // TODO no combined keys

  // TODO exceptions from Sodium

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
}

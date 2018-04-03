package net.consensys.orion.impl.enclave.sodium;

import static org.junit.Assert.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.enclave.EnclaveException;
import net.consensys.orion.api.enclave.KeyConfig;
import net.consensys.orion.impl.config.MemoryConfig;
import net.consensys.orion.impl.config.TomlConfigBuilder;
import net.consensys.orion.impl.utils.Base64;

import java.io.File;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Optional;

import org.junit.Test;

public class SodiumFileKeyStoreTest {

  private InputStream configAsStream = this.getClass().getClassLoader().getResourceAsStream("keyStoreTest.toml");
  private TomlConfigBuilder configBuilder = new TomlConfigBuilder();
  private Config config = configBuilder.build(configAsStream);
  private SodiumFileKeyStore keyStore = new SodiumFileKeyStore(config);
  private String publicKey1Base64Encoded = "BULeR8JyUWhiuuCMU/HLA0Q5pzkYT+cHII3ZKBey3Bo=";
  private String privateKey1Base64Encoded = "Wl+xSyXVuuqzpvznOS7dOobhcn4C5auxkFRi7yLtgtA=";
  private PublicKey publicKey1 = new SodiumPublicKey(Base64.decode(publicKey1Base64Encoded));
  private PrivateKey privateKey1 = new SodiumPrivateKey(Base64.decode(privateKey1Base64Encoded));

  @Test
  public void configLoadsRawKeys() {
    Optional<PrivateKey> storedKey = keyStore.privateKey(publicKey1);
    assertEquals(privateKey1, storedKey.get());
  }

  @Test
  public void configLoadsNullKey() {
    Optional<PrivateKey> storedKey = keyStore.privateKey(null);
    assertFalse(storedKey.isPresent());
  }

  @Test
  public void missingKeyBehaviourIsNice() {
    MemoryConfig config = new MemoryConfig();
    config.setPrivateKeys(new File[] {new File("Does not exist")});
    config.setPublicKeys(new File[] {new File("Does not exist")});
    config.setAlwaysSendTo(new File[] {new File("Does not exist")});
    try {
      keyStore = new SodiumFileKeyStore(config);
      fail("should have thrown an EnclaveException");
    } catch (EnclaveException e) {
      // expected
    }
  }

  @Test
  public void missingAlgorithimRaisesAppropriateException() {
    MemoryConfig config = new MemoryConfig();
    config.setPrivateKeys(new File[] {new File("keys/noalgorithm.key")});
    config.setPublicKeys(new File[] {new File("keys/tm1a.pub")});
    config.setAlwaysSendTo(new File[] {new File("keys/tm1a.pub")});
    try {
      keyStore = new SodiumFileKeyStore(config);
      fail("should have thrown an EnclaveException");
    } catch (EnclaveException e) {
      // expected
    }
  }

  @Test
  public void configLoadsMultipleKeys() {
    InputStream configAsStream = this.getClass().getClassLoader().getResourceAsStream("multipleKeyStoreTest.toml");

    Config config = configBuilder.build(configAsStream);
    keyStore = new SodiumFileKeyStore(config);
    String[] encodedPublicKeys =
        new String[] {"BULeR8JyUWhiuuCMU/HLA0Q5pzkYT+cHII3ZKBey3Bo=", "8SjRHlUBe4hAmTk3KDeJ96RhN+s10xRrHDrxEi1O5W0="};

    String[] encodedPrivateKeys =
        new String[] {"Wl+xSyXVuuqzpvznOS7dOobhcn4C5auxkFRi7yLtgtA=", "wGEar7J9G0JAgdisp61ZChyrJWeW2QPyKvecjjeVHOY="};

    for (int i = 0; i < encodedPrivateKeys.length; i++) {
      PrivateKey privateKey = new SodiumPrivateKey(Base64.decode(encodedPrivateKeys[i]));
      PublicKey publicKey = new SodiumPublicKey(Base64.decode(encodedPublicKeys[i]));
      Optional<PrivateKey> storedKey = keyStore.privateKey(publicKey);
      assertEquals(privateKey, storedKey.get());
    }
  }

  @Test
  public void alwaysSendTo() {
    InputStream configAsStream = this.getClass().getClassLoader().getResourceAsStream("alwaysSendToKeyStoreTest.toml");

    Config config = configBuilder.build(configAsStream);
    keyStore = new SodiumFileKeyStore(config);
    String[] encodedPublicKeys =
        new String[] {"BULeR8JyUWhiuuCMU/HLA0Q5pzkYT+cHII3ZKBey3Bo=", "8SjRHlUBe4hAmTk3KDeJ96RhN+s10xRrHDrxEi1O5W0="};

    PublicKey[] publicKeys = new PublicKey[encodedPublicKeys.length];
    for (int i = 0; i < encodedPublicKeys.length; i++) {
      PublicKey publicKey = new SodiumPublicKey(Base64.decode(encodedPublicKeys[i]));
      publicKeys[i] = publicKey;
    }
    assertArrayEquals(publicKeys, keyStore.alwaysSendTo());
  }

  public void loadOfPasswordProtectedKeys() {
    MemoryConfig config = new MemoryConfig();
    config.setPrivateKeys(new File[] {new File("keys/password.key")});
    config.setPublicKeys(new File[] {new File("keys/password.pub")});
    keyStore = new SodiumFileKeyStore(config);
  }

  @Test
  public void generateUnlockedProtectedKeyPair() {
    String keyPrefix = "keys/generated";
    try {
      keyStore = new SodiumFileKeyStore(config);
      keyStore.generateKeyPair(new KeyConfig(keyPrefix, Optional.empty()));

      // Load the a config using the generated key, and confirm that it is valid.
      // this shows that the key was stored and that we could load it.
      MemoryConfig config = new MemoryConfig();
      config.setPrivateKeys(new File[] {new File("keys/generated.key")});
      config.setPublicKeys(new File[] {new File("keys/generated.pub")});
      keyStore = new SodiumFileKeyStore(config);
      PublicKey fromStore = keyStore.nodeKeys()[0];
      assertNotNull(keyStore.privateKey(fromStore));
    } finally {
      File privateKey = new File(keyPrefix + ".key");
      File publicKey = new File(keyPrefix + ".pub");
      if (privateKey.exists()) {
        privateKey.delete();
      } else {
        fail("private key did not get created");
      }
      if (publicKey.exists()) {
        publicKey.delete();
      } else {
        fail("public key did not get created");
      }
    }
  }

  @Test
  public void generatePasswordProtectedKeyPair() {
    String keyPrefix = "keys/generated_password";
    try {
      keyStore = new SodiumFileKeyStore(config);
      keyStore.generateKeyPair(new KeyConfig(keyPrefix, Optional.of("yolo")));

      // Load the a config using the generated key, and confirm that it is valid.
      // this shows that the key was stored and that we could load it.
      MemoryConfig config = new MemoryConfig();
      config.setPasswords(new File("keys/password.txt"));
      config.setPrivateKeys(new File[] {new File("keys/generated_password.key")});
      config.setPublicKeys(new File[] {new File("keys/generated_password.pub")});
      keyStore = new SodiumFileKeyStore(config);
      PublicKey fromStore = keyStore.nodeKeys()[0];
      assertNotNull(keyStore.privateKey(fromStore));
    } finally {
      File privateKey = new File(keyPrefix + ".key");
      File publicKey = new File(keyPrefix + ".pub");
      if (privateKey.exists()) {
        privateKey.delete();
      } else {
        fail("private key did not get created");
      }
      if (publicKey.exists()) {
        publicKey.delete();
      } else {
        fail("public key did not get created");
      }
    }
  }
}

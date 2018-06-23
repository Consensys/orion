package net.consensys.orion.impl.enclave.sodium;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.enclave.EnclaveException;
import net.consensys.orion.api.enclave.KeyConfig;
import net.consensys.orion.impl.config.MemoryConfig;
import net.consensys.orion.impl.config.TomlConfigBuilder;
import net.consensys.orion.impl.utils.Base64;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class SodiumFileKeyStoreTest {

  private InputStream configAsStream = this.getClass().getClassLoader().getResourceAsStream("keyStoreTest.toml");
  private TomlConfigBuilder configBuilder = new TomlConfigBuilder();
  private Config config = configBuilder.build(configAsStream);
  private SodiumFileKeyStore keyStore = new SodiumFileKeyStore(config);
  private String publicKey1Base64Encoded = "BULeR8JyUWhiuuCMU/HLA0Q5pzkYT+cHII3ZKBey3Bo=";
  private String privateKey1Base64Encoded = "Wl+xSyXVuuqzpvznOS7dOobhcn4C5auxkFRi7yLtgtA=";
  private PublicKey publicKey1 = new SodiumPublicKey(Base64.decode(publicKey1Base64Encoded));
  private PrivateKey privateKey1 = new SodiumPrivateKey(Base64.decode(privateKey1Base64Encoded));

  @Test
  void configLoadsRawKeys() {
    Optional<PrivateKey> storedKey = keyStore.privateKey(publicKey1);
    assertEquals(privateKey1, storedKey.get());
  }

  @Test
  void configLoadsNullKey() {
    Optional<PrivateKey> storedKey = keyStore.privateKey(null);
    assertFalse(storedKey.isPresent());
  }

  @Test
  void missingKeyBehaviourIsNice() {
    MemoryConfig config = new MemoryConfig();
    config.setPrivateKeys(Paths.get("Does not exist"));
    config.setPublicKeys(Paths.get("Does not exist"));
    config.setAlwaysSendTo(Paths.get("Does not exist"));
    assertThrows(EnclaveException.class, () -> new SodiumFileKeyStore(config));
  }

  @Test
  void missingAlgorithimRaisesAppropriateException() {
    MemoryConfig config = new MemoryConfig();
    config.setPrivateKeys(Paths.get("keys/noalgorithm.key"));
    config.setPublicKeys(Paths.get("keys/tm1a.pub"));
    config.setAlwaysSendTo(Paths.get("keys/tm1a.pub"));
    assertThrows(EnclaveException.class, () -> new SodiumFileKeyStore(config));
  }

  @Test
  void configLoadsMultipleKeys() {
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
  void alwaysSendTo() {
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

  @Test
  void generateUnlockedProtectedKeyPair() throws Exception {
    String keyPrefix = "keys/generated";
    try {
      keyStore = new SodiumFileKeyStore(config);
      keyStore.generateKeyPair(new KeyConfig(keyPrefix, Optional.empty()));

      // Load the a config using the generated key, and confirm that it is valid.
      // this shows that the key was stored and that we could load it.
      MemoryConfig config = new MemoryConfig();
      config.setPrivateKeys(Paths.get("keys/generated.key"));
      config.setPublicKeys(Paths.get("keys/generated.pub"));
      keyStore = new SodiumFileKeyStore(config);
      PublicKey fromStore = keyStore.nodeKeys()[0];
      assertNotNull(keyStore.privateKey(fromStore));
    } finally {
      Path privateKey = Paths.get(keyPrefix + ".key");
      Path publicKey = Paths.get(keyPrefix + ".pub");
      if (Files.exists(privateKey)) {
        Files.delete(privateKey);
      } else {
        fail("private key did not get created");
      }
      if (Files.exists(publicKey)) {
        Files.delete(publicKey);
      } else {
        fail("public key did not get created");
      }
    }
  }

  @Test
  void generatePasswordProtectedKeyPair() throws Exception {
    String keyPrefix = "keys/generated_password";
    try {
      keyStore = new SodiumFileKeyStore(config);
      keyStore.generateKeyPair(new KeyConfig(keyPrefix, Optional.of("yolo")));

      // Load the a config using the generated key, and confirm that it is valid.
      // this shows that the key was stored and that we could load it.
      MemoryConfig config = new MemoryConfig();
      config.setPasswords(Paths.get("keys/password.txt"));
      config.setPrivateKeys(Paths.get("keys/generated_password.key"));
      config.setPublicKeys(Paths.get("keys/generated_password.pub"));
      keyStore = new SodiumFileKeyStore(config);
      PublicKey fromStore = keyStore.nodeKeys()[0];
      assertNotNull(keyStore.privateKey(fromStore));
    } finally {
      Path privateKey = Paths.get(keyPrefix + ".key");
      Path publicKey = Paths.get(keyPrefix + ".pub");
      if (Files.exists(privateKey)) {
        Files.delete(privateKey);
      } else {
        fail("private key did not get created");
      }
      if (Files.exists(publicKey)) {
        Files.delete(publicKey);
      } else {
        fail("public key did not get created");
      }
    }
  }
}

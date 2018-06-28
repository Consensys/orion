package net.consensys.orion.impl.enclave.sodium;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.enclave.EnclaveException;
import net.consensys.orion.api.enclave.KeyConfig;
import net.consensys.orion.api.enclave.PrivateKey;
import net.consensys.orion.api.enclave.PublicKey;
import net.consensys.orion.impl.utils.Base64;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class FileKeyStoreTest {

  private static final PublicKey PUBLIC_KEY_1 =
      new PublicKey(Base64.decode("BULeR8JyUWhiuuCMU/HLA0Q5pzkYT+cHII3ZKBey3Bo="));
  private static final PrivateKey PRIVATE_KEY_1 =
      new PrivateKey(Base64.decode("Wl+xSyXVuuqzpvznOS7dOobhcn4C5auxkFRi7yLtgtA="));
  private static Config config;
  private FileKeyStore keyStore;

  @BeforeAll
  static void setupConfig() throws Exception {
    config = Config.load(FileKeyStoreTest.class.getClassLoader().getResourceAsStream("keyStoreTest.toml"));
  }

  @BeforeEach
  void setupKeyStore() {
    keyStore = new FileKeyStore(config);
  }

  @Test
  void configLoadsRawKeys() {
    Optional<PrivateKey> storedKey = keyStore.privateKey(PUBLIC_KEY_1);
    assertEquals(PRIVATE_KEY_1, storedKey.get());
  }

  @Test
  void configLoadsNullKey() {
    Optional<PrivateKey> storedKey = keyStore.privateKey(null);
    assertFalse(storedKey.isPresent());
  }

  @Test
  void missingKeyBehaviourIsNice() {
    // @formatter:off
    Config config = Config.load(
        "privatekeys=[\"Does not exist\"]\n"
      + "publickeys=[\"Does not exist\"]\n"
      + "alwayssendto=[\"Does not exist\"]");
    // @formatter:on
    assertThrows(EnclaveException.class, () -> new FileKeyStore(config));
  }

  @Test
  void missingAlgorithimRaisesAppropriateException() {
    // @formatter:off
    Config config = Config.load(
        "privatekeys=[\"keys/noalgorithm.key\"]\n"
      + "publickeys=[\"keys/tm1a.pub\"]\n"
      + "alwayssendto=[\"keys/tm1a.pub\"]");
    // @formatter:on
    assertThrows(EnclaveException.class, () -> new FileKeyStore(config));
  }

  @Test
  void configLoadsMultipleKeys() throws IOException {
    Config config = Config.load(this.getClass().getClassLoader().getResourceAsStream("multipleKeyStoreTest.toml"));
    keyStore = new FileKeyStore(config);
    String[] encodedPublicKeys =
        new String[] {"BULeR8JyUWhiuuCMU/HLA0Q5pzkYT+cHII3ZKBey3Bo=", "8SjRHlUBe4hAmTk3KDeJ96RhN+s10xRrHDrxEi1O5W0="};

    String[] encodedPrivateKeys =
        new String[] {"Wl+xSyXVuuqzpvznOS7dOobhcn4C5auxkFRi7yLtgtA=", "wGEar7J9G0JAgdisp61ZChyrJWeW2QPyKvecjjeVHOY="};

    for (int i = 0; i < encodedPrivateKeys.length; i++) {
      PrivateKey privateKey = new PrivateKey(Base64.decode(encodedPrivateKeys[i]));
      PublicKey publicKey = new PublicKey(Base64.decode(encodedPublicKeys[i]));
      Optional<PrivateKey> storedKey = keyStore.privateKey(publicKey);
      assertEquals(privateKey, storedKey.get());
    }
  }

  @Test
  void alwaysSendTo() throws IOException {
    Config config = Config.load(this.getClass().getClassLoader().getResourceAsStream("alwaysSendToKeyStoreTest.toml"));
    assertEquals(Paths.get("keys"), config.workDir());
    keyStore = new FileKeyStore(config);
    String[] encodedPublicKeys =
        new String[] {"BULeR8JyUWhiuuCMU/HLA0Q5pzkYT+cHII3ZKBey3Bo=", "8SjRHlUBe4hAmTk3KDeJ96RhN+s10xRrHDrxEi1O5W0="};

    PublicKey[] publicKeys = new PublicKey[encodedPublicKeys.length];
    for (int i = 0; i < encodedPublicKeys.length; i++) {
      PublicKey publicKey = new PublicKey(Base64.decode(encodedPublicKeys[i]));
      publicKeys[i] = publicKey;
    }
    assertArrayEquals(publicKeys, keyStore.alwaysSendTo());
  }

  @Test
  void generateUnlockedProtectedKeyPair(@TempDirectory Path tempDir) throws Exception {
    Files.createDirectories(tempDir.resolve("keys"));
    Path keyPrefix = tempDir.resolve("keys").resolve("generated");
    keyStore = new FileKeyStore(config);
    keyStore.generateKeyPair(new KeyConfig(keyPrefix, Optional.empty()));

    // Load the a config using the generated key, and confirm that it is valid.
    // this shows that the key was stored and that we could load it.

    // @formatter:off
    Config config = Config.load(
          "privatekeys=['keys/generated.key']\n"
        + "publickeys=['keys/generated.pub']\n"
        + "workdir='" + tempDir + "'\n");
    // @formatter:on
    keyStore = new FileKeyStore(config);
    PublicKey fromStore = keyStore.nodeKeys()[0];
    assertNotNull(keyStore.privateKey(fromStore));

    Path privateKey = tempDir.resolve("keys").resolve("generated.key");
    Path publicKey = tempDir.resolve("keys").resolve("generated.pub");
    assertTrue(Files.exists(privateKey));
    assertTrue(Files.exists(publicKey));
  }

  @Test
  void generatePasswordProtectedKeyPair(@TempDirectory Path tempDir) throws Exception {
    Files.createDirectories(tempDir.resolve("keys"));
    Path keyPrefix = tempDir.resolve("keys").resolve("generated_password");
    keyStore = new FileKeyStore(config);
    keyStore.generateKeyPair(new KeyConfig(keyPrefix, Optional.of("yolo")));

    Path privateKey = tempDir.resolve("keys").resolve("generated_password.key");
    Path publicKey = tempDir.resolve("keys").resolve("generated_password.pub");

    // Load the a config using the generated key, and confirm that it is valid.
    // this shows that the key was stored and that we could load it.
    // @formatter:off
    Config config = Config.load(
        "passwords='keys/password.txt'\n"
      + "privatekeys=['" + privateKey.toAbsolutePath() + "']\n"
      + "publickeys=['" + publicKey.toAbsolutePath() + "']\n");
    // @formatter:on
    keyStore = new FileKeyStore(config);
    PublicKey fromStore = keyStore.nodeKeys()[0];
    assertNotNull(keyStore.privateKey(fromStore));

    assertTrue(Files.exists(privateKey));
    assertTrue(Files.exists(publicKey));
  }
}

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

import static net.consensys.cava.io.Base64.decodeBytes;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.config.Config;
import net.consensys.orion.enclave.EnclaveException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class FileKeyStoreTest {

  private static final Box.PublicKey PUBLIC_KEY_1 =
      Box.PublicKey.fromBytes(decodeBytes("BULeR8JyUWhiuuCMU/HLA0Q5pzkYT+cHII3ZKBey3Bo="));
  private static final Box.SecretKey PRIVATE_KEY_1 =
      Box.SecretKey.fromBytes(decodeBytes("Wl+xSyXVuuqzpvznOS7dOobhcn4C5auxkFRi7yLtgtA="));
  private static Config config;
  private FileKeyStore keyStore;

  @BeforeAll
  static void setupConfig() throws Exception {
    config = Config.load(FileKeyStoreTest.class.getClassLoader().getResourceAsStream("keyStoreTest.toml"));
  }

  @BeforeEach
  void setupKeyStore() throws Exception {
    keyStore = new FileKeyStore(config);
  }

  @Test
  void configLoadsRawKeys() {
    assertEquals(PRIVATE_KEY_1, keyStore.privateKey(PUBLIC_KEY_1));
  }

  @Test
  void configLoadsNullKey() {
    assertNull(keyStore.privateKey(null));
  }

  @Test
  void missingKeyBehaviourIsNice() {
    // @formatter:off
    Config config = Config.load(
        "privatekeys=[\"Does not exist\"]\n"
      + "publickeys=[\"Does not exist\"]\n"
      + "alwayssendto=[\"Does not exist\"]");
    // @formatter:on
    IOException ex = assertThrows(IOException.class, () -> new FileKeyStore(config));
    assertTrue(ex.getMessage().startsWith("Failed to read public key file '"));
  }

  @Test
  void unknownKeyTypeRaisesAppropriateException() {
    // @formatter:off
    Config config = Config.load(
        "privatekeys=[\"keys/unknown_type.key\"]\n"
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
      Box.SecretKey privateKey = Box.SecretKey.fromBytes(decodeBytes(encodedPrivateKeys[i]));
      Box.PublicKey publicKey = Box.PublicKey.fromBytes(decodeBytes(encodedPublicKeys[i]));
      assertEquals(privateKey, keyStore.privateKey(publicKey));
    }
  }

  @Test
  void alwaysSendTo() throws IOException {
    Config config = Config.load(this.getClass().getClassLoader().getResourceAsStream("alwaysSendToKeyStoreTest.toml"));
    assertEquals(Paths.get("keys"), config.workDir());
    keyStore = new FileKeyStore(config);
    String[] encodedPublicKeys =
        new String[] {"BULeR8JyUWhiuuCMU/HLA0Q5pzkYT+cHII3ZKBey3Bo=", "8SjRHlUBe4hAmTk3KDeJ96RhN+s10xRrHDrxEi1O5W0="};

    Box.PublicKey[] publicKeys = new Box.PublicKey[encodedPublicKeys.length];
    for (int i = 0; i < encodedPublicKeys.length; i++) {
      publicKeys[i] = Box.PublicKey.fromBytes(decodeBytes(encodedPublicKeys[i]));
    }
    assertArrayEquals(publicKeys, keyStore.alwaysSendTo());
  }

  @Test
  void generateUnlockedProtectedKeyPair(@TempDirectory Path tempDir) throws Exception {
    Files.createDirectories(tempDir.resolve("keys"));
    Path keyPrefix = tempDir.resolve("keys").resolve("generated");
    keyStore = new FileKeyStore(config);
    keyStore.generateKeyPair(keyPrefix);

    // Load the a config using the generated key, and confirm that it is valid.
    // this shows that the key was stored and that we could load it.

    // @formatter:off
    Config config = Config.load(
          "privatekeys=['keys/generated.key']\n"
        + "publickeys=['keys/generated.pub']\n"
        + "workdir='" + tempDir + "'\n");
    // @formatter:on
    keyStore = new FileKeyStore(config);
    Box.PublicKey fromStore = keyStore.nodeKeys()[0];
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
    keyStore.generateKeyPair(keyPrefix, "yolo");

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
    Box.PublicKey fromStore = keyStore.nodeKeys()[0];
    assertNotNull(keyStore.privateKey(fromStore));

    assertTrue(Files.exists(privateKey));
    assertTrue(Files.exists(publicKey));
  }
}

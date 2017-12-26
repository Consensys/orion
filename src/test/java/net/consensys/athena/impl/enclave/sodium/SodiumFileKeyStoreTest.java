package net.consensys.athena.impl.enclave.sodium;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import net.consensys.athena.api.config.Config;
import net.consensys.athena.api.enclave.EnclaveException;
import net.consensys.athena.api.enclave.KeyConfig;
import net.consensys.athena.impl.config.MemoryConfig;
import net.consensys.athena.impl.config.TomlConfigBuilder;
import net.consensys.athena.impl.http.data.Base64;
import net.consensys.athena.impl.http.data.Serializer;

import java.io.File;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Optional;

import org.junit.Test;

public class SodiumFileKeyStoreTest {

  InputStream configAsStream =
      this.getClass().getClassLoader().getResourceAsStream("keyStoreTest.toml");
  TomlConfigBuilder configBuilder = new TomlConfigBuilder();
  final Serializer serializer = new Serializer();
  Config config = configBuilder.build(configAsStream);
  SodiumFileKeyStore keyStore = new SodiumFileKeyStore(config, serializer);
  String publicKey1Base64Encoded = "BULeR8JyUWhiuuCMU/HLA0Q5pzkYT+cHII3ZKBey3Bo=";
  String privateKey1Base64Encoded = "Wl+xSyXVuuqzpvznOS7dOobhcn4C5auxkFRi7yLtgtA=";
  PublicKey publicKey1 = new SodiumPublicKey(Base64.decode(publicKey1Base64Encoded));
  PrivateKey privateKey1 = new SodiumPrivateKey(Base64.decode(privateKey1Base64Encoded));

  @Test
  public void testConfigLoadsRawKeys() throws Exception {
    PrivateKey storedKey = keyStore.getPrivateKey(publicKey1);
    assertEquals(privateKey1, storedKey);
  }

  @Test
  public void testMissingKeyBehaviourIsNice() {
    MemoryConfig config = new MemoryConfig();
    config.setPrivateKeys(new File[] {new File("Does not exist")});
    config.setPublicKeys(new File[] {new File("Does not exist")});
    config.setAlwaysSendTo(new File[] {new File("Does not exist")});
    try {
      keyStore = new SodiumFileKeyStore(config, serializer);
      fail("should have thrown an EnclaveException");
    } catch (EnclaveException e) {
      // expected
    }
  }

  @Test
  public void testMissingAlgorithimRaisesAppropriateException() {
    MemoryConfig config = new MemoryConfig();
    config.setPrivateKeys(new File[] {new File("keys/noalgorithm.key")});
    config.setPublicKeys(new File[] {new File("keys/tm1a.pub")});
    config.setAlwaysSendTo(new File[] {new File("keys/tm1a.pub")});
    try {
      keyStore = new SodiumFileKeyStore(config, serializer);
      fail("should have thrown an EnclaveException");
    } catch (EnclaveException e) {
      // expected
    }
  }

  @Test
  public void testConfigLoadsMultipleKeys() {
    InputStream configAsStream =
        this.getClass().getClassLoader().getResourceAsStream("multipleKeyStoreTest.toml");

    Config config = configBuilder.build(configAsStream);
    keyStore = new SodiumFileKeyStore(config, serializer);
    String[] encodedPublicKeys =
        new String[] {
          "BULeR8JyUWhiuuCMU/HLA0Q5pzkYT+cHII3ZKBey3Bo=",
          "8SjRHlUBe4hAmTk3KDeJ96RhN+s10xRrHDrxEi1O5W0="
        };

    String[] encodedPrivateKeys =
        new String[] {
          "Wl+xSyXVuuqzpvznOS7dOobhcn4C5auxkFRi7yLtgtA=",
          "wGEar7J9G0JAgdisp61ZChyrJWeW2QPyKvecjjeVHOY="
        };

    for (int i = 0; i < encodedPrivateKeys.length; i++) {
      PrivateKey privateKey = new SodiumPrivateKey(Base64.decode(encodedPrivateKeys[i]));
      PublicKey publicKey = new SodiumPublicKey(Base64.decode(encodedPublicKeys[i]));
      PrivateKey storedKey = keyStore.getPrivateKey(publicKey);
      assertEquals(privateKey, storedKey);
    }
  }

  @Test
  public void testAlwaysSendTo() {
    InputStream configAsStream =
        this.getClass().getClassLoader().getResourceAsStream("alwaysSendToKeyStoreTest.toml");

    Config config = configBuilder.build(configAsStream);
    keyStore = new SodiumFileKeyStore(config, serializer);
    String[] encodedPublicKeys =
        new String[] {
          "BULeR8JyUWhiuuCMU/HLA0Q5pzkYT+cHII3ZKBey3Bo=",
          "8SjRHlUBe4hAmTk3KDeJ96RhN+s10xRrHDrxEi1O5W0="
        };

    PublicKey[] publicKeys = new PublicKey[encodedPublicKeys.length];
    for (int i = 0; i < encodedPublicKeys.length; i++) {
      PublicKey publicKey = new SodiumPublicKey(Base64.decode(encodedPublicKeys[i]));
      publicKeys[i] = publicKey;
    }
    assertArrayEquals(publicKeys, keyStore.alwaysSendTo());
  }

  public void testLoadOfPasswordProtectedKeys() {
    MemoryConfig config = new MemoryConfig();
    config.setPrivateKeys(new File[] {new File("keys/password.key")});
    config.setPublicKeys(new File[] {new File("keys/password.pub")});
    keyStore = new SodiumFileKeyStore(config, serializer);
  }

  @Test
  public void testGenerateUnlockedProtectedKeyPair() {
    String keyPrefix = "keys/generated";
    try {
      keyStore = new SodiumFileKeyStore(config, serializer);
      keyStore.generateKeyPair(new KeyConfig(keyPrefix, Optional.empty()));

      MemoryConfig config = new MemoryConfig();
      config.setPrivateKeys(new File[] {new File("keys/generated.key")});
      config.setPublicKeys(new File[] {new File("keys/generated.pub")});
      keyStore = new SodiumFileKeyStore(config, serializer);
    } finally {
      File privateKey = new File(keyPrefix + ".key");
      File publicKey = new File(keyPrefix + ".pub");
      if (privateKey.exists()) {
        privateKey.delete();
      }
      if (publicKey.exists()) {
        publicKey.delete();
      }
    }
  }

  @Test
  public void testGeneratePasswordProtectedKeyPair() {
    String keyPrefix = "keys/generated_password";
    try {
      keyStore = new SodiumFileKeyStore(config, serializer);
      keyStore.generateKeyPair(new KeyConfig(keyPrefix, Optional.of("yolo")));

      MemoryConfig config = new MemoryConfig();
      config.setPasswords(new File("keys/password.txt"));
      config.setPrivateKeys(new File[] {new File("keys/generated_password.key")});
      config.setPublicKeys(new File[] {new File("keys/generated_password.pub")});
      keyStore = new SodiumFileKeyStore(config, serializer);
    } finally {
      File privateKey = new File(keyPrefix + ".key");
      File publicKey = new File(keyPrefix + ".pub");
      if (privateKey.exists()) {
        privateKey.delete();
      }
      if (publicKey.exists()) {
        publicKey.delete();
      }
    }
  }
}

package net.consensys.orion.impl.config;

import static org.junit.Assert.*;

import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.config.ConfigException;
import net.consensys.orion.impl.enclave.sodium.LibSodiumSettings;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

public class TomlConfigBuilderTest {

  @Test
  public void fullFileRead() throws Exception {

    Path expectedFile;
    Path expectedFilesArray[];

    InputStream configAsStream = this.getClass().getClassLoader().getResourceAsStream("fullConfigTest.toml");
    TomlConfigBuilder configBuilder = new TomlConfigBuilder();

    Config testConf = configBuilder.build(configAsStream);

    URL expectedURL = new URL("http://127.0.0.1:9001/");
    assertEquals(expectedURL, testConf.nodeUrl());
    assertEquals(9001, testConf.nodePort());
    assertEquals("0.0.0.0", testConf.nodeNetworkInterface());
    assertEquals("memory", testConf.storage());
    assertEquals("off", testConf.tls());
    assertEquals("ca-or-tofu", testConf.tlsServerTrust());
    assertEquals("ca", testConf.tlsClientTrust());

    // Optionals
    final Path expectedWorkdir = Paths.get("data");
    assertEquals(expectedWorkdir, testConf.workDir());

    expectedFile = expectedWorkdir.resolve("orion.ipc");

    expectedFile = expectedWorkdir.resolve("keys/password.txt");
    assertTrue(testConf.passwords().isPresent());
    assertEquals(expectedFile, testConf.passwords().get());

    // File Arrays
    expectedFilesArray = new Path[1];
    expectedFilesArray[0] = expectedWorkdir.resolve("keys/tm1.pub");
    assertArrayEquals(expectedFilesArray, testConf.publicKeys());

    expectedFilesArray = new Path[1];
    expectedFilesArray[0] = expectedWorkdir.resolve("keys/tm1.key");
    assertArrayEquals(expectedFilesArray, testConf.privateKeys());

    expectedFilesArray = new Path[1];
    expectedFilesArray[0] = expectedWorkdir.resolve("keys/tm1.pub");
    assertArrayEquals(expectedFilesArray, testConf.alwaysSendTo());

    expectedFilesArray = new Path[0];
    assertArrayEquals(expectedFilesArray, testConf.tlsServerChain());

    expectedFilesArray = new Path[0];
    assertArrayEquals(expectedFilesArray, testConf.tlsClientChain());

    // URL Array
    URL[] expectedURLArray = new URL[1];
    expectedURLArray[0] = new URL("http://127.0.0.1:9000/");
    assertArrayEquals(expectedURLArray, testConf.otherNodes());

    // Files
    expectedFile = expectedWorkdir.resolve("server-cert.pem");
    assertEquals(expectedFile, testConf.tlsServerCert());

    expectedFile = expectedWorkdir.resolve("server-key.pem");
    assertEquals(expectedFile, testConf.tlsServerKey());

    expectedFile = expectedWorkdir.resolve("known-clients");
    assertEquals(expectedFile, testConf.tlsKnownClients());

    expectedFile = expectedWorkdir.resolve("client-cert.pem");
    assertEquals(expectedFile, testConf.tlsClientCert());

    expectedFile = expectedWorkdir.resolve("client-key.pem");
    assertEquals(expectedFile, testConf.tlsClientKey());

    expectedFile = expectedWorkdir.resolve("known-servers");
    assertEquals(expectedFile, testConf.tlsKnownServers());

    assertEquals("/somepath", testConf.libSodiumPath());
  }

  @Test
  public void fullFileReadUsingDefaults() {

    InputStream configAsStream = this.getClass().getClassLoader().getResourceAsStream("defaultConfigTest.toml");
    TomlConfigBuilder configBuilder = new TomlConfigBuilder();

    Config testConf = configBuilder.build(configAsStream);

    File expectedEmptyFilesArray[] = new File[0];
    assertArrayEquals(expectedEmptyFilesArray, testConf.otherNodes());
    assertArrayEquals(expectedEmptyFilesArray, testConf.publicKeys());
    assertArrayEquals(expectedEmptyFilesArray, testConf.privateKeys());
    assertArrayEquals(expectedEmptyFilesArray, testConf.alwaysSendTo());
    assertArrayEquals(expectedEmptyFilesArray, testConf.tlsServerChain());
    assertArrayEquals(expectedEmptyFilesArray, testConf.tlsClientChain());

    assertEquals("leveldb", testConf.storage());
    assertEquals("strict", testConf.tls());
    assertEquals("tofu", testConf.tlsServerTrust());
    assertEquals("ca-or-tofu", testConf.tlsClientTrust());

    assertFalse(testConf.passwords().isPresent());

    Path expectedFile;
    expectedFile = Paths.get("tls-server-cert.pem");
    assertEquals(expectedFile, testConf.tlsServerCert());

    expectedFile = Paths.get("tls-server-key.pem");
    assertEquals(expectedFile, testConf.tlsServerKey());

    expectedFile = Paths.get("tls-known-clients");
    assertEquals(expectedFile, testConf.tlsKnownClients());

    expectedFile = Paths.get("tls-client-cert.pem");
    assertEquals(expectedFile, testConf.tlsClientCert());

    expectedFile = Paths.get("tls-client-key.pem");
    assertEquals(expectedFile, testConf.tlsClientKey());

    expectedFile = Paths.get("tls-known-servers");
    assertEquals(expectedFile, testConf.tlsKnownServers());

    assertEquals(LibSodiumSettings.defaultLibSodiumPath(), testConf.libSodiumPath());
  }

  @Test
  public void invalidConfigsThrowException() {

    InputStream configAsStream = this.getClass().getClassLoader().getResourceAsStream("invalidConfigTest.toml");
    TomlConfigBuilder configBuilder = new TomlConfigBuilder();

    try {
      Config testConf = configBuilder.build(configAsStream);
      fail("Expected Config Exception to be thrown");
    } catch (ConfigException e) {
      String message = "Invalid Configuration Options\n"
          + "Error: key 'nodeurl' in config is malformed.\n\tunknown protocol: htt\n"
          + "Error: key 'clienturl' in config is malformed.\n\tunknown protocol: htt\n"
          + "Error: value for key 'nodeport' in config must be different to 'clientport'\n"
          + "Error: key 'othernodes' in config contains malformed URLS.\n"
          + "\tURL [htt://127.0.0.1:9000/] unknown protocol: htt\n"
          + "\tURL [10.1.1.1] no protocol: 10.1.1.1\n"
          + "Error: the number of keys specified for keys 'publickeys' and 'privatekeys' must be the same\n"
          + "Error: value for key 'storage' type must start with: ['leveldb', 'mapdb'] or be 'memory'\n"
          + "Error: value for key 'tls' status must be 'strict' or 'off'\n"
          + "Error: value for key 'tlsservertrust' mode must must be one of ['whitelist', 'tofu', 'ca', 'ca-or-tofu', 'insecure-no-validation']\n"
          + "Error: value for key 'tlsclienttrust' mode must must be one of ['whitelist', 'tofu', 'ca', 'ca-or-tofu', 'insecure-no-validation']\n";
      assertEquals(message, e.getMessage());
    }
  }

  @Test
  public void trustModeValidation() {
    TomlConfigBuilder configBuilder = new TomlConfigBuilder();

    assertTrue(configBuilder.validateTrustMode("whitelist"));
    assertTrue(configBuilder.validateTrustMode("tofu"));
    assertTrue(configBuilder.validateTrustMode("ca"));
    assertTrue(configBuilder.validateTrustMode("ca-or-tofu"));
    assertTrue(configBuilder.validateTrustMode("insecure-no-validation"));

    assertFalse(configBuilder.validateTrustMode("invalid-trust-mode"));
    assertFalse(configBuilder.validateTrustMode("ca-or"));
    assertFalse(configBuilder.validateTrustMode("or-tofu"));
  }

  @Test
  public void tlsValidation() {
    TomlConfigBuilder configBuilder = new TomlConfigBuilder();

    assertTrue(configBuilder.validateTLS("strict"));
    assertTrue(configBuilder.validateTLS("off"));

    assertFalse(configBuilder.validateTLS("notValid"));
  }

  @Test
  public void storageValidationTypes() {
    TomlConfigBuilder configBuilder = new TomlConfigBuilder();
    assertTrue(configBuilder.validateStorageTypes("leveldb:path"));
    assertTrue(configBuilder.validateStorageTypes("memory"));
    assertTrue(configBuilder.validateStorageTypes("leveldb"));
    assertTrue(configBuilder.validateStorageTypes("mapdb"));

    assertFalse(configBuilder.validateStorageTypes("memoryX"));
    assertFalse(configBuilder.validateStorageTypes("invalidStorage"));
  }
}

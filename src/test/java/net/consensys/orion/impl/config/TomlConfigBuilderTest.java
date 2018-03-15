package net.consensys.orion.impl.config;

import static org.junit.Assert.*;

import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.config.ConfigException;
import net.consensys.orion.impl.enclave.sodium.LibSodiumSettings;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import org.junit.Test;

public class TomlConfigBuilderTest {

  @Test
  public void fullFileRead() throws Exception {

    File expectedFile;
    File expectedFilesArray[];

    InputStream configAsStream =
        this.getClass().getClassLoader().getResourceAsStream("fullConfigTest.toml");
    TomlConfigBuilder configBuilder = new TomlConfigBuilder();

    Config testConf = configBuilder.build(configAsStream);

    URL expectedURL = new URL("http://127.0.0.1:9001/");
    assertEquals(expectedURL, testConf.url());
    assertEquals(9001, testConf.port());
    assertEquals("memory", testConf.storage());
    assertEquals("off", testConf.tls());
    assertEquals("ca-or-tofu", testConf.tlsServerTrust());
    assertEquals("ca", testConf.tlsClientTrust());
    assertEquals(3, testConf.verbosity());

    // Optionals
    final String expectedWorkdir = "data";
    expectedFile = new File(expectedWorkdir);
    assertTrue(testConf.workDir().isPresent());
    assertEquals(expectedFile, testConf.workDir().get());

    expectedFile = new File(expectedWorkdir, "orion.ipc");
    assertTrue(testConf.socket().isPresent());
    assertEquals(expectedFile, testConf.socket().get());

    expectedFile = new File(expectedWorkdir, "keys/password.txt");
    assertTrue(testConf.passwords().isPresent());
    assertEquals(expectedFile, testConf.passwords().get());

    // File Arrays
    expectedFilesArray = new File[1];
    expectedFilesArray[0] = new File(expectedWorkdir, "keys/tm1.pub");
    assertArrayEquals(expectedFilesArray, testConf.publicKeys());

    expectedFilesArray = new File[1];
    expectedFilesArray[0] = new File(expectedWorkdir, "keys/tm1.key");
    assertArrayEquals(expectedFilesArray, testConf.privateKeys());

    expectedFilesArray = new File[1];
    expectedFilesArray[0] = new File(expectedWorkdir, "keys/tm1.pub");
    assertArrayEquals(expectedFilesArray, testConf.alwaysSendTo());

    expectedFilesArray = new File[0];
    assertArrayEquals(expectedFilesArray, testConf.tlsServerChain());

    expectedFilesArray = new File[0];
    assertArrayEquals(expectedFilesArray, testConf.tlsClientChain());

    // URL Array
    URL[] expectedURLArray = new URL[1];
    expectedURLArray[0] = new URL("http://127.0.0.1:9000/");
    assertArrayEquals(expectedURLArray, testConf.otherNodes());

    // String Array
    String expectedStringArray[] = {"10.0.0.1", "2001:0db8:85a3:0000:0000:8a2e:0370:7334"};
    assertArrayEquals(expectedStringArray, testConf.ipWhitelist());

    // Files
    expectedFile = new File(expectedWorkdir, "server-cert.pem");
    assertEquals(expectedFile, testConf.tlsServerCert());

    expectedFile = new File(expectedWorkdir, "server-key.pem");
    assertEquals(expectedFile, testConf.tlsServerKey());

    expectedFile = new File(expectedWorkdir, "known-clients");
    assertEquals(expectedFile, testConf.tlsKnownClients());

    expectedFile = new File(expectedWorkdir, "client-cert.pem");
    assertEquals(expectedFile, testConf.tlsClientCert());

    expectedFile = new File(expectedWorkdir, "client-key.pem");
    assertEquals(expectedFile, testConf.tlsClientKey());

    expectedFile = new File(expectedWorkdir, "known-servers");
    assertEquals(expectedFile, testConf.tlsKnownServers());

    assertEquals("/somepath", testConf.libSodiumPath());
  }

  @Test
  public void fullFileReadUsingDefaults() {

    InputStream configAsStream =
        this.getClass().getClassLoader().getResourceAsStream("defaultConfigTest.toml");
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
    assertEquals(1, testConf.verbosity());

    assertFalse(testConf.workDir().isPresent());
    assertFalse(testConf.socket().isPresent());
    assertFalse(testConf.passwords().isPresent());

    String expectedStringArray[] = new String[0];
    assertArrayEquals(expectedStringArray, testConf.ipWhitelist());

    File expectedFile;
    expectedFile = new File("tls-server-cert.pem");
    assertEquals(expectedFile, testConf.tlsServerCert());

    expectedFile = new File("tls-server-key.pem");
    assertEquals(expectedFile, testConf.tlsServerKey());

    expectedFile = new File("tls-known-clients");
    assertEquals(expectedFile, testConf.tlsKnownClients());

    expectedFile = new File("tls-client-cert.pem");
    assertEquals(expectedFile, testConf.tlsClientCert());

    expectedFile = new File("tls-client-key.pem");
    assertEquals(expectedFile, testConf.tlsClientKey());

    expectedFile = new File("tls-known-servers");
    assertEquals(expectedFile, testConf.tlsKnownServers());

    assertEquals(LibSodiumSettings.defaultLibSodiumPath(), testConf.libSodiumPath());
  }

  @Test
  public void invalidConfigsThrowException() {

    InputStream configAsStream =
        this.getClass().getClassLoader().getResourceAsStream("invalidConfigTest.toml");
    TomlConfigBuilder configBuilder = new TomlConfigBuilder();

    try {
      Config testConf = configBuilder.build(configAsStream);
      fail("Expected Config Exception to be thrown");
    } catch (ConfigException e) {
      String message =
          "Invalid Configuration Options\n"
              + "Error: key 'url' in config is malformed.\n\tunknown protocol: htt\n"
              + "Error: key 'othernodes' in config containes malformed URLS.\n"
              + "\tURL [htt://127.0.0.1:9000/] unknown protocol: htt\n"
              + "\tURL [10.1.1.1] no protocol: 10.1.1.1\n"
              + "Error: the number of keys specified for keys 'publickeys' and 'privatekeys' must be the same\n"
              + "Error: value for key 'storage' type must start with: ['leveldb', 'mapdb'] or be 'memory'\n"
              + "Error: value for key 'tls' status must be 'strict' or 'off'\n"
              + "Error: value for key 'tlsservertrust' mode must must be one of ['whitelist', 'tofu', 'ca', 'ca-or-tofu', 'insecure-no-validation']\n"
              + "Error: value for key 'tlsclienttrust' mode must must be one of ['whitelist', 'tofu', 'ca', 'ca-or-tofu', 'insecure-no-validation']\n"
              + "Error: value for key 'verbosity' must be within range 0 to 3\n";
      assertEquals(message, e.getMessage());
    }
  }

  @Test
  public void missingMandatoryConfigsThrowException() {

    InputStream configAsStream =
        this.getClass().getClassLoader().getResourceAsStream("missingMandatoryConfigTest.toml");
    TomlConfigBuilder configBuilder = new TomlConfigBuilder();

    try {
      Config testConf = configBuilder.build(configAsStream);
      fail("Expected Config Exception to be thrown");
    } catch (ConfigException e) {
      String message =
          "Invalid Configuration Options\n"
              + "Error: value for key 'url' in config must be specified\n"
              + "Error: value for key 'port' in config must be specified\n";
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
  public void verbosityValidation() {
    TomlConfigBuilder configBuilder = new TomlConfigBuilder();

    assertTrue(configBuilder.validateVerbosity(0));
    assertTrue(configBuilder.validateVerbosity(3));

    assertFalse(configBuilder.validateVerbosity(-1));
    assertFalse(configBuilder.validateVerbosity(4));
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

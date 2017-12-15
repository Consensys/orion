package net.consensys.athena.impl.config;

import static org.junit.Assert.*;

import net.consensys.athena.api.config.Config;
import net.consensys.athena.api.config.ConfigException;

import java.io.File;
import java.io.InputStream;

import org.junit.Test;

public class TomlConfigBuilderTest {

  @Test
  public void testFullFileRead() throws Exception {

    File expectedFile;
    File expectedFilesArray[];

    InputStream configAsStream =
        this.getClass().getClassLoader().getResourceAsStream("fullConfigTest.toml");
    TomlConfigBuilder configBuilder = new TomlConfigBuilder();

    Config testConf = configBuilder.build(configAsStream);

    assertEquals("http://127.0.0.1:9001/", testConf.url());
    assertEquals(9001, testConf.port());
    assertEquals("memory", testConf.storage());
    assertEquals("off", testConf.tls());
    assertEquals("ca-or-tofu", testConf.tlsServerTrust());
    assertEquals("ca", testConf.tlsClientTrust());
    assertEquals(3, testConf.verbosity());

    // Optionals
    expectedFile = new File("data");
    assertTrue(testConf.workDir().isPresent());
    assertEquals(expectedFile, testConf.workDir().get());

    expectedFile = new File("athena.ipc");
    assertTrue(testConf.socket().isPresent());
    assertEquals(expectedFile, testConf.socket().get());

    expectedFile = new File("passwords");
    assertTrue(testConf.passwords().isPresent());
    assertEquals(expectedFile, testConf.passwords().get());

    // File Arrays
    expectedFilesArray = new File[1];
    expectedFilesArray[0] = new File("http://127.0.0.1:9000/");
    assertArrayEquals(expectedFilesArray, testConf.otherNodes());

    expectedFilesArray = new File[1];
    expectedFilesArray[0] = new File("foo.pub");
    assertArrayEquals(expectedFilesArray, testConf.publicKeys());

    expectedFilesArray = new File[1];
    expectedFilesArray[0] = new File("foo.key");
    assertArrayEquals(expectedFilesArray, testConf.privateKeys());

    expectedFilesArray = new File[1];
    expectedFilesArray[0] = new File("http://127.0.0.1:9000/");
    assertArrayEquals(expectedFilesArray, testConf.alwaysSendTo());

    expectedFilesArray = new File[0];
    assertArrayEquals(expectedFilesArray, testConf.tlsServerChain());

    expectedFilesArray = new File[0];
    assertArrayEquals(expectedFilesArray, testConf.tlsClientChain());

    // String Array
    String expectedStringArray[] = {"10.0.0.1", "2001:0db8:85a3:0000:0000:8a2e:0370:7334"};
    assertArrayEquals(expectedStringArray, testConf.ipWhitelist());

    // Files
    expectedFile = new File("server-cert.pem");
    assertEquals(expectedFile, testConf.tlsServerCert());

    expectedFile = new File("server-key.pem");
    assertEquals(expectedFile, testConf.tlsServerKey());

    expectedFile = new File("known-clients");
    assertEquals(expectedFile, testConf.tlsKnownClients());

    expectedFile = new File("client-cert.pem");
    assertEquals(expectedFile, testConf.tlsClientCert());

    expectedFile = new File("client-key.pem");
    assertEquals(expectedFile, testConf.tlsClientKey());

    expectedFile = new File("known-servers");
    assertEquals(expectedFile, testConf.tlsKnownServers());
  }

  @Test
  public void testFullFileReadUsingDefaults() throws Exception {

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

    assertEquals("dir:storage", testConf.storage());
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
  }

  @Test
  public void testInvalidConfigsThrowException() throws Exception {

    InputStream configAsStream =
        this.getClass().getClassLoader().getResourceAsStream("invalidConfigTest.toml");
    TomlConfigBuilder configBuilder = new TomlConfigBuilder();

    try {
      Config testConf = configBuilder.build(configAsStream);
      fail("Expected Config Exception to be thrown");
    } catch (ConfigException e) {
      String message =
          "Invalid Configuration Options\n"
              + "Error: the number of keys specified for keys 'publickeys' and 'privatekeys' must be the same\n"
              + "Error: value for key 'storage' type must start with: ['bdp:', 'dir:', 'leveldb:', 'sqllite:'] or be 'memory'\n"
              + "Error: value for key 'tls' status must be 'strict' or 'off'\n"
              + "Error: value for key 'tlsservertrust' mode must must be one of ['whitelist', 'tofu', 'ca', 'ca-or-tofu', 'insecure-no-validation']\n"
              + "Error: value for key 'tlsclienttrust' mode must must be one of ['whitelist', 'tofu', 'ca', 'ca-or-tofu', 'insecure-no-validation']\n"
              + "Error: value for key 'verbosity' must be within range 0 to 3\n";
      assertEquals(message, e.getMessage());
    }
  }

  @Test
  public void testMissingMandatoryConfigsThrowException() throws Exception {

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
  public void testMissingStoragePathThrowException() throws Exception {

    InputStream configAsStream =
        this.getClass()
            .getClassLoader()
            .getResourceAsStream("fullConfigMissingStoragePathTest.toml");
    TomlConfigBuilder configBuilder = new TomlConfigBuilder();

    try {
      Config testConf = configBuilder.build(configAsStream);
      fail("Expected Config Exception to be thrown");
    } catch (ConfigException e) {
      String message =
          "Invalid Configuration Options\n"
              + "Error: value for key 'storage' of types ['bdp:', 'dir:', 'leveldb:', 'sqllite:'] must specify a path\n";
      assertEquals(message, e.getMessage());
    }
  }

  @Test
  public void testTrustModeValidation() throws Exception {
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
  public void testVerbosityValidation() throws Exception {
    TomlConfigBuilder configBuilder = new TomlConfigBuilder();

    assertTrue(configBuilder.validateVerbosity(0));
    assertTrue(configBuilder.validateVerbosity(3));

    assertFalse(configBuilder.validateVerbosity(-1));
    assertFalse(configBuilder.validateVerbosity(4));
  }

  @Test
  public void testTLSValidation() throws Exception {
    TomlConfigBuilder configBuilder = new TomlConfigBuilder();

    assertTrue(configBuilder.validateTLS("strict"));
    assertTrue(configBuilder.validateTLS("off"));

    assertFalse(configBuilder.validateTLS("notValid"));
  }

  @Test
  public void testStorageValidationTypes() throws Exception {
    TomlConfigBuilder configBuilder = new TomlConfigBuilder();
    assertTrue(configBuilder.validateStorageTypes("bdp:path"));
    assertTrue(configBuilder.validateStorageTypes("dir:path"));
    assertTrue(configBuilder.validateStorageTypes("leveldb:path"));
    assertTrue(configBuilder.validateStorageTypes("memory"));
    assertTrue(configBuilder.validateStorageTypes("sqlite:path"));

    assertFalse(configBuilder.validateStorageTypes("memoryX"));
    assertFalse(configBuilder.validateStorageTypes("invalidStorage"));
  }

  @Test
  public void testStorageValidationPathsExist() throws Exception {
    TomlConfigBuilder configBuilder = new TomlConfigBuilder();
    assertTrue(configBuilder.validateStoragePathsExist("bdp:path"));
    assertTrue(configBuilder.validateStoragePathsExist("dir:path"));
    assertTrue(configBuilder.validateStoragePathsExist("leveldb:path"));
    assertTrue(configBuilder.validateStoragePathsExist("memory"));
    assertTrue(configBuilder.validateStoragePathsExist("sqlite:path"));

    assertFalse(configBuilder.validateStoragePathsExist("bdp:"));
    assertFalse(configBuilder.validateStoragePathsExist("dir:"));
    assertFalse(configBuilder.validateStoragePathsExist("leveldb:"));
    assertFalse(configBuilder.validateStoragePathsExist("sqlite:"));
  }
}

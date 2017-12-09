package net.consensys.athena.impl.config;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.consensys.athena.api.config.Config;

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
    assertEquals(4, testConf.verbosity());

    // Optionals
    expectedFile = new File("data");
    assertTrue(testConf.workDir().isPresent());
    assertEquals(expectedFile, testConf.workDir().get());

    expectedFile = new File("constellation.ipc");
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

    expectedFilesArray = new File[0];
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
}

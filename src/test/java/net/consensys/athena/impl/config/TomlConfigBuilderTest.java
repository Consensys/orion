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

    //Optional
    expectedFile = new File("data");
    assertTrue(testConf.workDir().isPresent());
    assertEquals(expectedFile, testConf.workDir().get());

    //Optional
    expectedFile = new File("constellation.ipc");
    assertTrue(testConf.socket().isPresent());
    assertEquals(expectedFile, testConf.socket().get());

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

    //Optional
    expectedFile = new File("passwords");
    assertTrue(testConf.passwords().isPresent());
    assertEquals(expectedFile, testConf.passwords().get());

    assertEquals("dir:storage", testConf.storage());

    String expectedStringArray[] = {"10.0.0.1", "2001:0db8:85a3:0000:0000:8a2e:0370:7334"};
    assertArrayEquals(expectedStringArray, testConf.ipWhitelist());

    assertEquals("strict", testConf.tls());
    expectedFile = new File("tls-server-cert.pem");
    assertEquals(expectedFile, testConf.tlsServerCert());

    expectedFilesArray = new File[0];
    assertArrayEquals(expectedFilesArray, testConf.tlsServerChain());

    expectedFile = new File("tls-server-key.pem");
    assertEquals(expectedFile, testConf.tlsServerKey());
    assertEquals("tofu", testConf.tlsServerTrust());
    expectedFile = new File("tls-known-clients");
    assertEquals(expectedFile, testConf.tlsKnownClients());
    expectedFile = new File("tls-client-cert.pem");
    assertEquals(expectedFile, testConf.tlsClientCert());

    expectedFilesArray = new File[0];
    assertArrayEquals(expectedFilesArray, testConf.tlsClientChain());

    expectedFile = new File("tls-client-key.pem");
    assertEquals(expectedFile, testConf.tlsClientKey());
    assertEquals("ca-or-tofu", testConf.tlsClientTrust());
    expectedFile = new File("tls-known-servers");
    assertEquals(expectedFile, testConf.tlsKnownServers());
    assertEquals(1, testConf.verbosity());
  }
}

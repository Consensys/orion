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
package net.consensys.orion.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TomlConfigTest {

  @Test
  void fullFileRead() throws Exception {
    final InputStream configAsStream = this.getClass().getClassLoader().getResourceAsStream("fullConfigTest.toml");
    final Config testConf = Config.load(configAsStream, Collections.emptyMap());

    final URL expectedURL = new URL("http://127.0.0.1:9001/");
    assertEquals(expectedURL, testConf.nodeUrl().get());
    assertEquals(9001, testConf.nodePort());
    assertEquals("0.0.0.0", testConf.nodeNetworkInterface());
    assertEquals("memory", testConf.storage());
    assertEquals("mapdb:knownnodesdb", testConf.knownNodesStorage());
    assertEquals("off", testConf.tls());
    assertEquals("ca-or-tofu", testConf.tlsServerTrust());
    assertEquals("ca", testConf.tlsClientTrust());
    assertEquals("off", testConf.clientConnectionTls());
    assertEquals("ca-or-tofu", testConf.clientConnectionTlsServerTrust());

    // Files
    final Path expectedWorkdir = Paths.get("data");
    assertEquals(expectedWorkdir, testConf.workDir());
    assertTrue(testConf.passwords().isPresent());
    assertEquals(expectedWorkdir.resolve("keys/password.txt"), testConf.passwords().get());
    assertEquals(expectedWorkdir.resolve("server-cert.pem"), testConf.tlsServerCert());
    assertEquals(expectedWorkdir.resolve("server-key.pem"), testConf.tlsServerKey());
    assertEquals(expectedWorkdir.resolve("known-clients"), testConf.tlsKnownClients());
    assertEquals(expectedWorkdir.resolve("client-cert.pem"), testConf.tlsClientCert());
    assertEquals(expectedWorkdir.resolve("client-key.pem"), testConf.tlsClientKey());
    assertEquals(expectedWorkdir.resolve("known-servers"), testConf.tlsKnownServers());
    assertEquals(expectedWorkdir.resolve("client-presented-cert.pem"), testConf.clientConnectionTlsServerCert());
    assertEquals(
        expectedWorkdir.resolve("client-connection-known-clients"),
        testConf.clientConnectionTlsKnownClients());
    assertEquals(Paths.get("/somepath"), testConf.libSodiumPath());



    // File Arrays
    assertEquals(Collections.singletonList(expectedWorkdir.resolve("keys/tm1.pub")), testConf.publicKeys());
    assertEquals(Collections.singletonList(expectedWorkdir.resolve("keys/tm1.key")), testConf.privateKeys());
    assertEquals(Collections.singletonList(expectedWorkdir.resolve("keys/tm1.pub")), testConf.alwaysSendTo());
    assertEquals(Collections.emptyList(), testConf.tlsServerChain());
    assertEquals(Collections.emptyList(), testConf.tlsClientChain());
    assertEquals(Collections.emptyList(), testConf.clientConnectionTlsServerChain());

    // URL Array
    assertEquals(Collections.singletonList(URI.create("http://127.0.0.1:9000/")), testConf.otherNodes());
  }

  @Test
  void testEnvOverride() throws Exception {
    final InputStream configAsStream = this.getClass().getClassLoader().getResourceAsStream("fullConfigTest.toml");
    Map<String, String> overrides = new HashMap<>();
    overrides.put("ORION_NODEPORT", "10001");
    overrides.put("ORION_NODENETWORKINTERFACE", "192.168.0.1");
    overrides.put("ORION_NODEURL", "http://192.168.0.1:10001/");
    overrides.put("ORION_STORAGE", "mapdb:somefolder");
    overrides.put("ORION_KNOWNNODESSTORAGE", "mapdb:someotherfolder");
    overrides.put("ORION_OTHERNODES", "foo,bar,noes");
    overrides.put("ORION_TLS", "on");
    overrides.put("ORION_TLSSERVERTRUST", "ca");
    overrides.put("ORION_PASSWORDS", "/etc/pass");

    final Config testConf = Config.load(configAsStream, overrides);

    final URL expectedURL = new URL("http://192.168.0.1:10001/");
    assertEquals(expectedURL, testConf.nodeUrl().get());
    assertEquals(10001, testConf.nodePort());
    assertEquals("192.168.0.1", testConf.nodeNetworkInterface());
    assertEquals("mapdb:somefolder", testConf.storage());
    assertEquals("mapdb:someotherfolder", testConf.knownNodesStorage());
    assertEquals(Arrays.asList(URI.create("foo"), URI.create("bar"), URI.create("noes")), testConf.otherNodes());

    assertEquals("on", testConf.tls());
    assertEquals("ca", testConf.tlsServerTrust());
    assertEquals(Paths.get("/etc/pass"), testConf.passwords().get());
  }

  @Test
  void fullFileReadUsingDefaults() throws Exception {
    final Config testConf = Config
        .load(this.getClass().getClassLoader().getResourceAsStream("defaultConfigTest.toml"), Collections.emptyMap());

    assertEquals("leveldb", testConf.storage());
    assertEquals("memory", testConf.knownNodesStorage());
    assertEquals("off", testConf.tls());
    assertEquals("tofu", testConf.tlsServerTrust());
    assertEquals("ca-or-tofu", testConf.tlsClientTrust());

    assertFalse(testConf.passwords().isPresent());
    final Path workDir = Paths.get("data");
    assertEquals(workDir.resolve("tls-server-cert.pem"), testConf.tlsServerCert());
    assertEquals(workDir.resolve("tls-server-key.pem"), testConf.tlsServerKey());
    assertEquals(workDir.resolve("tls-known-clients"), testConf.tlsKnownClients());
    assertEquals(workDir.resolve("tls-client-cert.pem"), testConf.tlsClientCert());
    assertEquals(workDir.resolve("tls-client-key.pem"), testConf.tlsClientKey());
    assertEquals(workDir.resolve("tls-known-servers"), testConf.tlsKnownServers());
    assertNull(testConf.libSodiumPath());
  }

  @Test
  void invalidConfigsThrowException() {
    final ConfigException e = assertThrows(
        ConfigException.class,
        () -> Config.load(this.getClass().getClassLoader().getResourceAsStream("invalidConfigTest.toml")));
    final String tuweniPrefix = "org.apache.tuweni.config.ConfigurationError: ";
    final String message = tuweniPrefix
        + "Value of property 'clienturl' is not a valid URL (line 4, column 1)\n"
        + tuweniPrefix
        + "Value of property 'storage' must have storage type of \"leveldb\", \"mapdb\", \"sql\" or \"memory\" (line 11, column 1)\n"
        + tuweniPrefix
        + "Value of property 'othernodes' is not a valid URL (line 6, column 1)\n"
        + tuweniPrefix
        + "Value of property 'othernodes' is not a valid URL (line 6, column 1)\n"
        + tuweniPrefix
        + "Value of property 'tlsservertrust' should be \"whitelist\", \"ca\", \"ca-or-whitelist\", \"tofu\", \"insecure-tofa\", \"ca-or-tofu\", \"insecure-ca-or-tofa\", \"insecure-no-validation\", \"insecure-record\", or \"insecure-ca-or-record\" (line 9, column 1)\n"
        + "...";
    assertEquals(message, e.getMessage());
  }

  @Test
  void trustModeValidation() {
    Config.load("tlsservertrust=\"whitelist\"");
    Config.load("tlsservertrust=\"tofu\"");
    Config.load("tlsservertrust=\"ca\"");
    Config.load("tlsservertrust=\"ca-or-tofu\"");
    Config.load("tlsservertrust=\"insecure-no-validation\"");

    assertThrows(ConfigException.class, () -> Config.load("tlsservertrust=\"invalid-trust-mode\""));
    assertThrows(ConfigException.class, () -> Config.load("tlsservertrust=\"ca-or\""));
    assertThrows(ConfigException.class, () -> Config.load("tlsservertrust=\"or-tofu\""));
  }

  @Test
  void tlsValidation() {
    Config.load("tls=\"strict\"");
    Config.load("tls=\"off\"");
    assertThrows(ConfigException.class, () -> Config.load("tls=\"notValid\""));
  }

  @Test
  void storageValidationTypes() {
    Config.load("storage=\"leveldb:path\"");
    Config.load("storage=\"memory\"");
    Config.load("storage=\"leveldb\"");
    Config.load("storage=\"mapdb\"");
    Config.load("storage=\"sql:url\"");
    assertThrows(ConfigException.class, () -> Config.load("storage=\"memoryX\""));
    assertThrows(ConfigException.class, () -> Config.load("storage=\"invalidStorage\""));
  }
}

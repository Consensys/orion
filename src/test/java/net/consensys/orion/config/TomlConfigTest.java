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
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.junit.jupiter.api.Test;

class TomlConfigTest {

  @Test
  void fullFileRead() throws Exception {
    InputStream configAsStream = this.getClass().getClassLoader().getResourceAsStream("fullConfigTest.toml");
    Config testConf = Config.load(configAsStream);

    URL expectedURL = new URL("http://127.0.0.1:9001/");
    assertEquals(expectedURL, testConf.nodeUrl());
    assertEquals(9001, testConf.nodePort());
    assertEquals("0.0.0.0", testConf.nodeNetworkInterface());
    assertEquals("memory", testConf.storage());
    assertEquals("off", testConf.tls());
    assertEquals("ca-or-tofu", testConf.tlsServerTrust());
    assertEquals("ca", testConf.tlsClientTrust());

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
    assertEquals(Paths.get("/somepath"), testConf.libSodiumPath());

    // File Arrays
    assertEquals(Collections.singletonList(expectedWorkdir.resolve("keys/tm1.pub")), testConf.publicKeys());
    assertEquals(Collections.singletonList(expectedWorkdir.resolve("keys/tm1.key")), testConf.privateKeys());
    assertEquals(Collections.singletonList(expectedWorkdir.resolve("keys/tm1.pub")), testConf.alwaysSendTo());
    assertEquals(Collections.emptyList(), testConf.tlsServerChain());
    assertEquals(Collections.emptyList(), testConf.tlsClientChain());

    // URL Array
    assertEquals(Collections.singletonList(new URL("http://127.0.0.1:9000/")), testConf.otherNodes());
  }

  @Test
  void fullFileReadUsingDefaults() throws Exception {
    Config testConf = Config.load(this.getClass().getClassLoader().getResourceAsStream("defaultConfigTest.toml"));

    assertEquals("leveldb", testConf.storage());
    assertEquals("off", testConf.tls());
    assertEquals("tofu", testConf.tlsServerTrust());
    assertEquals("ca-or-tofu", testConf.tlsClientTrust());

    assertFalse(testConf.passwords().isPresent());
    Path workDir = Paths.get("data");
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
    ConfigException e = assertThrows(
        ConfigException.class,
        () -> Config.load(this.getClass().getClassLoader().getResourceAsStream("invalidConfigTest.toml")));
    String message = "Value of property 'clienturl' is not a valid URL (line 4, column 1)\n"
        + "Value of property 'storage' must have storage type of \"leveldb\", \"mapdb\", \"sql\" or \"memory\" (line 11, column 1)\n"
        + "Value of property 'othernodes' is not a valid URL (line 6, column 1)\n"
        + "Value of property 'othernodes' is not a valid URL (line 6, column 1)\n"
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

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
package net.consensys.orion.http;

import static io.vertx.core.Vertx.vertx;
import static net.consensys.orion.TestUtils.configureJDKTrustStore;
import static net.consensys.orion.TestUtils.generateAndLoadConfiguration;
import static net.consensys.orion.TestUtils.writeClientConnectionServerCertToConfig;
import static net.consensys.orion.TestUtils.writeServerCertToConfig;
import static org.apache.tuweni.net.tls.TLS.certificateHexFingerprint;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.consensys.orion.cmd.Orion;
import net.consensys.orion.config.Config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.net.ssl.SSLHandshakeException;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.net.SelfSignedCertificate;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.concurrent.CompletableAsyncResult;
import org.apache.tuweni.junit.TempDirectory;
import org.apache.tuweni.junit.TempDirectoryExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class CAOrTofuSecurityTest {

  private final static Vertx vertx = vertx();
  private static String exampleComFingerprint;
  private static HttpClient nonCAhttpClient;
  private static HttpClient httpClient;
  private static Orion orion;
  private static Config config;
  private final static String TRUST_MODE = "ca-or-tofu";

  @BeforeAll
  static void setUp(@TempDirectory final Path tempDir) throws Exception {
    final SelfSignedCertificate serverCertificate = SelfSignedCertificate.create("localhost");
    config = generateAndLoadConfiguration(tempDir, writer -> {
      writer.write("tlsservertrust='" + TRUST_MODE + "'\n");
      writer.write("clientconnectiontls='strict'\n");
      writer.write("clientconnectiontlsservertrust='" + TRUST_MODE + "'\n");
      writeServerCertToConfig(writer, serverCertificate);
      writeClientConnectionServerCertToConfig(writer, serverCertificate);
    });

    final SelfSignedCertificate nonCAClientCertificate = SelfSignedCertificate.create("example.com");
    exampleComFingerprint = certificateHexFingerprint(Paths.get(nonCAClientCertificate.keyCertOptions().getCertPath()));
    nonCAhttpClient = vertx.createHttpClient(
        new HttpClientOptions().setSsl(true).setTrustAll(true).setKeyCertOptions(
            nonCAClientCertificate.keyCertOptions()));

    final SelfSignedCertificate clientCert = SelfSignedCertificate.create("other.com");
    configureJDKTrustStore(clientCert, tempDir);
    httpClient = vertx.createHttpClient(
        new HttpClientOptions().setSsl(true).setTrustAll(true).setKeyCertOptions(clientCert.keyCertOptions()));

    orion = new Orion(vertx);
    orion.run(config, false);
  }

  @AfterAll
  static void tearDown() {
    System.clearProperty("javax.net.ssl.trustStore");
    System.clearProperty("javax.net.ssl.trustStorePassword");
    orion.stop();
    vertx.close();
  }

  @Test
  void testNodePort() throws Exception {
    connectionsOnPortAreSuccesfulAndUpdateKnownClientsFile(config.nodePort(), config.tlsKnownClients());
  }

  @Test
  void testClientPort() throws Exception {
    connectionsOnPortAreSuccesfulAndUpdateKnownClientsFile(
        config.clientPort(),
        config.clientConnectionTlsKnownClients());
  }

  private void connectionsOnPortAreSuccesfulAndUpdateKnownClientsFile(final int port, final Path clientFingerprintFile)
      throws Exception {
    {
      final HttpClientRequest req = nonCAhttpClient.get(port, "localhost", "/upcheck");
      final CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
      req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();
      final HttpClientResponse resp = result.get();
      assertEquals(200, resp.statusCode());
    }
    List<String> fingerprints = Files.readAllLines(clientFingerprintFile);
    assertEquals(1, fingerprints.size(), String.join("\n", fingerprints));
    assertEquals("example.com " + exampleComFingerprint, fingerprints.get(0));

    final HttpClientRequest req = httpClient.get(port, "localhost", "/upcheck");
    final CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
    req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();
    final HttpClientResponse resp = result.get();
    assertEquals(200, resp.statusCode());

    fingerprints = Files.readAllLines(clientFingerprintFile);
    assertEquals(1, fingerprints.size(), String.join("\n", fingerprints));
  }

  @Test
  void accessingNodePortWithoutSSLConfigurationFails() {
    assertThrows(SSLHandshakeException.class, () -> upcheckFailsUsingNonTlsClient(config.nodePort()));
  }

  @Test
  void accessingClientPortWithoutSSLConfigurationFails() {
    assertThrows(SSLHandshakeException.class, () -> upcheckFailsUsingNonTlsClient(config.clientPort()));
  }

  private void upcheckFailsUsingNonTlsClient(final int orionPort) throws Exception {
    final OkHttpClient unsecureHttpClient = new OkHttpClient.Builder().build();

    final Request upcheckRequest = new Request.Builder().url("https://localhost:" + orionPort + "/upcheck").build();
    unsecureHttpClient.newCall(upcheckRequest).execute();
  }
}

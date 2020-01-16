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
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.orion.cmd.Orion;
import net.consensys.orion.config.Config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionException;
import javax.net.ssl.SSLHandshakeException;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.net.SelfSignedCertificate;
import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.concurrent.CompletableAsyncResult;
import org.apache.tuweni.junit.TempDirectory;
import org.apache.tuweni.junit.TempDirectoryExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class TofuSecurityTest {

  private final Vertx vertx = vertx();
  private String exampleComFingerprint;
  private HttpClient httpClient;
  private HttpClient anotherExampleComClient;
  private Orion orion;
  private static Config config;
  private static String TRUST_MODE = "tofu";

  @BeforeEach
  void setUp(@TempDirectory final Path tempDir) throws Exception {
    final SelfSignedCertificate serverCertificate = SelfSignedCertificate.create("localhost");
    config = generateAndLoadConfiguration(tempDir, writer -> {
      writer.write("tlsservertrust='" + TRUST_MODE + "'\n");
      writer.write("clientconnectiontls='strict'\n");
      writer.write("clientconnectiontlsservertrust='" + TRUST_MODE + "'\n");
      writeServerCertToConfig(writer, serverCertificate);
      writeClientConnectionServerCertToConfig(writer, serverCertificate);
    });

    configureJDKTrustStore(serverCertificate, tempDir);

    final SelfSignedCertificate clientCertificate = SelfSignedCertificate.create("example.com");
    exampleComFingerprint =
        certificateHexFingerprint(Paths.get(clientCertificate.keyCertOptions().getCertPath()));
    Files.write(config.tlsKnownClients(), Collections.singletonList("#First line"));
    Files.write(config.clientConnectionTlsKnownClients(), Collections.singletonList("#First line"));
    httpClient = vertx
        .createHttpClient(new HttpClientOptions().setSsl(true)
            .setKeyCertOptions(clientCertificate.keyCertOptions()));

    final SelfSignedCertificate anotherExampleDotComCert =
        SelfSignedCertificate.create("example.com");
    anotherExampleComClient = vertx.createHttpClient(
        new HttpClientOptions().setSsl(true)
            .setKeyCertOptions(anotherExampleDotComCert.keyCertOptions()));

    orion = new Orion(vertx);
    orion.run(System.out, System.err, config);
  }

  @AfterEach
  void tearDown() {
    orion.stop();
    vertx.close();
  }

  @Test
  void testUpCheckOnNodePort() throws Exception {
    assertUpCheckOnPortIsSuccessfulAndUpdatesKnownClientsFile(config.nodePort(),
        config.tlsKnownClients());
    assertUpCheckOnPortIsSuccessfulAndUpdatesKnownClientsFile(config.clientPort(),
        config.clientConnectionTlsKnownClients());
  }

  void assertUpCheckOnPortIsSuccessfulAndUpdatesKnownClientsFile(final int port,
      final Path knownClientsFile) throws Exception {
    for (int i = 0; i < 5; i++) {
      final HttpClientRequest req = httpClient.get(port, "localhost", "/upcheck");
      final CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
      req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();
      final HttpClientResponse resp = result.get();
      assertEquals(200, resp.statusCode());
    }
    final List<String> fingerprints = Files.readAllLines(knownClientsFile);
    assertEquals(2, fingerprints.size(), String.join("\n", fingerprints));
    assertEquals("#First line", fingerprints.get(0));
    assertEquals("example.com " + exampleComFingerprint, fingerprints.get(1));
  }


  @Test
  void testSameHostnameUnknownCertificate() throws Exception {

    assertTwoDifferentClientsCannotConnectUsingSameCredentials(config.nodePort(),
        config.tlsKnownClients());
    assertTwoDifferentClientsCannotConnectUsingSameCredentials(config.clientPort(),
        config.clientConnectionTlsKnownClients());
  }

  void assertTwoDifferentClientsCannotConnectUsingSameCredentials(final int port,
      final Path knownClients) throws Exception {
    assertUpCheckOnPortIsSuccessfulAndUpdatesKnownClientsFile(port, knownClients);
    final HttpClientRequest req = anotherExampleComClient.get(port, "localhost", "/upcheck");
    final CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
    req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();

    final CompletionException e = assertThrows(CompletionException.class, result::get);
    assertEquals("Received fatal alert: certificate_unknown", e.getCause().getCause().getMessage());
  }

  @Test
  void testWithoutSSLConfiguration() {
    final CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
    final HttpClient insecureClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true));

    final HttpClientRequest req = insecureClient.get(config.nodePort(), "localhost", "/upcheck");
    req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();

    final CompletionException e = assertThrows(CompletionException.class, result::get);
    assertTrue(e.getCause() instanceof SSLHandshakeException);

    final HttpClientRequest clientRequest = insecureClient.get(config.clientPort(), "localhost", "/upcheck");
    clientRequest.handler(result::complete).exceptionHandler(result::completeExceptionally).end();

    final CompletionException clientException = assertThrows(CompletionException.class, result::get);
    assertTrue(clientException.getCause() instanceof SSLHandshakeException);
  }
}

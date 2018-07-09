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

package net.consensys.orion.impl.http;

import static io.vertx.core.Vertx.vertx;
import static net.consensys.cava.net.tls.TLS.certificateHexFingerprint;
import static net.consensys.orion.impl.TestUtils.configureJDKTrustStore;
import static net.consensys.orion.impl.TestUtils.generateAndLoadConfiguration;
import static net.consensys.orion.impl.TestUtils.writeServerCertToConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.concurrent.CompletableAsyncResult;
import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.api.cmd.Orion;
import net.consensys.orion.api.config.Config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CompletionException;
import javax.net.ssl.SSLHandshakeException;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.net.SelfSignedCertificate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class WhiteListSecurityTest {

  private static Vertx vertx = vertx();
  private static HttpClient httpClient;
  private static HttpClient httpClientWithUnregisteredCert;
  private static HttpClient httpClientWithImproperCertificate;
  private static HttpClient anotherExampleComClient;
  private static int nodePort;
  private static Orion orion;

  @BeforeAll
  static void setUp(@TempDirectory Path tempDir) throws Exception {
    SelfSignedCertificate serverCertificate = SelfSignedCertificate.create("localhost");
    Config config = generateAndLoadConfiguration(tempDir, writer -> {
      writer.write("tlsservertrust='whitelist'\n");
      writeServerCertToConfig(writer, serverCertificate);
    });

    configureJDKTrustStore(serverCertificate, tempDir);
    Path knownClientsFile = config.tlsKnownClients();

    SelfSignedCertificate clientCertificate = SelfSignedCertificate.create("example.com");
    String fingerprint = certificateHexFingerprint(Paths.get(clientCertificate.keyCertOptions().getCertPath()));
    Files.write(knownClientsFile, Arrays.asList("#First line", "example.com " + fingerprint));
    httpClient = vertx
        .createHttpClient(new HttpClientOptions().setSsl(true).setKeyCertOptions(clientCertificate.keyCertOptions()));

    SelfSignedCertificate fooCertificate = SelfSignedCertificate.create("foo.bar.baz");
    httpClientWithUnregisteredCert =
        vertx.createHttpClient(new HttpClientOptions().setSsl(true).setKeyCertOptions(fooCertificate.keyCertOptions()));

    SelfSignedCertificate noCNCert = SelfSignedCertificate.create("");
    httpClientWithImproperCertificate =
        vertx.createHttpClient(new HttpClientOptions().setSsl(true).setKeyCertOptions(noCNCert.keyCertOptions()));

    SelfSignedCertificate anotherExampleDotComCert = SelfSignedCertificate.create("example.com");
    anotherExampleComClient = vertx.createHttpClient(
        new HttpClientOptions().setSsl(true).setKeyCertOptions(anotherExampleDotComCert.keyCertOptions()));

    nodePort = config.nodePort();
    orion = new Orion(vertx);
    orion.run(System.out, System.err, config);
  }

  @AfterAll
  static void tearDown() {
    orion.stop();
    vertx.close();
  }

  @Test
  void testUpCheckOnNodePort() throws Exception {
    for (int i = 0; i < 5; i++) {
      HttpClientRequest req = httpClient.get(nodePort, "localhost", "/upcheck");
      CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
      req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();
      HttpClientResponse resp = result.get();
      assertEquals(200, resp.statusCode());
    }
  }

  @Test
  void testSameHostnameUnknownCertificate() {
    HttpClientRequest req = anotherExampleComClient.get(nodePort, "localhost", "/upcheck");
    CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
    req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();
    CompletionException e = assertThrows(CompletionException.class, result::get);
    assertEquals("Received fatal alert: certificate_unknown", e.getCause().getCause().getMessage());
  }

  @Test
  void testUpCheckOnNodePortWithUnregisteredClientCert() {
    HttpClientRequest req = httpClientWithUnregisteredCert.get(nodePort, "localhost", "/upcheck");
    CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
    req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();
    CompletionException e = assertThrows(CompletionException.class, result::get);
    assertEquals("Received fatal alert: certificate_unknown", e.getCause().getCause().getMessage());
  }

  @Test
  void testUpCheckOnNodePortWithInvalidClientCert() {
    HttpClientRequest req = httpClientWithImproperCertificate.get(nodePort, "localhost", "/upcheck");
    CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
    req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();
    CompletionException e = assertThrows(CompletionException.class, result::get);
    assertEquals("Received fatal alert: certificate_unknown", e.getCause().getCause().getMessage());
  }

  @Test
  void testWithoutSSLConfiguration() {
    CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
    HttpClient insecureClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true));
    HttpClientRequest req = insecureClient.get(nodePort, "localhost", "/upcheck");
    req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();

    CompletionException e = assertThrows(CompletionException.class, result::get);
    assertTrue(e.getCause() instanceof SSLHandshakeException);
  }
}

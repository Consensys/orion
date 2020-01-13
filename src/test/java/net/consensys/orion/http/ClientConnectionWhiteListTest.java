/*
 * Copyright 2020 ConsenSys AG.
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.orion.cmd.Orion;
import net.consensys.orion.config.Config;

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
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.SelfSignedCertificate;
import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.concurrent.CompletableAsyncResult;
import org.apache.tuweni.junit.TempDirectory;
import org.apache.tuweni.junit.TempDirectoryExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class ClientConnectionWhiteListTest {

  private final static Vertx vertx = vertx();
  private static HttpClient httpClient;
  private static HttpClient httpClientWithUnregisteredCert;
  private static HttpClient httpClientWithImproperCertificate;
  private static HttpClient httpClientWithMismatchedTrust;
  private static int clientPort;
  private static Orion orion;

  @BeforeAll
  static void setUp(@TempDirectory final Path tempDir) throws Exception {
    final SelfSignedCertificate serverCertificate = SelfSignedCertificate.create("localhost");
    final Config config = generateAndLoadConfiguration(tempDir, writer -> {
      writer.write("clientconnectiontls='strict'\n");
      writer.write("clientconnectiontlsservertrust='whitelist'\n");
      writeServerCertToConfig(writer, serverCertificate);
      writeClientConnectionServerCertToConfig(writer, serverCertificate);
    });

    configureJDKTrustStore(serverCertificate, tempDir);
    final Path knownClientsFile = config.clientConnectionTlsKnownClients();

    final SelfSignedCertificate clientCertificate = SelfSignedCertificate.create("example.com");
    final String fingerprint = certificateHexFingerprint(Paths.get(clientCertificate.keyCertOptions().getCertPath()));
    Files.write(knownClientsFile, Arrays.asList("#First line", "example.com " + fingerprint));
    httpClient = vertx
        .createHttpClient(new HttpClientOptions().setSsl(true).setKeyCertOptions(clientCertificate.keyCertOptions()));

    final SelfSignedCertificate fooCertificate = SelfSignedCertificate.create("foo.bar.baz");
    httpClientWithUnregisteredCert =
        vertx.createHttpClient(new HttpClientOptions().setSsl(true).setKeyCertOptions(fooCertificate.keyCertOptions()));

    final SelfSignedCertificate noCNCert = SelfSignedCertificate.create("");
    httpClientWithImproperCertificate =
        vertx.createHttpClient(new HttpClientOptions().setSsl(true).setKeyCertOptions(noCNCert.keyCertOptions()));

    httpClientWithMismatchedTrust = vertx.createHttpClient(
        new HttpClientOptions().setSsl(true).setKeyCertOptions(clientCertificate.keyCertOptions()).setTrustOptions(
            new PemTrustOptions().addCertPath(clientCertificate.certificatePath())));

    clientPort = config.clientPort();
    orion = new Orion(vertx);
    orion.run(System.out, System.err, config);
  }

  @AfterAll
  static void tearDown() {
    orion.stop();
    vertx.close();
  }

  @Test
  void clientInWhiteListCanInvokeUpcheck() throws Exception {
    final HttpClientRequest req = httpClient.get(clientPort, "localhost", "/upcheck");
    final CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
    req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();
    final HttpClientResponse resp = result.get();
    assertEquals(200, resp.statusCode());
  }

  @Test
  void clientNotInWhiteListCannotInvokeUpcheck() {
    final HttpClientRequest req = httpClientWithUnregisteredCert.get(clientPort, "localhost", "/upcheck");
    final CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
    req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();
    
    final Throwable thrown = catchThrowable(result::get);

    assertThat(thrown).isInstanceOf(CompletionException.class);
    assertThat(thrown.getCause()).isInstanceOf(SSLHandshakeException.class);
  }

  @Test
  void clientWithImproperCertificateCannnotInvokeUpcheck() {
    final HttpClientRequest req = httpClientWithImproperCertificate.get(clientPort, "localhost", "/upcheck");
    final CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
    req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();

    final Throwable thrown = catchThrowable(result::get);

    assertThat(thrown).isInstanceOf(CompletionException.class);
    assertThat(thrown.getCause()).isInstanceOf(SSLHandshakeException.class);
  }

  @Test
  void clientWithIncorrectTrustCannotInvokeUpcheck() {
    final HttpClientRequest req = httpClientWithMismatchedTrust.get(clientPort, "localhost", "/upcheck");
    final CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
    req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();
    final Throwable thrown = catchThrowable(result::get);

    assertThat(thrown).isInstanceOf(CompletionException.class);
    assertThat(thrown.getCause()).isInstanceOf(SSLHandshakeException.class);
  }
}

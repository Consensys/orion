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
import static net.consensys.orion.TestUtils.generateAndLoadConfiguration;
import static net.consensys.orion.TestUtils.writeClientConnectionServerCertToConfig;
import static net.consensys.orion.TestUtils.writeServerCertToConfig;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.consensys.orion.TestUtils;
import net.consensys.orion.cmd.Orion;
import net.consensys.orion.config.Config;

import java.nio.file.Path;
import javax.net.ssl.SSLException;

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
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class CertificateAuthoritySecurityTest {

  private final static Vertx vertx = vertx();
  private static HttpClient httpClient;
  private static Orion orion;
  private static Config config;
  private final static String TRUST_MODE = "ca";

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

    final SelfSignedCertificate clientCert = SelfSignedCertificate.create("example.com");
    TestUtils.configureJDKTrustStore(clientCert, tempDir);
    httpClient = vertx.createHttpClient(
        new HttpClientOptions().setSsl(true).setTrustAll(true).setKeyCertOptions(clientCert.keyCertOptions()));

    orion = new Orion(vertx);
    orion.run(System.out, System.err, config);
  }

  @AfterAll
  static void tearDown() {
    System.clearProperty("javax.net.ssl.trustStore");
    System.clearProperty("javax.net.ssl.trustStorePassword");
    orion.stop();
    vertx.close();
  }

  @Test
  void testUpCheckOnServerPortsIsSuccessful() throws Exception {
    Assertions.assertThat(upcheckOnPortUsingSslEnabledClient(config.nodePort()).statusCode()).isEqualTo(200);
    Assertions.assertThat(upcheckOnPortUsingSslEnabledClient(config.clientPort()).statusCode()).isEqualTo(200);
  }

  private HttpClientResponse upcheckOnPortUsingSslEnabledClient(final int port) throws Exception {
    final HttpClientRequest req = httpClient.get(port, "localhost", "/upcheck");
    final CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
    req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();
    return result.get();

  }

  @Test
  void testWithoutSSLConfiguration() {
    final OkHttpClient unsecureHttpClient = new OkHttpClient.Builder().build();

    final Request nodeUpcheckRequest =
        new Request.Builder().url("https://localhost:" + config.nodePort() + "/upcheck").build();
    assertThrows(SSLException.class, () -> unsecureHttpClient.newCall(nodeUpcheckRequest).execute());

    final Request clientUpcheckRequest =
        new Request.Builder().url("https://localhost:" + config.clientPort() + "/upcheck").build();
    assertThrows(SSLException.class, () -> unsecureHttpClient.newCall(clientUpcheckRequest).execute());
  }
}

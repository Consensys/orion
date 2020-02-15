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
package net.consensys.orion.network;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.consensys.orion.TestUtils.generateAndLoadConfiguration;
import static net.consensys.orion.TestUtils.getFreePort;
import static net.consensys.orion.TestUtils.writeClientCertToConfig;
import static org.apache.tuweni.net.tls.TLS.certificateHexFingerprint;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.orion.config.Config;
import net.consensys.orion.http.server.HttpContentType;
import net.consensys.orion.utils.Serializer;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionException;
import javax.net.ssl.SSLException;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.ext.web.Router;
import org.apache.tuweni.concurrent.AsyncCompletion;
import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.concurrent.CompletableAsyncCompletion;
import org.apache.tuweni.concurrent.CompletableAsyncResult;
import org.apache.tuweni.junit.TempDirectory;
import org.apache.tuweni.junit.TempDirectoryExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class TofuNodeClientTest {

  private final static Vertx vertx = Vertx.vertx();
  private static Config config;
  private static Path knownServersFile;

  private HttpServer tofuServer;
  private String fooFingerprint;
  private HttpClient client;

  @BeforeAll
  static void setUpConfig(@TempDirectory final Path tempDir) throws Exception {
    final SelfSignedCertificate clientCert = SelfSignedCertificate.create("localhost");
    config = generateAndLoadConfiguration(tempDir, writer -> {
      writer.write("tlsclienttrust='tofu'\n");
      writeClientCertToConfig(writer, clientCert);
    });
    knownServersFile = config.tlsKnownServers();
  }

  @BeforeEach
  void setUp() throws Exception {
    final SelfSignedCertificate serverCert = SelfSignedCertificate.create("foo.com");
    fooFingerprint = certificateHexFingerprint(Paths.get(serverCert.keyCertOptions().getCertPath()));
    Files.write(knownServersFile, Collections.singletonList("#First line"));

    final Router dummyRouter = Router.router(vertx);
    final ReadOnlyNetworkNodes payload =
        new ReadOnlyNetworkNodes(URI.create("http://www.example.com"), Collections.emptyList(), Collections.emptyMap());
    dummyRouter.post("/partyinfo").handler(routingContext -> {
      routingContext.response().end(Buffer.buffer(Serializer.serialize(HttpContentType.CBOR, payload)));
    });
    client = NodeHttpClientBuilder.build(vertx, config, 100);
    tofuServer = vertx
        .createHttpServer(new HttpServerOptions().setSsl(true).setPemKeyCertOptions(serverCert.keyCertOptions()))
        .requestHandler(dummyRouter::accept);
    startServer(tofuServer);
  }

  private static void startServer(final HttpServer server) throws Exception {
    final CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    server.listen(getFreePort(), result -> {
      if (result.succeeded()) {
        completion.complete();
      } else {
        completion.completeExceptionally(result.cause());
      }
    });
    completion.join();
  }

  @Test
  void testTOFU() throws Exception {
    final CompletableAsyncResult<Integer> statusCode = AsyncResult.incomplete();
    client
        .post(
            tofuServer.actualPort(),
            "localhost",
            "/partyinfo",
            response -> statusCode.complete(response.statusCode()))
        .end();
    assertEquals((Integer) 200, statusCode.get());

    final List<String> fingerprints = Files.readAllLines(knownServersFile);
    assertEquals(2, fingerprints.size(), String.join("\n", fingerprints));
    assertEquals("#First line", fingerprints.get(0));
    assertEquals("localhost:" + tofuServer.actualPort() + " " + fooFingerprint, fingerprints.get(1));
  }

  @Test
  void testServerWithIncorrectFingerprint() throws Exception {
    Files.write(
        knownServersFile,
        ("localhost:" + tofuServer.actualPort() + " " + new StringBuilder(fooFingerprint).reverse().toString())
            .getBytes(UTF_8));
    final HttpClient newClient = NodeHttpClientBuilder.build(vertx, config, 100);

    final CompletableAsyncResult<Integer> statusCode = AsyncResult.incomplete();
    newClient
        .post(
            tofuServer.actualPort(),
            "localhost",
            "/partyinfo",
            response -> statusCode.complete(response.statusCode()))
        .exceptionHandler(statusCode::completeExceptionally)
        .end();
    final CompletionException e = assertThrows(CompletionException.class, statusCode::get);
    assertTrue(e.getCause() instanceof SSLException);
  }

  @AfterAll
  static void tearDown() {
    vertx.close();
  }
}

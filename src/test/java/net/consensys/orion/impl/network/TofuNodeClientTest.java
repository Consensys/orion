package net.consensys.orion.impl.network;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.consensys.cava.net.tls.TLS.certificateHexFingerprint;
import static net.consensys.orion.impl.TestUtils.generateAndLoadConfiguration;
import static net.consensys.orion.impl.TestUtils.getFreePort;
import static net.consensys.orion.impl.TestUtils.writeClientCertToConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.concurrent.CompletableAsyncCompletion;
import net.consensys.cava.concurrent.CompletableAsyncResult;
import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.api.config.Config;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Serializer;

import java.net.URL;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class TofuNodeClientTest {

  private static Vertx vertx = Vertx.vertx();
  private static Config config;
  private static Path knownServersFile;

  private HttpServer tofuServer;
  private String fooFingerprint;
  private HttpClient client;

  @BeforeAll
  static void setUpConfig(@TempDirectory Path tempDir) throws Exception {
    SelfSignedCertificate clientCert = SelfSignedCertificate.create("localhost");
    config = generateAndLoadConfiguration(tempDir, writer -> {
      writer.write("tlsclienttrust='tofu'\n");
      writeClientCertToConfig(writer, clientCert);
    });
    knownServersFile = config.tlsKnownServers();
  }

  @BeforeEach
  void setUp() throws Exception {
    SelfSignedCertificate serverCert = SelfSignedCertificate.create("foo.com");
    fooFingerprint = certificateHexFingerprint(Paths.get(serverCert.keyCertOptions().getCertPath()));
    Files.write(knownServersFile, Collections.singletonList("#First line"));

    Router dummyRouter = Router.router(vertx);
    ConcurrentNetworkNodes payload = new ConcurrentNetworkNodes(new URL("http://www.example.com"));
    dummyRouter.post("/partyinfo").handler(routingContext -> {
      routingContext.response().end(Buffer.buffer(Serializer.serialize(HttpContentType.CBOR, payload)));
    });
    client = NodeHttpClientBuilder.build(vertx, config, 100);
    tofuServer = vertx
        .createHttpServer(new HttpServerOptions().setSsl(true).setPemKeyCertOptions(serverCert.keyCertOptions()))
        .requestHandler(dummyRouter::accept);
    startServer(tofuServer);
  }

  private static void startServer(HttpServer server) throws Exception {
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
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
    CompletableAsyncResult<Integer> statusCode = AsyncResult.incomplete();
    client
        .post(
            tofuServer.actualPort(),
            "localhost",
            "/partyinfo",
            response -> statusCode.complete(response.statusCode()))
        .end();
    assertEquals((Integer) 200, statusCode.get());

    List<String> fingerprints = Files.readAllLines(knownServersFile);
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
    HttpClient newClient = NodeHttpClientBuilder.build(vertx, config, 100);

    CompletableAsyncResult<Integer> statusCode = AsyncResult.incomplete();
    newClient
        .post(
            tofuServer.actualPort(),
            "localhost",
            "/partyinfo",
            response -> statusCode.complete(response.statusCode()))
        .exceptionHandler(statusCode::completeExceptionally)
        .end();
    CompletionException e = assertThrows(CompletionException.class, statusCode::get);
    assertTrue(e.getCause() instanceof SSLException);
  }

  @AfterAll
  static void tearDown() {
    vertx.close();
  }
}

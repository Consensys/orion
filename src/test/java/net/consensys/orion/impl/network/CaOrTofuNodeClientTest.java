package net.consensys.orion.impl.network;

import static net.consensys.cava.net.tls.TLS.certificateHexFingerprint;
import static net.consensys.orion.impl.TestUtils.generateAndLoadConfiguration;
import static net.consensys.orion.impl.TestUtils.getFreePort;
import static net.consensys.orion.impl.TestUtils.writeClientCertToConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.concurrent.CompletableAsyncCompletion;
import net.consensys.cava.concurrent.CompletableAsyncResult;
import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.api.config.Config;
import net.consensys.orion.impl.TestUtils;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Serializer;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.ext.web.Router;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class CaOrTofuNodeClientTest {

  private static Vertx vertx = Vertx.vertx();
  private static HttpServer caValidServer;
  private static HttpServer tofuServer;
  private static String fooFingerprint;
  private static Path knownServersFile;
  private static HttpClient client;

  @BeforeAll
  static void setUp(@TempDirectory Path tempDir) throws Exception {
    SelfSignedCertificate clientCert = SelfSignedCertificate.create("localhost");
    Config config = generateAndLoadConfiguration(tempDir, writer -> {
      writer.write("tlsclienttrust='ca-or-tofu'\n");
      writeClientCertToConfig(writer, clientCert);
    });

    knownServersFile = config.tlsKnownServers();

    SelfSignedCertificate serverCert = SelfSignedCertificate.create("foo.com");
    SelfSignedCertificate tofuCert = SelfSignedCertificate.create();
    TestUtils.configureJDKTrustStore(serverCert, tempDir);
    fooFingerprint = certificateHexFingerprint(Paths.get(tofuCert.keyCertOptions().getCertPath()));
    Files.write(knownServersFile, Collections.singletonList("#First line"));

    Router dummyRouter = Router.router(vertx);
    ConcurrentNetworkNodes payload = new ConcurrentNetworkNodes(new URL("http://www.example.com"));
    dummyRouter.post("/partyinfo").handler(routingContext -> {
      routingContext.response().end(Buffer.buffer(Serializer.serialize(HttpContentType.CBOR, payload)));
    });

    caValidServer = vertx
        .createHttpServer(new HttpServerOptions().setSsl(true).setPemKeyCertOptions(serverCert.keyCertOptions()))
        .requestHandler(dummyRouter::accept);
    startServer(caValidServer);
    tofuServer = vertx
        .createHttpServer(new HttpServerOptions().setSsl(true).setPemKeyCertOptions(tofuCert.keyCertOptions()))
        .requestHandler(dummyRouter::accept);
    startServer(tofuServer);

    client = NodeHttpClientBuilder.build(vertx, config, 100);
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
  void testValidCertificateServer() throws Exception {
    CompletableAsyncResult<Integer> statusCode = AsyncResult.incomplete();
    client
        .post(
            caValidServer.actualPort(),
            "localhost",
            "/partyinfo",
            response -> statusCode.complete(response.statusCode()))
        .end();
    assertEquals((Integer) 200, statusCode.get());
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

  @AfterAll
  static void tearDown() {
    vertx.close();
  }
}

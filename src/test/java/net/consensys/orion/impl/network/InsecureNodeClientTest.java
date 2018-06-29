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
class InsecureNodeClientTest {

  private static Vertx vertx = Vertx.vertx();
  private static HttpServer insecureServer;
  private static HttpServer foobarComServer;
  private static Path knownServersFile;
  private static String fooFingerprint;
  private static HttpClient client;

  @BeforeAll
  static void setUp(@TempDirectory Path tempDir) throws Exception {
    SelfSignedCertificate clientCert = SelfSignedCertificate.create("localhost");
    Config config = generateAndLoadConfiguration(tempDir, writer -> {
      writer.write("tlsclienttrust='insecure-no-validation'\n");
      writeClientCertToConfig(writer, clientCert);
    });

    knownServersFile = config.tlsKnownServers();

    SelfSignedCertificate serverCert = SelfSignedCertificate.create("foo.com");
    fooFingerprint = certificateHexFingerprint(Paths.get(serverCert.keyCertOptions().getCertPath()));
    Files.write(knownServersFile, Collections.singletonList("#First line"));

    client = NodeHttpClientBuilder.build(vertx, config, 100);

    Router dummyRouter = Router.router(vertx);
    ConcurrentNetworkNodes payload = new ConcurrentNetworkNodes(new URL("http://www.example.com"));
    dummyRouter.post("/partyinfo").handler(routingContext -> {
      routingContext.response().end(Buffer.buffer(Serializer.serialize(HttpContentType.CBOR, payload)));
    });

    insecureServer = vertx
        .createHttpServer(new HttpServerOptions().setSsl(true).setPemKeyCertOptions(serverCert.keyCertOptions()))
        .requestHandler(dummyRouter::accept);
    startServer(insecureServer);
    foobarComServer = vertx
        .createHttpServer(
            new HttpServerOptions().setSsl(true).setPemKeyCertOptions(
                SelfSignedCertificate.create("foobar.com").keyCertOptions()))
        .requestHandler(dummyRouter::accept);
    startServer(foobarComServer);
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
  void testInsecure() throws Exception {
    CompletableAsyncResult<Integer> statusCode = AsyncResult.incomplete();
    client
        .post(
            insecureServer.actualPort(),
            "localhost",
            "/partyinfo",
            response -> statusCode.complete(response.statusCode()))
        .end();
    assertEquals((Integer) 200, statusCode.get());

    List<String> fingerprints = Files.readAllLines(knownServersFile);
    assertEquals(2, fingerprints.size(), String.join("\n", fingerprints));
    assertEquals("#First line", fingerprints.get(0));
    assertEquals("localhost:" + insecureServer.actualPort() + " " + fooFingerprint, fingerprints.get(1));

    CompletableAsyncResult<Integer> secondStatusCode = AsyncResult.incomplete();
    client
        .post(
            foobarComServer.actualPort(),
            "localhost",
            "/partyinfo",
            response -> secondStatusCode.complete(response.statusCode()))
        .end();
    assertEquals((Integer) 200, secondStatusCode.get());

    fingerprints = Files.readAllLines(knownServersFile);
    assertEquals(3, fingerprints.size(), String.join("\n", fingerprints));
  }

  @AfterAll
  static void tearDown() {
    vertx.close();
  }
}

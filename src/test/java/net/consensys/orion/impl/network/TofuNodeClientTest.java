package net.consensys.orion.impl.network;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.consensys.cava.crypto.Hash.sha2_256;
import static org.junit.Assert.assertEquals;

import net.consensys.orion.impl.config.MemoryConfig;
import net.consensys.orion.impl.http.handlers.SecurityTestUtils;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Serializer;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import javax.net.ssl.SSLException;

import io.netty.util.internal.StringUtil;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.ext.web.Router;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TofuNodeClientTest {

  private MemoryConfig config;

  private Vertx vertx;

  private HttpServer tofuServer;
  private Path knownServersFile;
  private String fooFingerprint;
  private HttpClient client;

  @Before
  public void setUp() throws Exception {
    vertx = Vertx.vertx();
    config = new MemoryConfig();
    config.setWorkDir(Files.createTempDirectory("data"));
    config.setTls("strict");
    config.setTlsClientTrust("tofu");
    SelfSignedCertificate clientCert = SelfSignedCertificate.create();
    config.setTlsClientCert(Paths.get(clientCert.certificatePath()));
    config.setTlsClientKey(Paths.get(clientCert.privateKeyPath()));

    SelfSignedCertificate serverCert = SelfSignedCertificate.create("foo.com");
    knownServersFile = Files.createTempFile("knownservers", ".txt");
    config.setTlsKnownServers(knownServersFile);
    fooFingerprint = StringUtil
        .toHexStringPadded(sha2_256(SecurityTestUtils.loadPEM(Paths.get(serverCert.keyCertOptions().getCertPath()))));
    Files.write(knownServersFile, Arrays.asList("#First line"));

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
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    server.listen(SecurityTestUtils.getFreePort(), result -> {
      if (result.succeeded()) {
        future.complete(true);
      } else {
        future.completeExceptionally(result.cause());
      }
      future.join();
    });
  }

  @Test
  public void testTOFU() throws Exception {
    CompletableFuture<Integer> statusCode = new CompletableFuture<>();
    client
        .post(
            tofuServer.actualPort(),
            "localhost",
            "/partyinfo",
            response -> statusCode.complete(response.statusCode()))
        .end();
    assertEquals((Integer) 200, statusCode.join());

    List<String> fingerprints = Files.readAllLines(knownServersFile);
    assertEquals(String.join("\n", fingerprints), 2, fingerprints.size());
    assertEquals("#First line", fingerprints.get(0));
    assertEquals("localhost:" + tofuServer.actualPort() + " " + fooFingerprint, fingerprints.get(1));
  }

  @Test(expected = SSLException.class)
  public void testServerWithIncorrectFingerprint() throws Throwable {
    Files.write(
        knownServersFile,
        ("localhost:" + tofuServer.actualPort() + " " + new StringBuilder(fooFingerprint).reverse().toString())
            .getBytes(UTF_8));
    HttpClient newClient = NodeHttpClientBuilder.build(vertx, config, 100);

    CompletableFuture<Integer> statusCode = new CompletableFuture<>();
    newClient
        .post(
            tofuServer.actualPort(),
            "localhost",
            "/partyinfo",
            response -> statusCode.complete(response.statusCode()))
        .exceptionHandler(statusCode::completeExceptionally)
        .end();
    try {
      statusCode.join();
      List<String> fingerprints = Files.readAllLines(knownServersFile);
    } catch (CompletionException e) {
      throw e.getCause();
    }
  }

  @After
  public void tearDown() throws Exception {
    vertx.close();
  }
}

package net.consensys.orion.impl.network;

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

import com.google.common.hash.Hashing;
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

public class InsecureNodeClientTest {

  private MemoryConfig config;

  private Vertx vertx;

  private HttpServer insecureServer;
  private HttpServer foobarComServer;
  private Path knownServersFile;
  private String fooFingerprint;
  private HttpClient client;

  @Before
  public void setUp() throws Exception {
    vertx = Vertx.vertx();
    config = new MemoryConfig();
    config.setWorkDir(Files.createTempDirectory("data"));
    config.setTls("strict");
    config.setTlsClientTrust("insecure-no-validation");
    SelfSignedCertificate clientCert = SelfSignedCertificate.create();
    config.setTlsClientCert(Paths.get(clientCert.certificatePath()));
    config.setTlsClientKey(Paths.get(clientCert.privateKeyPath()));

    SelfSignedCertificate serverCert = SelfSignedCertificate.create("foo.com");
    knownServersFile = Files.createTempFile("knownservers", ".txt");
    config.setTlsKnownServers(knownServersFile);
    fooFingerprint = StringUtil.toHexStringPadded(
        Hashing
            .sha1()
            .hashBytes(SecurityTestUtils.loadPEM(Paths.get(serverCert.keyCertOptions().getCertPath())))
            .asBytes());
    Files.write(knownServersFile, Arrays.asList("#First line"));

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
  public void testInsecure() throws Exception {
    CompletableFuture<Integer> statusCode = new CompletableFuture<>();
    client
        .post(
            insecureServer.actualPort(),
            "localhost",
            "/partyinfo",
            response -> statusCode.complete(response.statusCode()))
        .end();
    assertEquals((Integer) 200, statusCode.join());

    List<String> fingerprints = Files.readAllLines(knownServersFile);
    assertEquals(String.join("\n", fingerprints), 2, fingerprints.size());
    assertEquals("#First line", fingerprints.get(0));
    assertEquals("foo.com " + fooFingerprint, fingerprints.get(1));
    CompletableFuture<Integer> secondStatusCode = new CompletableFuture<>();
    client
        .post(
            foobarComServer.actualPort(),
            "localhost",
            "/partyinfo",
            response -> secondStatusCode.complete(response.statusCode()))
        .end();
    assertEquals((Integer) 200, secondStatusCode.join());

    fingerprints = Files.readAllLines(knownServersFile);
    assertEquals(String.join("\n", fingerprints), 3, fingerprints.size());
  }

  @After
  public void tearDown() throws Exception {
    vertx.close();
  }
}

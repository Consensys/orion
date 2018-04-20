package net.consensys.orion.impl.network;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import net.consensys.orion.impl.config.MemoryConfig;
import net.consensys.orion.impl.enclave.sodium.SodiumPublicKey;
import net.consensys.orion.impl.http.handlers.CertificateAuthoritySecurityTest;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Serializer;

import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import com.google.common.hash.Hashing;
import io.netty.util.internal.StringUtil;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.ext.web.Router;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class WhitelistNetworkDiscoveryTest {

  private static MemoryConfig config;

  private static Vertx vertx = Vertx.vertx();

  private static HttpServer whitelistedServer;

  private static HttpServer unknownServer;

  public static int getFreePort() throws Exception {
    try (ServerSocket nodeSocket = new ServerSocket(0);) {
      return nodeSocket.getLocalPort();
    }
  }

  @BeforeClass
  public static void setUp() throws Exception {
    config = new MemoryConfig();
    config.setWorkDir(Files.createTempDirectory("data"));
    config.setTls("strict");
    config.setTlsClientTrust("whitelist");
    SelfSignedCertificate clientCert = SelfSignedCertificate.create();
    config.setTlsClientCert(Paths.get(clientCert.certificatePath()));
    config.setTlsClientKey(Paths.get(clientCert.privateKeyPath()));

    SelfSignedCertificate serverCert = SelfSignedCertificate.create("foo.com");
    Path knownServersFile = Files.createTempFile("knownservers", ".txt");
    config.setTlsKnownServers(knownServersFile);
    String fingerprint = StringUtil.toHexStringPadded(
        Hashing
            .sha1()
            .hashBytes(CertificateAuthoritySecurityTest.loadPEM(Paths.get(serverCert.keyCertOptions().getCertPath())))
            .asBytes());
    Files.write(knownServersFile, Arrays.asList("#First line", "foo.com " + fingerprint));

    Router dummyRouter = Router.router(vertx);
    ConcurrentNetworkNodes payload = new ConcurrentNetworkNodes(new URL("http://www.example.com"));
    dummyRouter.post("/partyinfo").handler(routingContext -> {
      routingContext.response().end(Buffer.buffer(Serializer.serialize(HttpContentType.CBOR, payload)));
    });

    whitelistedServer = vertx
        .createHttpServer(new HttpServerOptions().setSsl(true).setPemKeyCertOptions(serverCert.keyCertOptions()))
        .requestHandler(dummyRouter::accept);
    startServer(whitelistedServer);
    unknownServer = vertx
        .createHttpServer(
            new HttpServerOptions().setSsl(true).setPemKeyCertOptions(SelfSignedCertificate.create().keyCertOptions()))
        .requestHandler(dummyRouter::accept);
    startServer(unknownServer);
  }

  private static void startServer(HttpServer server) throws Exception {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    server.listen(getFreePort(), result -> {
      if (result.succeeded()) {
        future.complete(true);
      } else {
        future.completeExceptionally(result.cause());
      }
      future.join();
    });
  }

  @Test
  public void testWhitelistedServer() throws Exception {
    ConcurrentNetworkNodes networkNodes =
        new ConcurrentNetworkNodes(new MemoryConfig(), new PublicKey[] {new SodiumPublicKey("pk1".getBytes(UTF_8))});
    URL url = new URL("https://localhost:" + whitelistedServer.actualPort());
    networkNodes.addNode(new SodiumPublicKey("pk2".getBytes(UTF_8)), url);
    NetworkDiscovery networkDiscovery = new NetworkDiscovery(networkNodes, config, 100, 200);
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    vertx.deployVerticle(networkDiscovery, result -> future.complete(true));
    future.join();
    Thread.sleep(500);
    assertNotEquals(Instant.MIN, networkDiscovery.discoverers().get(url.toString()).lastUpdate);
  }

  @Test
  public void testUnknownServer() throws Exception {
    ConcurrentNetworkNodes networkNodes =
        new ConcurrentNetworkNodes(new MemoryConfig(), new PublicKey[] {new SodiumPublicKey("pk1".getBytes(UTF_8))});
    URL url = new URL("https://localhost:" + unknownServer.actualPort());
    networkNodes.addNode(new SodiumPublicKey("pk2".getBytes(UTF_8)), url);
    NetworkDiscovery networkDiscovery = new NetworkDiscovery(networkNodes, config, 100, 200);
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    vertx.deployVerticle(networkDiscovery, result -> future.complete(true));
    future.join();
    Thread.sleep(1000);
    assertEquals(Instant.MIN, networkDiscovery.discoverers().get(url.toString()).lastUpdate);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    vertx.close();
  }
}

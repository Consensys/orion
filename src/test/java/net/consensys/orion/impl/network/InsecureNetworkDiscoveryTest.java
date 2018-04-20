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
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.common.hash.Hashing;
import io.netty.util.internal.StringUtil;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.ext.web.Router;
import org.apache.logging.log4j.util.Strings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class InsecureNetworkDiscoveryTest {

  private MemoryConfig config;

  private Vertx vertx;

  private HttpServer insecureServer;

  private static HttpServer foobarComServer;
  private static Path knownServersFile;
  private static String fooFingerprint;
  private ConcurrentNetworkNodes networkNodes;
  private URL tofuServerURL;
  private NetworkDiscovery networkDiscovery;

  public static int getFreePort() throws Exception {
    try (ServerSocket nodeSocket = new ServerSocket(0);) {
      return nodeSocket.getLocalPort();
    }
  }

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
            .hashBytes(CertificateAuthoritySecurityTest.loadPEM(Paths.get(serverCert.keyCertOptions().getCertPath())))
            .asBytes());
    Files.write(knownServersFile, Arrays.asList("#First line"));

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

    networkNodes =
        new ConcurrentNetworkNodes(new MemoryConfig(), new PublicKey[] {new SodiumPublicKey("pk1".getBytes(UTF_8))});
    tofuServerURL = new URL("https://localhost:" + insecureServer.actualPort());
    networkNodes.addNode(new SodiumPublicKey("pk2".getBytes(UTF_8)), tofuServerURL);
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
  public void testInsecure() throws Exception {
    networkDiscovery = new NetworkDiscovery(networkNodes, config, 100, 1000);
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    vertx.deployVerticle(networkDiscovery, result -> future.complete(true));
    future.join();
    Thread.sleep(1500);
    assertNotEquals(Instant.MIN, networkDiscovery.discoverers().get(tofuServerURL.toString()).lastUpdate);
    List<String> fingerprints = Files.readAllLines(knownServersFile);
    assertEquals(Strings.join(fingerprints, '\n'), 2, fingerprints.size());
    assertEquals("#First line", fingerprints.get(0));
    assertEquals("foo.com " + fooFingerprint, fingerprints.get(1));

    URL unknownServerURL = new URL("https://localhost:" + foobarComServer.actualPort());
    networkNodes.addNode(new SodiumPublicKey("pk3".getBytes(UTF_8)), unknownServerURL);
    Thread.sleep(2000);
    assertNotEquals(Instant.MIN, networkDiscovery.discoverers().get(unknownServerURL.toString()).lastUpdate);

    fingerprints = Files.readAllLines(knownServersFile);
    assertEquals(Strings.join(fingerprints, '\n'), 3, fingerprints.size());
  }

  @After
  public void tearDown() throws Exception {
    vertx.close();
  }
}

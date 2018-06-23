package net.consensys.orion.impl.network;

import static net.consensys.cava.crypto.Hash.sha2_256;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.concurrent.CompletableAsyncCompletion;
import net.consensys.cava.concurrent.CompletableAsyncResult;
import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.impl.config.MemoryConfig;
import net.consensys.orion.impl.http.SecurityTestUtils;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Serializer;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class WhitelistNodeClientTest {

  private static Vertx vertx = Vertx.vertx();
  private static HttpServer whitelistedServer;
  private static HttpServer unknownServer;
  private static HttpClient client;

  @BeforeAll
  static void setUp(@TempDirectory Path tempDir) throws Exception {
    MemoryConfig config = new MemoryConfig();
    config.setWorkDir(tempDir);
    config.setTls("strict");
    config.setTlsClientTrust("whitelist");
    SelfSignedCertificate clientCert = SelfSignedCertificate.create();
    config.setTlsClientCert(Paths.get(clientCert.certificatePath()));
    config.setTlsClientKey(Paths.get(clientCert.privateKeyPath()));

    SelfSignedCertificate serverCert = SelfSignedCertificate.create("localhost");
    Path knownServersFile = tempDir.resolve("knownservers.txt");
    config.setTlsKnownServers(knownServersFile);
    Router dummyRouter = Router.router(vertx);
    whitelistedServer = vertx
        .createHttpServer(new HttpServerOptions().setSsl(true).setPemKeyCertOptions(serverCert.keyCertOptions()))
        .requestHandler(dummyRouter::accept);
    startServer(whitelistedServer);
    String fingerprint = StringUtil
        .toHexStringPadded(sha2_256(SecurityTestUtils.loadPEM(Paths.get(serverCert.keyCertOptions().getCertPath()))));
    Files.write(
        knownServersFile,
        Arrays.asList("#First line", "localhost:" + whitelistedServer.actualPort() + " " + fingerprint));

    client = NodeHttpClientBuilder.build(vertx, config, 100);

    ConcurrentNetworkNodes payload = new ConcurrentNetworkNodes(new URL("http://www.example.com"));
    dummyRouter.post("/partyinfo").handler(routingContext -> {
      routingContext.response().end(Buffer.buffer(Serializer.serialize(HttpContentType.CBOR, payload)));
    });

    unknownServer = vertx
        .createHttpServer(
            new HttpServerOptions().setSsl(true).setPemKeyCertOptions(SelfSignedCertificate.create().keyCertOptions()))
        .requestHandler(dummyRouter::accept);
    startServer(unknownServer);
  }

  private static void startServer(HttpServer server) throws Exception {
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    server.listen(SecurityTestUtils.getFreePort(), result -> {
      if (result.succeeded()) {
        completion.complete();
      } else {
        completion.completeExceptionally(result.cause());
      }
    });
    completion.join();
  }

  @Test
  void testWhitelistedServer() throws Exception {
    CompletableAsyncResult<Integer> statusCode = AsyncResult.incomplete();
    client
        .post(
            whitelistedServer.actualPort(),
            "localhost",
            "/partyinfo",
            response -> statusCode.complete(response.statusCode()))
        .end();
    assertEquals((Integer) 200, statusCode.get());
  }

  @Test
  void testUnknownServer() {
    CompletableAsyncResult<Integer> statusCode = AsyncResult.incomplete();
    client
        .post(
            unknownServer.actualPort(),
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

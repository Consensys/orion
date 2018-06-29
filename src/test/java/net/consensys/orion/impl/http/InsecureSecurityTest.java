package net.consensys.orion.impl.http;

import static io.vertx.core.Vertx.vertx;
import static net.consensys.cava.net.tls.TLS.certificateHexFingerprint;
import static net.consensys.orion.impl.TestUtils.configureJDKTrustStore;
import static net.consensys.orion.impl.TestUtils.generateAndLoadConfiguration;
import static net.consensys.orion.impl.TestUtils.writeServerCertToConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.concurrent.CompletableAsyncResult;
import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.api.cmd.Orion;
import net.consensys.orion.api.config.Config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletionException;
import javax.net.ssl.SSLHandshakeException;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.net.SelfSignedCertificate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class InsecureSecurityTest {

  private static Vertx vertx = vertx();
  private static Path knownClientsFile;
  private static String exampleComFingerprint;
  private static HttpClient httpClient;
  private static int nodePort;
  private static Orion orion;

  @BeforeAll
  static void setUp(@TempDirectory Path tempDir) throws Exception {
    SelfSignedCertificate serverCertificate = SelfSignedCertificate.create("localhost");
    Config config = generateAndLoadConfiguration(tempDir, writer -> {
      writer.write("tlsservertrust='insecure-no-validation'\n");
      writeServerCertToConfig(writer, serverCertificate);
    });

    configureJDKTrustStore(serverCertificate, tempDir);
    knownClientsFile = config.tlsKnownClients();

    SelfSignedCertificate clientCertificate = SelfSignedCertificate.create("example.com");
    exampleComFingerprint = certificateHexFingerprint(Paths.get(clientCertificate.keyCertOptions().getCertPath()));
    httpClient = vertx
        .createHttpClient(new HttpClientOptions().setSsl(true).setKeyCertOptions(clientCertificate.keyCertOptions()));

    nodePort = config.nodePort();
    orion = new Orion(vertx);
    orion.run(System.out, System.err, config);
  }

  @AfterAll
  static void tearDown() {
    orion.stop();
    vertx.close();
  }

  @Test
  void testUpCheckOnNodePort() throws Exception {
    assertTrue(Files.readAllLines(knownClientsFile).isEmpty());
    for (int i = 0; i < 5; i++) {
      HttpClientRequest req = httpClient.get(nodePort, "localhost", "/upcheck");
      CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
      req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();
      HttpClientResponse resp = result.get();
      assertEquals(200, resp.statusCode());
    }
    List<String> fingerprints = Files.readAllLines(knownClientsFile);
    assertEquals(1, fingerprints.size());
    assertEquals("example.com " + exampleComFingerprint, fingerprints.get(0));
  }

  @Test
  void testWithoutSSLConfiguration() {
    CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
    HttpClient insecureClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true));
    HttpClientRequest req = insecureClient.get(nodePort, "localhost", "/upcheck");
    req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();

    CompletionException e = assertThrows(CompletionException.class, result::get);
    assertTrue(e.getCause() instanceof SSLHandshakeException);
  }
}

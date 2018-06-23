package net.consensys.orion.impl.http.handlers;

import static io.vertx.core.Vertx.vertx;
import static net.consensys.cava.crypto.Hash.sha2_256;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.concurrent.CompletableAsyncResult;
import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.api.cmd.Orion;
import net.consensys.orion.impl.config.MemoryConfig;
import net.consensys.orion.impl.http.SecurityTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CompletionException;
import javax.net.ssl.SSLHandshakeException;

import io.netty.util.internal.StringUtil;
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
class WhiteListSecurityTest {

  private static Vertx vertx = vertx();
  private static HttpClient httpClient;
  private static Orion orion;
  private static HttpClient httpClientWithUnregisteredCert;
  private static HttpClient httpClientWithImproperCertificate;
  private static HttpClient anotherExampleComClient;
  private static MemoryConfig config;

  @BeforeAll
  static void setUp(@TempDirectory Path tempDir) throws Exception {
    orion = new Orion(vertx);

    config = new MemoryConfig();
    config.setWorkDir(tempDir.resolve("data"));
    config.setTls("strict");
    config.setTlsServerTrust("whitelist");

    SecurityTestUtils.installServerCert(config, tempDir);

    SelfSignedCertificate clientCertificate = SelfSignedCertificate.create("example.com");

    Path knownClientsFile = tempDir.resolve("knownclients.txt");
    config.setTlsKnownClients(knownClientsFile);
    String fingerprint = StringUtil.toHexStringPadded(
        sha2_256(SecurityTestUtils.loadPEM(Paths.get(clientCertificate.keyCertOptions().getCertPath()))));
    Files.write(knownClientsFile, Arrays.asList("#First line", "example.com " + fingerprint));

    SecurityTestUtils.installPorts(config);
    orion.run(System.out, System.err, config);

    httpClient = vertx
        .createHttpClient(new HttpClientOptions().setSsl(true).setKeyCertOptions(clientCertificate.keyCertOptions()));

    SelfSignedCertificate fooCertificate = SelfSignedCertificate.create("foo.bar.baz");

    httpClientWithUnregisteredCert =
        vertx.createHttpClient(new HttpClientOptions().setSsl(true).setKeyCertOptions(fooCertificate.keyCertOptions()));

    SelfSignedCertificate noCNCert = SelfSignedCertificate.create("");

    httpClientWithImproperCertificate =
        vertx.createHttpClient(new HttpClientOptions().setSsl(true).setKeyCertOptions(noCNCert.keyCertOptions()));

    SelfSignedCertificate anotherExampleDotComCert = SelfSignedCertificate.create("example.com");

    anotherExampleComClient = vertx.createHttpClient(
        new HttpClientOptions().setSsl(true).setKeyCertOptions(anotherExampleDotComCert.keyCertOptions()));
  }

  @AfterAll
  static void tearDown() {
    orion.stop();
    vertx.close();
  }

  @Test
  void testUpCheckOnNodePort() throws Exception {
    for (int i = 0; i < 5; i++) {
      HttpClientRequest req = httpClient.get(config.nodePort(), "localhost", "/upcheck");
      CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
      req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();
      HttpClientResponse resp = result.get();
      assertEquals(200, resp.statusCode());
    }
  }

  @Test
  void testSameHostnameUnknownCertificate() {
    HttpClientRequest req = anotherExampleComClient.get(config.nodePort(), "localhost", "/upcheck");
    CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
    req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();
    CompletionException e = assertThrows(CompletionException.class, result::get);
    assertEquals("Received fatal alert: certificate_unknown", e.getCause().getCause().getMessage());
  }

  @Test
  void testUpCheckOnNodePortWithUnregisteredClientCert() {
    HttpClientRequest req = httpClientWithUnregisteredCert.get(config.nodePort(), "localhost", "/upcheck");
    CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
    req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();
    CompletionException e = assertThrows(CompletionException.class, result::get);
    assertEquals("Received fatal alert: certificate_unknown", e.getCause().getCause().getMessage());
  }

  @Test
  void testUpCheckOnNodePortWithInvalidClientCert() {
    HttpClientRequest req = httpClientWithImproperCertificate.get(config.nodePort(), "localhost", "/upcheck");
    CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
    req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();
    CompletionException e = assertThrows(CompletionException.class, result::get);
    assertEquals("Received fatal alert: certificate_unknown", e.getCause().getCause().getMessage());
  }

  @Test
  void testWithoutSSLConfiguration() {
    CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
    HttpClient insecureClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true));
    HttpClientRequest req = insecureClient.get(config.nodePort(), "localhost", "/upcheck");
    req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();

    CompletionException e = assertThrows(CompletionException.class, result::get);
    assertTrue(e.getCause() instanceof SSLHandshakeException);
  }
}

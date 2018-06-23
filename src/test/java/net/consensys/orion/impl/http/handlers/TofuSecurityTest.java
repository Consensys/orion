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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionException;
import javax.net.ssl.SSLHandshakeException;

import io.netty.util.internal.StringUtil;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.net.SelfSignedCertificate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class TofuSecurityTest {

  private Vertx vertx;
  private HttpClient httpClient;
  private Orion orion;
  private HttpClient anotherExampleComClient;
  private Path knownClientsFile;
  private MemoryConfig config;
  private String exampleComFingerprint;

  @BeforeEach
  void setUp(@TempDirectory Path tempDir) throws Exception {
    vertx = vertx();
    orion = new Orion(vertx);

    config = new MemoryConfig();
    config.setWorkDir(tempDir.resolve("data"));
    config.setTls("strict");
    config.setTlsServerTrust("tofu");

    SecurityTestUtils.installServerCert(config, tempDir);

    SelfSignedCertificate clientCertificate = SelfSignedCertificate.create("example.com");

    knownClientsFile = tempDir.resolve("knownclients.txt");
    config.setTlsKnownClients(knownClientsFile);
    exampleComFingerprint = StringUtil.toHexStringPadded(
        sha2_256(SecurityTestUtils.loadPEM(Paths.get(clientCertificate.keyCertOptions().getCertPath()))));
    Files.write(knownClientsFile, Collections.singletonList("#First line"));

    SecurityTestUtils.installPorts(config);
    orion.run(System.out, System.err, config);

    httpClient = vertx
        .createHttpClient(new HttpClientOptions().setSsl(true).setKeyCertOptions(clientCertificate.keyCertOptions()));

    SelfSignedCertificate anotherExampleDotComCert = SelfSignedCertificate.create("example.com");

    anotherExampleComClient = vertx.createHttpClient(
        new HttpClientOptions().setSsl(true).setKeyCertOptions(anotherExampleDotComCert.keyCertOptions()));
  }

  @AfterEach
  void tearDown() {
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
    List<String> fingerprints = Files.readAllLines(knownClientsFile);
    assertEquals(2, fingerprints.size(), String.join("\n", fingerprints));
    assertEquals("#First line", fingerprints.get(0));
    assertEquals("example.com " + exampleComFingerprint, fingerprints.get(1));
  }

  @Test
  void testSameHostnameUnknownCertificate() throws Exception {
    testUpCheckOnNodePort();
    HttpClientRequest req = anotherExampleComClient.get(config.nodePort(), "localhost", "/upcheck");
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

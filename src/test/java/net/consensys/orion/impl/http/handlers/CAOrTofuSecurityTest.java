package net.consensys.orion.impl.http.handlers;

import static io.vertx.core.Vertx.vertx;
import static net.consensys.cava.crypto.Hash.sha2_256;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import java.util.List;
import javax.net.ssl.SSLHandshakeException;

import io.netty.util.internal.StringUtil;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.net.SelfSignedCertificate;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class CAOrTofuSecurityTest {

  private static Vertx vertx = vertx();

  private static HttpClient httpClient;

  private static Orion orion;

  private static HttpClient nonCAhttpClient;
  private static Path knownClientsFile;
  private static String exampleComFingerprint;
  private static MemoryConfig config;

  @BeforeAll
  static void setUp(@TempDirectory Path tempDir) throws Exception {
    SelfSignedCertificate clientCert = SelfSignedCertificate.create("localhost");

    orion = new Orion(vertx);
    config = new MemoryConfig();
    config.setWorkDir(tempDir.resolve("data"));
    config.setTls("strict");
    config.setTlsServerTrust("ca-or-tofu");
    knownClientsFile = tempDir.resolve("knownclients.txt");
    config.setTlsKnownClients(knownClientsFile);

    SecurityTestUtils.installServerCert(config, tempDir);
    SecurityTestUtils.configureJDKTrustStore(clientCert, tempDir);


    SecurityTestUtils.installPorts(config);

    SelfSignedCertificate nonCAClientCertificate = SelfSignedCertificate.create("localhost");

    exampleComFingerprint = StringUtil.toHexStringPadded(
        sha2_256(SecurityTestUtils.loadPEM(Paths.get(nonCAClientCertificate.keyCertOptions().getCertPath()))));

    nonCAhttpClient = vertx.createHttpClient(
        new HttpClientOptions().setSsl(true).setTrustAll(true).setKeyCertOptions(
            nonCAClientCertificate.keyCertOptions()));

    httpClient = vertx.createHttpClient(
        new HttpClientOptions().setSsl(true).setTrustAll(true).setKeyCertOptions(clientCert.keyCertOptions()));

    orion.run(System.out, System.err, config);
  }

  @AfterAll
  static void tearDown() {
    System.clearProperty("javax.net.ssl.trustStore");
    System.clearProperty("javax.net.ssl.trustStorePassword");
    orion.stop();
    vertx.close();
  }

  @Test
  void testTofuThenCA() throws Exception {
    {
      HttpClientRequest req = nonCAhttpClient.get(config.nodePort(), "localhost", "/upcheck");
      CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
      req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();
      HttpClientResponse resp = result.get();
      assertEquals(200, resp.statusCode());
    }
    List<String> fingerprints = Files.readAllLines(knownClientsFile);
    assertEquals(1, fingerprints.size(), String.join("\n", fingerprints));
    assertEquals("localhost " + exampleComFingerprint, fingerprints.get(0));

    HttpClientRequest req = httpClient.get(config.nodePort(), "localhost", "/upcheck");
    CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
    req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();
    HttpClientResponse resp = result.get();
    assertEquals(200, resp.statusCode());

    fingerprints = Files.readAllLines(knownClientsFile);
    assertEquals(1, fingerprints.size(), String.join("\n", fingerprints));
  }

  @Test
  void testWithoutSSLConfiguration() {
    OkHttpClient unsecureHttpClient = new OkHttpClient.Builder().build();

    Request upcheckRequest = new Request.Builder().url("https://localhost:" + config.nodePort() + "/upcheck").build();
    assertThrows(SSLHandshakeException.class, () -> unsecureHttpClient.newCall(upcheckRequest).execute());
  }
}

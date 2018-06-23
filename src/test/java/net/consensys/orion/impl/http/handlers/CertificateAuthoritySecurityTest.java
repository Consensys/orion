package net.consensys.orion.impl.http.handlers;

import static io.vertx.core.Vertx.vertx;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.concurrent.CompletableAsyncResult;
import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.api.cmd.Orion;
import net.consensys.orion.impl.config.MemoryConfig;
import net.consensys.orion.impl.http.SecurityTestUtils;

import java.nio.file.Path;
import javax.net.ssl.SSLException;

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
class CertificateAuthoritySecurityTest {

  private static Vertx vertx = vertx();
  private static HttpClient httpClient;
  private static Orion orion;
  private static MemoryConfig config;

  @BeforeAll
  static void setUp(@TempDirectory Path tempDir) throws Exception {
    orion = new Orion(vertx);
    config = new MemoryConfig();
    config.setWorkDir(tempDir.resolve("data"));
    config.setTls("strict");
    config.setTlsServerTrust("ca");
    Path knownClientsFile = tempDir.resolve("knownclients.txt");
    config.setTlsKnownClients(knownClientsFile);
    SecurityTestUtils.installServerCert(config, tempDir);

    SelfSignedCertificate clientCert = SelfSignedCertificate.create();
    SecurityTestUtils.configureJDKTrustStore(clientCert, tempDir);
    SecurityTestUtils.installPorts(config);

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
  void testUpCheckOnNodePort() throws Exception {
    HttpClientRequest req = httpClient.get(config.nodePort(), "localhost", "/upcheck");
    CompletableAsyncResult<HttpClientResponse> result = AsyncResult.incomplete();
    req.handler(result::complete).exceptionHandler(result::completeExceptionally).end();
    HttpClientResponse resp = result.get();
    assertEquals(200, resp.statusCode());
  }

  @Test
  void testWithoutSSLConfiguration() {
    OkHttpClient unsecureHttpClient = new OkHttpClient.Builder().build();

    Request upcheckRequest = new Request.Builder().url("https://localhost:" + config.nodePort() + "/upcheck").build();
    assertThrows(SSLException.class, () -> unsecureHttpClient.newCall(upcheckRequest).execute());
  }
}

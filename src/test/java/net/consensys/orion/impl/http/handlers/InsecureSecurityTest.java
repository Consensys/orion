package net.consensys.orion.impl.http.handlers;

import static io.vertx.core.Vertx.vertx;
import static net.consensys.cava.crypto.Hash.sha2_256;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.consensys.orion.api.cmd.Orion;
import net.consensys.orion.impl.config.MemoryConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class InsecureSecurityTest {

  private static Vertx vertx = vertx();

  private static HttpClient httpClient;

  private static Orion orion;

  private static Path knownClientsFile;
  private static String exampleComFingerprint;
  private static MemoryConfig config;

  @BeforeClass
  public static void setUp() throws Exception {
    Path workDir = Files.createTempDirectory("data");
    orion = new Orion(vertx);

    config = new MemoryConfig();
    config.setWorkDir(workDir);
    config.setTls("strict");
    config.setTlsServerTrust("insecure-no-validation");
    SecurityTestUtils.installServerCert(config);

    knownClientsFile = Files.createTempFile("knownclients", ".txt");
    config.setTlsKnownClients(knownClientsFile);

    SecurityTestUtils.installPorts(config);
    orion.run(System.out, System.err, config);

    SelfSignedCertificate clientCertificate = SelfSignedCertificate.create();

    exampleComFingerprint = StringUtil.toHexStringPadded(
        sha2_256(SecurityTestUtils.loadPEM(Paths.get(clientCertificate.keyCertOptions().getCertPath()))));

    httpClient = vertx
        .createHttpClient(new HttpClientOptions().setSsl(true).setKeyCertOptions(clientCertificate.keyCertOptions()));

    SelfSignedCertificate certificate = SelfSignedCertificate.create();
  }

  @AfterClass
  public static void tearDown() {
    orion.stop();
    vertx.close();
  }

  @Test
  public void testUpCheckOnNodePort() throws Exception {
    assertTrue(Files.readAllLines(knownClientsFile).isEmpty());
    for (int i = 0; i < 5; i++) {
      HttpClientRequest req = httpClient.get(config.nodePort(), "localhost", "/upcheck");
      CompletableFuture<HttpClientResponse> respFuture = new CompletableFuture<>();
      req.handler(respFuture::complete).exceptionHandler(respFuture::completeExceptionally).end();
      HttpClientResponse resp = respFuture.join();
      assertEquals(200, resp.statusCode());
    }
    List<String> fingerprints = Files.readAllLines(knownClientsFile);
    assertEquals(1, fingerprints.size());
    assertEquals("example.com " + exampleComFingerprint, fingerprints.get(0));
  }

  @Test(expected = SSLHandshakeException.class)
  public void testWithoutSSLConfiguration() throws Exception {
    OkHttpClient unsecureHttpClient = new OkHttpClient.Builder().build();

    Request upcheckRequest = new Request.Builder().url("https://localhost:" + config.nodePort() + "/upcheck").build();
    unsecureHttpClient.newCall(upcheckRequest).execute();
  }
}

package net.consensys.orion.impl.http.handlers;

import static io.vertx.core.Vertx.vertx;
import static net.consensys.cava.crypto.Hash.sha2_256;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import net.consensys.orion.api.cmd.Orion;
import net.consensys.orion.impl.config.MemoryConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TofuSecurityTest {

  private Vertx vertx;

  private HttpClient httpClient;

  private Orion orion;

  private HttpClient anotherExampleComClient;
  private Path knownClientsFile;
  private MemoryConfig config;
  private String exampleComFingerprint;

  @Before
  public void setUp() throws Exception {
    vertx = vertx();
    Path workDir = Files.createTempDirectory("data");
    orion = new Orion(vertx);

    config = new MemoryConfig();
    config.setWorkDir(workDir);
    config.setTls("strict");
    config.setTlsServerTrust("tofu");

    SecurityTestUtils.installServerCert(config);

    SelfSignedCertificate clientCertificate = SelfSignedCertificate.create("example.com");

    knownClientsFile = Files.createTempFile("knownclients", ".txt");
    config.setTlsKnownClients(knownClientsFile);
    exampleComFingerprint = StringUtil.toHexStringPadded(
        sha2_256(SecurityTestUtils.loadPEM(Paths.get(clientCertificate.keyCertOptions().getCertPath()))));
    Files.write(knownClientsFile, Arrays.asList("#First line"));

    SecurityTestUtils.installPorts(config);
    orion.run(System.out, System.err, config);

    httpClient = vertx
        .createHttpClient(new HttpClientOptions().setSsl(true).setKeyCertOptions(clientCertificate.keyCertOptions()));

    SelfSignedCertificate anotherExampleDotComCert = SelfSignedCertificate.create("example.com");

    anotherExampleComClient = vertx.createHttpClient(
        new HttpClientOptions().setSsl(true).setKeyCertOptions(anotherExampleDotComCert.keyCertOptions()));
  }

  @After
  public void tearDown() {
    orion.stop();
    vertx.close();
  }

  @Test
  public void testUpCheckOnNodePort() throws Exception {
    for (int i = 0; i < 5; i++) {
      HttpClientRequest req = httpClient.get(config.nodePort(), "localhost", "/upcheck");
      CompletableFuture<HttpClientResponse> respFuture = new CompletableFuture<>();
      req.handler(respFuture::complete).exceptionHandler(respFuture::completeExceptionally).end();
      HttpClientResponse resp = respFuture.join();
      assertEquals(200, resp.statusCode());
    }
    List<String> fingerprints = Files.readAllLines(knownClientsFile);
    assertEquals(String.join("\n", fingerprints), 2, fingerprints.size());
    assertEquals("#First line", fingerprints.get(0));
    assertEquals("example.com " + exampleComFingerprint, fingerprints.get(1));
  }

  @Test
  public void testSameHostnameUnknownCertificate() throws Exception {
    testUpCheckOnNodePort();
    HttpClientRequest req = anotherExampleComClient.get(config.nodePort(), "localhost", "/upcheck");
    CompletableFuture<HttpClientResponse> respFuture = new CompletableFuture<>();
    req.handler(respFuture::complete).exceptionHandler(respFuture::completeExceptionally).end();
    boolean caught = false;
    try {
      respFuture.join();
      fail();
    } catch (CompletionException e) {
      assertEquals("Received fatal alert: certificate_unknown", e.getCause().getCause().getMessage());
      caught = true;
    }
    assertTrue(caught);
  }

  @Test(expected = SSLHandshakeException.class)
  public void testWithoutSSLConfiguration() throws Exception {
    OkHttpClient unsecureHttpClient = new OkHttpClient.Builder().build();

    Request upcheckRequest = new Request.Builder().url("https://localhost:" + config.nodePort() + "/upcheck").build();
    unsecureHttpClient.newCall(upcheckRequest).execute();
  }
}

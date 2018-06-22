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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class WhiteListSecurityTest {

  private static Vertx vertx = vertx();

  private static HttpClient httpClient;

  private static Orion orion;

  private static HttpClient httpClientWithUnregisteredCert;
  private static HttpClient httpClientWithImproperCertificate;
  private static HttpClient anotherExampleComClient;
  private static MemoryConfig config;

  @BeforeClass
  public static void setUp() throws Exception {
    orion = new Orion(vertx);

    config = new MemoryConfig();
    config.setWorkDir(Files.createTempDirectory("data"));
    config.setTls("strict");
    config.setTlsServerTrust("whitelist");

    SecurityTestUtils.installServerCert(config);

    SelfSignedCertificate clientCertificate = SelfSignedCertificate.create("example.com");

    Path knownClientsFile = Files.createTempFile("knownclients", ".txt");
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

  @AfterClass
  public static void tearDown() {
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
  }

  @Test
  public void testSameHostnameUnknownCertificate() throws Exception {
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

  @Test
  public void testUpCheckOnNodePortWithUnregisteredClientCert() throws Exception {
    HttpClientRequest req = httpClientWithUnregisteredCert.get(config.nodePort(), "localhost", "/upcheck");
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

  @Test
  public void testUpCheckOnNodePortWithInvalidClientCert() throws Exception {
    HttpClientRequest req = httpClientWithImproperCertificate.get(config.nodePort(), "localhost", "/upcheck");
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

package net.consensys.orion.impl.http.handlers;

import static io.vertx.core.Vertx.vertx;
import static org.junit.Assert.assertEquals;

import net.consensys.orion.api.cmd.Orion;
import net.consensys.orion.api.network.TrustManagerFactoryWrapper;
import net.consensys.orion.impl.config.MemoryConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.net.ssl.SSLException;

import com.google.common.hash.Hashing;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.internal.StringUtil;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.net.SelfSignedCertificate;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.logging.log4j.util.Strings;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class CAOrTofuSecurityTest {

  private static Vertx vertx = vertx();

  private static HttpClient httpClient;

  private static Orion orion;

  private static HttpClient nonCAhttpClient;
  private static Path knownClientsFile;
  private static String exampleComFingerprint;
  private static MemoryConfig config;

  @BeforeClass
  public static void setUp() throws Exception {
    SelfSignedCertificate clientCert = SelfSignedCertificate.create();
    SecurityTestUtils.configureJDKTrustStore(clientCert);

    Path workDir = Files.createTempDirectory("data");
    orion = new Orion(vertx);
    config = new MemoryConfig();
    config.setWorkDir(workDir);
    config.setTls("strict");
    config.setTlsServerTrust("ca-or-tofu");
    knownClientsFile = Files.createTempFile("knownclients", ".txt");
    config.setTlsKnownClients(knownClientsFile);

    SecurityTestUtils.installServerCert(config);


    SecurityTestUtils.installPorts(config);

    SelfSignedCertificate nonCAClientCertificate = SelfSignedCertificate.create();

    exampleComFingerprint = StringUtil.toHexStringPadded(
        Hashing
            .sha1()
            .hashBytes(SecurityTestUtils.loadPEM(Paths.get(nonCAClientCertificate.keyCertOptions().getCertPath())))
            .asBytes());

    nonCAhttpClient = vertx.createHttpClient(
        new HttpClientOptions()
            .setTrustOptions(new TrustManagerFactoryWrapper(InsecureTrustManagerFactory.INSTANCE))
            .setSsl(true)
            .setKeyCertOptions(nonCAClientCertificate.keyCertOptions()));

    httpClient = vertx.createHttpClient(
        new HttpClientOptions()
            .setTrustOptions(new TrustManagerFactoryWrapper(InsecureTrustManagerFactory.INSTANCE))
            .setSsl(true)
            .setKeyCertOptions(clientCert.keyCertOptions()));

    orion.run(System.out, System.err, config);
  }

  @AfterClass
  public static void tearDown() {
    System.clearProperty("javax.net.ssl.trustStore");
    System.clearProperty("javax.net.ssl.trustStorePassword");
    orion.stop();
    vertx.close();
  }

  @Test
  public void testTofuThenCA() throws Exception {
    {
      HttpClientRequest req = nonCAhttpClient.get(config.nodePort(), "localhost", "/upcheck");
      CompletableFuture<HttpClientResponse> respFuture = new CompletableFuture<>();
      req.handler(respFuture::complete).exceptionHandler(respFuture::completeExceptionally).end();
      HttpClientResponse resp = respFuture.join();
      assertEquals(200, resp.statusCode());
    }
    List<String> fingerprints = Files.readAllLines(knownClientsFile);
    assertEquals(Strings.join(fingerprints, '\n'), 1, fingerprints.size());
    assertEquals("example.com " + exampleComFingerprint, fingerprints.get(0));

    HttpClientRequest req = httpClient.get(config.nodePort(), "localhost", "/upcheck");
    CompletableFuture<HttpClientResponse> respFuture = new CompletableFuture<>();
    req.handler(respFuture::complete).exceptionHandler(respFuture::completeExceptionally).end();
    HttpClientResponse resp = respFuture.join();
    assertEquals(200, resp.statusCode());

    fingerprints = Files.readAllLines(knownClientsFile);
    assertEquals(Strings.join(fingerprints, '\n'), 1, fingerprints.size());
  }

  @Test(expected = SSLException.class)
  public void testWithoutSSLConfiguration() throws Exception {
    OkHttpClient unsecureHttpClient = new OkHttpClient.Builder().build();

    Request upcheckRequest = new Request.Builder().url("https://localhost:" + config.nodePort() + "/upcheck").build();
    unsecureHttpClient.newCall(upcheckRequest).execute();
  }
}

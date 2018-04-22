package net.consensys.orion.impl.http.handlers;

import static io.vertx.core.Vertx.vertx;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

import net.consensys.orion.api.cmd.Orion;
import net.consensys.orion.api.network.TrustManagerFactoryWrapper;
import net.consensys.orion.impl.config.MemoryConfig;
import net.consensys.orion.impl.utils.Base64;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import javax.net.ssl.SSLException;

import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
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

public class CertificateAuthoritySecurityTest {

  private static Vertx vertx = vertx();

  private static HttpClient httpClient;

  private static Orion orion;

  private static int nodePort;

  private static int clientPort;

  public static void setCATruststore(SelfSignedCertificate clientCert) throws Exception {
    KeyStore ks = KeyStore.getInstance("JKS");
    ks.load(null, null);

    KeyFactory kf = KeyFactory.getInstance("RSA");
    PKCS8EncodedKeySpec keysp = new PKCS8EncodedKeySpec(loadPEM(new File(clientCert.privateKeyPath()).toPath()));
    PrivateKey clientPrivateKey = kf.generatePrivate(keysp);
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    Certificate certificate = cf.generateCertificate(
        new ByteArrayInputStream(Files.readAllBytes(new File(clientCert.certificatePath()).toPath())));
    ks.setCertificateEntry("clientCert", certificate);
    ks.setKeyEntry("client", clientPrivateKey, "changeit".toCharArray(), new Certificate[] {certificate});
    Path tempKeystore = Files.createTempFile("keystore", ".jks");
    try (FileOutputStream output = new FileOutputStream(tempKeystore.toFile());) {
      ks.store(output, "changeit".toCharArray());
    }
    System.setProperty("javax.net.ssl.trustStore", tempKeystore.toString());
    System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
  }

  public static byte[] loadPEM(Path pemFilePath) throws IOException {
    String pem = new String(Files.readAllBytes(pemFilePath), UTF_8);
    Pattern parse = Pattern.compile("(?m)(?s)^---*BEGIN.*---*$(.*)^---*END.*---*$.*");
    String encoded = parse.matcher(pem).replaceFirst("$1").replaceAll("\\s", "");
    return Base64.decode(encoded);
  }

  @BeforeClass
  public static void setUp() throws Exception {
    Path workDir = Files.createTempDirectory("data");
    orion = new Orion(vertx);
    MemoryConfig config = new MemoryConfig();
    config.setWorkDir(workDir);
    config.setTls("strict");
    config.setTlsServerTrust("ca");
    config.setTlsKnownServers(Files.createTempFile("knownservers", ".txt"));
    Path knownClientsFile = Files.createTempFile("knownclients", ".txt");
    config.setTlsKnownClients(knownClientsFile);
    installServerCert(config);

    SelfSignedCertificate clientCert = SelfSignedCertificate.create();
    setCATruststore(clientCert);

    installPorts(config);

    httpClient = vertx.createHttpClient(
        new HttpClientOptions()
            .setSsl(true)
            .setTrustOptions(new TrustManagerFactoryWrapper(InsecureTrustManagerFactory.INSTANCE))
            .setKeyCertOptions(clientCert.keyCertOptions()));

    orion.run(System.out, System.err, config);
  }

  public static void installPorts(MemoryConfig config) throws Exception {
    try (ServerSocket nodeSocket = new ServerSocket(0); ServerSocket clientSocket = new ServerSocket(0)) {
      nodePort = nodeSocket.getLocalPort();
      clientPort = clientSocket.getLocalPort();
      config.setNodePort(nodePort);
      config.setClientPort(clientPort);
    }
  }

  public static void installServerCert(MemoryConfig config) {
    SelfSignedCertificate serverCert = SelfSignedCertificate.create();
    config.setTlsServerCert(new File(serverCert.certificatePath()).toPath());
    config.setTlsServerKey(new File(serverCert.privateKeyPath()).toPath());
  }

  @AfterClass
  public static void tearDown() {
    System.clearProperty("javax.net.ssl.trustStore");
    System.clearProperty("javax.net.ssl.trustStorePassword");
    orion.stop();
    vertx.close();
  }

  @Test
  public void testUpCheckOnNodePort() throws Exception {
    HttpClientRequest req = httpClient.get(nodePort, "localhost", "/upcheck");
    CompletableFuture<HttpClientResponse> respFuture = new CompletableFuture<>();
    req.handler(respFuture::complete).exceptionHandler(respFuture::completeExceptionally).end();
    HttpClientResponse resp = respFuture.join();
    assertEquals(200, resp.statusCode());
  }

  @Test(expected = SSLException.class)
  public void testWithoutSSLConfiguration() throws Exception {
    OkHttpClient unsecureHttpClient = new OkHttpClient.Builder().build();

    Request upcheckRequest = new Request.Builder().url("https://localhost:" + nodePort + "/upcheck").build();
    unsecureHttpClient.newCall(upcheckRequest).execute();
  }
}

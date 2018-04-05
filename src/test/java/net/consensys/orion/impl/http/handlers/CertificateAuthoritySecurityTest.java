package net.consensys.orion.impl.http.handlers;

import static org.junit.Assert.*;

import net.consensys.orion.api.cmd.Orion;
import net.consensys.orion.impl.config.MemoryConfig;

import java.io.File;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class CertificateAuthoritySecurityTest {

  private static OkHttpClient httpClient = new OkHttpClient();

  private static Orion orion;

  private static int publicPort;

  private static int privatePort;

  private static KeyStore readKeyStore() throws Exception {
    URL resource = CertificateAuthoritySecurityTest.class.getClassLoader().getResource("ssl/keystore.jks");
    KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
    keystore.load(resource.openStream(), "changeit".toCharArray());
    return keystore;
  }

  @BeforeClass
  public static void setUp() throws Exception {
    Path workDir = Files.createTempDirectory("data");
    orion = new Orion();
    MemoryConfig config = new MemoryConfig();
    config.setWorkDir(workDir);
    config.setTls("strict");
    config.setTlsServerTrust("ca");
    URL serverCert = CertificateAuthoritySecurityTest.class.getClassLoader().getResource("ssl/server.crt");
    config.setTlsServerCert(new File(serverCert.getFile()).toPath());
    URL serverKey = CertificateAuthoritySecurityTest.class.getClassLoader().getResource("ssl/privkey.pem");
    config.setTlsServerKey(new File(serverKey.getFile()).toPath());

    try (ServerSocket publicSocket = new ServerSocket(0); ServerSocket privateSocket = new ServerSocket(0)) {
      publicPort = publicSocket.getLocalPort();
      privatePort = privateSocket.getLocalPort();
      config.setPort(publicPort);
      config.setPrivacyPort(privatePort);
    }
    orion.run(System.out, System.err, config);

    KeyStore keyStore = readKeyStore();
    SSLContext sslContext = SSLContext.getInstance("SSL");
    TrustManagerFactory trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(keyStore);

    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(keyStore, "changeit".toCharArray());
    sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
    httpClient = new OkHttpClient.Builder()
        .sslSocketFactory(
            sslContext.getSocketFactory(),
            (X509ExtendedTrustManager) trustManagerFactory.getTrustManagers()[0])
        .hostnameVerifier((s, sslSession) -> "localhost".equals(s))
        .build();
  }

  @AfterClass
  public static void tearDown() {
    orion.stop();
  }

  @Test
  public void testUpCheckOnPublicPort() throws Exception {
    Request upcheckRequest = new Request.Builder().url("https://localhost:" + publicPort + "/upcheck").build();
    Response resp = httpClient.newCall(upcheckRequest).execute();
    assertEquals(200, resp.code());
  }

  @Test(expected = SSLException.class)
  public void testWithoutSSLConfiguration() throws Exception {
    OkHttpClient unsecureHttpClient = new OkHttpClient.Builder().build();

    Request upcheckRequest = new Request.Builder().url("https://localhost:" + publicPort + "/upcheck").build();
    unsecureHttpClient.newCall(upcheckRequest).execute();
  }
}

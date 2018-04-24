package net.consensys.orion.impl.http.handlers;

import static java.nio.charset.StandardCharsets.UTF_8;

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
import java.util.regex.Pattern;

import io.vertx.core.net.SelfSignedCertificate;

public class SecurityTestUtils {

  private SecurityTestUtils() {}

  public static void configureJDKTrustStore(SelfSignedCertificate clientCert) throws Exception {
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
    String encoded = parse.matcher(pem).replaceFirst("$1").replace("\n", "");
    return Base64.decode(encoded);
  }

  public static void installPorts(MemoryConfig config) throws Exception {
    try (ServerSocket nodeSocket = new ServerSocket(0); ServerSocket clientSocket = new ServerSocket(0)) {
      int nodePort = nodeSocket.getLocalPort();
      int clientPort = clientSocket.getLocalPort();
      config.setNodePort(nodePort);
      config.setClientPort(clientPort);
    }
  }

  public static void installServerCert(MemoryConfig config) {
    SelfSignedCertificate serverCert = SelfSignedCertificate.create();
    config.setTlsServerCert(new File(serverCert.certificatePath()).toPath());
    config.setTlsServerKey(new File(serverCert.privateKeyPath()).toPath());
  }

  public static int getFreePort() throws Exception {
    try (ServerSocket nodeSocket = new ServerSocket(0);) {
      return nodeSocket.getLocalPort();
    }
  }
}

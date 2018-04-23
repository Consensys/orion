package net.consensys.orion.api.cmd;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

class TLSEnvironmentHelper {

  static {
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
  }

  private TLSEnvironmentHelper() {}

  private static void createDirectories(Path path) {
    try {
      Files.createDirectories(path);
    } catch (IOException ex) {
      throw new OrionStartException(
          "Couldn't create working directory '" + path.toString() + "': " + ex.getMessage(),
          ex);
    }
  }

  private static void createFile(Path workDir, Path file) {
    createDirectories(workDir.resolve(file).getParent());
    try {
      workDir.resolve(file).toFile().createNewFile();
    } catch (IOException ex) {
      throw new OrionStartException("Couldn't create file '" + file + "': " + ex.getMessage(), ex);
    }
  }

  private static void createSelfSignedCertificate(Date now, Path key, Path cert) throws NoSuchAlgorithmException,
      IOException,
      OperatorCreationException,
      CertificateException {
    KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA");
    rsa.initialize(2048, new SecureRandom());

    KeyPair keyPair = rsa.generateKeyPair();

    Calendar cal = Calendar.getInstance();
    cal.setTime(now);
    cal.add(Calendar.YEAR, 1);
    Date yearFromNow = cal.getTime();

    X500Name dn = new X500Name("CN=example.com");

    X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
        dn,
        new BigInteger(64, new SecureRandom()),
        now,
        yearFromNow,
        dn,
        keyPair.getPublic());

    ContentSigner signer =
        new JcaContentSignerBuilder("SHA256WithRSAEncryption").setProvider("BC").build(keyPair.getPrivate());
    X509Certificate x509Certificate =
        new JcaX509CertificateConverter().setProvider("BC").getCertificate(builder.build(signer));

    try (BufferedWriter writer = Files.newBufferedWriter(key, UTF_8); PemWriter pemWriter = new PemWriter(writer)) {
      pemWriter.writeObject(new PemObject("PRIVATE KEY", keyPair.getPrivate().getEncoded()));
    }

    try (BufferedWriter writer = Files.newBufferedWriter(cert, UTF_8); PemWriter pemWriter = new PemWriter(writer)) {
      pemWriter.writeObject(new PemObject("CERTIFICATE", x509Certificate.getEncoded()));
    }
  }

  private static void generateKeyPairIfMissing(Date now, Path cert, Path key) {
    if (!Files.exists(cert) || !Files.exists(key)) {
      createDirectories(cert.getParent());
      createDirectories(key.getParent());

      Path keyFile;
      Path certFile;
      try {
        keyFile = Files.createTempFile(key.getParent(), "client-key", ".tmp");
        certFile = Files.createTempFile(cert.getParent(), "client-cert", ".tmp");
      } catch (IOException e) {
        throw new OrionStartException(
            "Could not write temporary files when generating certificate " + e.getMessage(),
            e);
      }

      try {
        createSelfSignedCertificate(now, keyFile, certFile);
      } catch (CertificateException | NoSuchAlgorithmException | OperatorCreationException | IOException e) {
        throw new OrionStartException("Could not generate certificate " + e.getMessage(), e);
      }

      try {
        Files.move(keyFile, key, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        throw new OrionStartException("Error writing private key " + key.toString(), e);
      }

      try {
        Files.move(certFile, cert, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        throw new OrionStartException("Error writing public key " + cert.toString(), e);
      }
    }
  }

  static void configureTLSRelatedFiles(
      Path workDir,
      Path knownClients,
      Path knownServers,
      Path tlsClientCert,
      Path tlsClientKey,
      Path tlsServerCert,
      Path tlsServerKey) {

    createFile(workDir, knownClients);
    createFile(workDir, knownServers);

    Date now = new Date();
    generateKeyPairIfMissing(now, workDir.resolve(tlsClientCert), workDir.resolve(tlsClientKey));
    generateKeyPairIfMissing(now, workDir.resolve(tlsServerCert), workDir.resolve(tlsServerKey));
  }
}

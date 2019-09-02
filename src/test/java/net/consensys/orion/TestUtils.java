/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.orion;

import static com.google.common.base.Charsets.UTF_8;
import static net.consensys.cava.net.tls.TLS.readPemFile;

import net.consensys.cava.io.IOConsumer;
import net.consensys.orion.config.Config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;

import io.vertx.core.net.SelfSignedCertificate;

public class TestUtils {

  private TestUtils() {}

  public static void configureJDKTrustStore(final SelfSignedCertificate clientCert, final Path tempDir)
      throws Exception {
    final KeyStore ks = KeyStore.getInstance("JKS");
    ks.load(null, null);

    final KeyFactory kf = KeyFactory.getInstance("RSA");
    final PKCS8EncodedKeySpec keysp = new PKCS8EncodedKeySpec(readPemFile(Paths.get(clientCert.privateKeyPath())));
    final PrivateKey clientPrivateKey = kf.generatePrivate(keysp);
    final CertificateFactory cf = CertificateFactory.getInstance("X.509");
    final Certificate certificate = cf.generateCertificate(
        new ByteArrayInputStream(Files.readAllBytes(new File(clientCert.certificatePath()).toPath())));
    ks.setCertificateEntry("clientCert", certificate);
    ks.setKeyEntry("client", clientPrivateKey, "changeit".toCharArray(), new Certificate[] {certificate});
    final Path tempKeystore = tempDir.resolve("keystore.jks");
    try (final FileOutputStream output = new FileOutputStream(tempKeystore.toFile())) {
      ks.store(output, "changeit".toCharArray());
    }
    System.setProperty("javax.net.ssl.trustStore", tempKeystore.toString());
    System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
  }

  public static Config generateAndLoadConfiguration(final Path rootDir, final IOConsumer<Writer> consumer)
      throws IOException {
    final Path workDir = rootDir.resolve("data").toAbsolutePath();
    final Path knownClientsFile = rootDir.resolve("knownclients.txt").toAbsolutePath();
    final Path knownServersFile = rootDir.resolve("knownservers.txt").toAbsolutePath();
    final Path configFile = rootDir.resolve("config");
    try (final Writer writer = Files.newBufferedWriter(configFile, UTF_8)) {
      writer.write("tls='strict'\n");
      writer.write("workdir='" + workDir + "'\n");
      writer.write("tlsknownclients='" + knownClientsFile + "'\n");
      writer.write("tlsknownservers='" + knownServersFile + "'\n");
      findFreePortsAndWriteToConfig(writer);
      consumer.accept(writer);
    }
    return Config.load(configFile);
  }

  public static void findFreePortsAndWriteToConfig(final Writer configWriter) throws IOException {
    try (final ServerSocket nodeSocket = new ServerSocket(0); final ServerSocket clientSocket = new ServerSocket(0)) {
      final int nodePort = nodeSocket.getLocalPort();
      final int clientPort = clientSocket.getLocalPort();
      configWriter.write("nodeport=" + nodePort + "\n");
      configWriter.write("clientport=" + clientPort + "\n");
    }
  }

  public static void writeServerCertToConfig(final Writer configWriter, final SelfSignedCertificate serverCert)
      throws IOException {
    configWriter.write("tlsservercert=\"" + serverCert.certificatePath() + "\"\n");
    configWriter.write("tlsserverkey=\"" + serverCert.privateKeyPath() + "\"\n");
  }

  public static void writeClientCertToConfig(final Writer configWriter, final SelfSignedCertificate serverCert)
      throws IOException {
    configWriter.write("tlsclientcert=\"" + serverCert.certificatePath() + "\"\n");
    configWriter.write("tlsclientkey=\"" + serverCert.privateKeyPath() + "\"\n");
  }

  public static int getFreePort() throws Exception {
    try (final ServerSocket nodeSocket = new ServerSocket(0)) {
      return nodeSocket.getLocalPort();
    }
  }
}

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

  public static void configureJDKTrustStore(SelfSignedCertificate clientCert, Path tempDir) throws Exception {
    KeyStore ks = KeyStore.getInstance("JKS");
    ks.load(null, null);

    KeyFactory kf = KeyFactory.getInstance("RSA");
    PKCS8EncodedKeySpec keysp = new PKCS8EncodedKeySpec(readPemFile(Paths.get(clientCert.privateKeyPath())));
    PrivateKey clientPrivateKey = kf.generatePrivate(keysp);
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    Certificate certificate = cf.generateCertificate(
        new ByteArrayInputStream(Files.readAllBytes(new File(clientCert.certificatePath()).toPath())));
    ks.setCertificateEntry("clientCert", certificate);
    ks.setKeyEntry("client", clientPrivateKey, "changeit".toCharArray(), new Certificate[] {certificate});
    Path tempKeystore = tempDir.resolve("keystore.jks");
    try (FileOutputStream output = new FileOutputStream(tempKeystore.toFile())) {
      ks.store(output, "changeit".toCharArray());
    }
    System.setProperty("javax.net.ssl.trustStore", tempKeystore.toString());
    System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
  }

  public static Config generateAndLoadConfiguration(Path rootDir, IOConsumer<Writer> consumer) throws IOException {
    Path workDir = rootDir.resolve("data").toAbsolutePath();
    Path knownClientsFile = rootDir.resolve("knownclients.txt").toAbsolutePath();
    Path knownServersFile = rootDir.resolve("knownservers.txt").toAbsolutePath();
    Path configFile = rootDir.resolve("config");
    try (Writer writer = Files.newBufferedWriter(configFile, UTF_8)) {
      writer.write("tls='strict'\n");
      writer.write("workdir='" + workDir + "'\n");
      writer.write("tlsknownclients='" + knownClientsFile + "'\n");
      writer.write("tlsknownservers='" + knownServersFile + "'\n");
      findFreePortsAndWriteToConfig(writer);
      consumer.accept(writer);
    }
    return Config.load(configFile);
  }

  public static void findFreePortsAndWriteToConfig(Writer configWriter) throws IOException {
    try (ServerSocket nodeSocket = new ServerSocket(0); ServerSocket clientSocket = new ServerSocket(0)) {
      int nodePort = nodeSocket.getLocalPort();
      int clientPort = clientSocket.getLocalPort();
      configWriter.write("nodeport=" + nodePort + "\n");
      configWriter.write("clientport=" + clientPort + "\n");
    }
  }

  public static void writeServerCertToConfig(Writer configWriter, SelfSignedCertificate serverCert) throws IOException {
    configWriter.write("tlsservercert=\"" + serverCert.certificatePath() + "\"\n");
    configWriter.write("tlsserverkey=\"" + serverCert.privateKeyPath() + "\"\n");
  }

  public static void writeClientCertToConfig(Writer configWriter, SelfSignedCertificate serverCert) throws IOException {
    configWriter.write("tlsclientcert=\"" + serverCert.certificatePath() + "\"\n");
    configWriter.write("tlsclientkey=\"" + serverCert.privateKeyPath() + "\"\n");
  }

  public static int getFreePort() throws Exception {
    try (ServerSocket nodeSocket = new ServerSocket(0)) {
      return nodeSocket.getLocalPort();
    }
  }
}

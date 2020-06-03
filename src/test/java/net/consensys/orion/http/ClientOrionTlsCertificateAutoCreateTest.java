/*
 * Copyright 2020 ConsenSys AG.
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
package net.consensys.orion.http;

import static io.vertx.core.Vertx.vertx;
import static net.consensys.orion.TestUtils.generateAndLoadConfiguration;
import static net.consensys.orion.TestUtils.writeClientConnectionServerCertToConfig;
import static net.consensys.orion.TestUtils.writeServerCertToConfig;
import static org.apache.tuweni.net.tls.TLS.readPemFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import net.consensys.orion.cmd.Orion;
import net.consensys.orion.config.Config;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;

import io.vertx.core.Vertx;
import org.apache.tuweni.junit.TempDirectory;
import org.apache.tuweni.junit.TempDirectoryExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
public class ClientOrionTlsCertificateAutoCreateTest {
  private final static Vertx vertx = vertx();
  private Orion orion;
  private Config config;
  private static final String TRUST_MODE = "tofu";
  private final static String serverCert = "serverCert.pem";
  private final static String serverKey = "serverKey.key";
  private final static String clientConnectionServerCert = "clientConnectionServerCert.pem";
  private final static String clientConnectionServerKey = "clientConnectionServerKey.key";

  @BeforeEach
  void setUpConfig(@TempDirectory final Path tempDir) throws Exception {
    config = generateAndLoadConfiguration(tempDir, writer -> {
      writer.write("tlsservertrust='" + TRUST_MODE + "'\n");
      writer.write("clientconnectiontls='strict'\n");
      writer.write("clientconnectiontlsservertrust='" + TRUST_MODE + "'\n");

      writeServerCertToConfig(writer, tempDir.resolve(serverCert).toString(), tempDir.resolve(serverKey).toString());

      writeClientConnectionServerCertToConfig(
          writer,
          tempDir.resolve(clientConnectionServerCert).toString(),
          tempDir.resolve(clientConnectionServerKey).toString());
    });
  }

  @Test
  void clientConnectionTlsKeyCertPairCreatedIfNotExist() {
    final Path privateKeyPath = config.clientConnectionTlsServerKey();
    final Path certificatePath = config.clientConnectionTlsServerCert();

    assertThat(privateKeyPath).doesNotExist();
    assertThat(certificatePath).doesNotExist();

    try {
      // start Orion
      orion = new Orion(vertx);
      orion.run(config, false);

      // key/certificate created after Orion start and are in valid readable format.
      assertThat(privateKeyPath).exists();
      assertThat(certificatePath).exists();

      assertThatCode(() -> new PKCS8EncodedKeySpec(readPemFile(privateKeyPath))).doesNotThrowAnyException();

      assertThatCode(
          () -> CertificateFactory.getInstance("X.509").generateCertificate(
              new ByteArrayInputStream(Files.readAllBytes(certificatePath)))).doesNotThrowAnyException();
    } finally {
      if (orion != null) {
        orion.stop();
      }

      vertx.close();
    }
  }
}

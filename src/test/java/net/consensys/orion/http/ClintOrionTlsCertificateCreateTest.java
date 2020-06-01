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

import net.consensys.orion.cmd.Orion;
import net.consensys.orion.config.Config;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;

import io.vertx.core.Vertx;
import io.vertx.core.net.SelfSignedCertificate;
import org.apache.tuweni.junit.TempDirectory;
import org.apache.tuweni.junit.TempDirectoryExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
public class ClintOrionTlsCertificateCreateTest {
  private final static Vertx vertx = vertx();
  private Orion orion;
  private Config config;
  private static final String TRUST_MODE = "tofu";
  private final static String clientConnectionServerCert = "clientConnectionServerCert.pem";
  private final static String clientConnectionServerKey = "clientConnectionServerKey.key";

  @BeforeEach
  void setUp(@TempDirectory final Path tempDir) throws Exception {
    final SelfSignedCertificate serverCertificate = SelfSignedCertificate.create("localhost");

    config = generateAndLoadConfiguration(tempDir, writer -> {
      writer.write("tlsservertrust='" + TRUST_MODE + "'\n");
      writer.write("clientconnectiontls='strict'\n");
      writer.write("clientconnectiontlsservertrust='" + TRUST_MODE + "'\n");
      writeServerCertToConfig(writer, serverCertificate);
      writeClientConnectionServerCertToConfig(
          writer,
          tempDir.resolve(clientConnectionServerCert).toString(),
          tempDir.resolve(clientConnectionServerKey).toString());
    });

    orion = new Orion(vertx);
    orion.run(config, false);
  }

  @AfterEach
  void tearDown() {
    orion.stop();
    vertx.close();
  }

  @Test
  void clientConnectionTlsKeyCertificatePairCreated() {
    Assertions
        .assertThatCode(() -> new PKCS8EncodedKeySpec(readPemFile(config.clientConnectionTlsServerKey())))
        .doesNotThrowAnyException();

    Assertions
        .assertThatCode(
            () -> CertificateFactory.getInstance("X.509").generateCertificate(
                new ByteArrayInputStream(Files.readAllBytes(config.clientConnectionTlsServerCert()))))
        .doesNotThrowAnyException();
  }
}

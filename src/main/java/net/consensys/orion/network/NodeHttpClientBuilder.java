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

package net.consensys.orion.network;

import net.consensys.cava.net.tls.VertxTrustOptions;
import net.consensys.orion.config.Config;

import java.nio.file.Path;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.PemKeyCertOptions;

public class NodeHttpClientBuilder {

  private NodeHttpClientBuilder() {}

  public static HttpClient build(Vertx vertx, Config config, int clientTimeoutMs) {
    HttpClientOptions options =
        new HttpClientOptions().setConnectTimeout(clientTimeoutMs).setIdleTimeout(clientTimeoutMs);

    if ("strict".equals(config.tls())) {
      Path workDir = config.workDir();
      Path tlsClientCert = workDir.resolve(config.tlsClientCert());
      Path tlsClientKey = workDir.resolve(config.tlsClientKey());

      PemKeyCertOptions pemKeyCertOptions =
          new PemKeyCertOptions().setKeyPath(tlsClientKey.toString()).setCertPath(tlsClientCert.toString());
      for (Path chainCert : config.tlsClientChain()) {
        pemKeyCertOptions.addCertPath(chainCert.toAbsolutePath().toString());
      }

      options.setSsl(true);
      options.setPemKeyCertOptions(pemKeyCertOptions);

      Path knownServersFile = config.tlsKnownServers();
      String clientTrustMode = config.tlsClientTrust();
      switch (clientTrustMode) {
        case "whitelist":
          options.setTrustOptions(VertxTrustOptions.whitelistServers(knownServersFile, false));
          break;
        case "ca":
          // use default trust options
          break;
        case "ca-or-whitelist":
          options.setTrustOptions(VertxTrustOptions.whitelistServers(knownServersFile, true));
          break;
        case "tofu":
          options.setTrustOptions(VertxTrustOptions.trustServerOnFirstUse(knownServersFile, false));
          break;
        case "ca-or-tofu":
          options.setTrustOptions(VertxTrustOptions.trustServerOnFirstUse(knownServersFile, true));
          break;
        case "insecure-no-validation":
        case "insecure-record":
          options.setTrustOptions(VertxTrustOptions.recordServerFingerprints(knownServersFile, false));
          break;
        case "insecure-ca-or-record":
          options.setTrustOptions(VertxTrustOptions.recordServerFingerprints(knownServersFile, true));
          break;

        default:
          throw new UnsupportedOperationException(
              "\"" + clientTrustMode + "\" option for tlsclienttrust is not supported");
      }
    }
    return vertx.createHttpClient(options);
  }
}

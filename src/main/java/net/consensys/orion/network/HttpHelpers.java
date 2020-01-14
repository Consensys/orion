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
package net.consensys.orion.network;

import java.nio.file.Path;
import java.util.List;

import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.TrustOptions;
import org.apache.tuweni.net.tls.VertxTrustOptions;

public class HttpHelpers {

  public static TrustOptions createTrustOptions(final String trustMode, final Path knownConnectionFile) {
    switch (trustMode) {
      case "whitelist":
        return VertxTrustOptions.whitelistClients(knownConnectionFile, false);
      case "ca":
        return null;
      case "tofu":
      case "insecure-tofa":
        return VertxTrustOptions.trustClientOnFirstAccess(knownConnectionFile, false);
      case "insecure-no-validation":
      case "insecure-record":
        return VertxTrustOptions.recordClientFingerprints(knownConnectionFile, false);
      case "ca-or-tofu":
      case "insecure-ca-or-tofa":
        return VertxTrustOptions.trustClientOnFirstAccess(knownConnectionFile, true);
      case "ca-or-whitelist":
        return VertxTrustOptions.whitelistClients(knownConnectionFile, true);
      case "insecure-ca-or-record":
        return VertxTrustOptions.recordClientFingerprints(knownConnectionFile, true);
      default:
        throw new UnsupportedOperationException("\"" + trustMode + "\" option is not supported");
    }
  }

  public static PemTrustOptions createPemTrustOptions(final List<Path> certChain) {
    if (!certChain.isEmpty()) {
      final PemTrustOptions pemTrustOptions = new PemTrustOptions();
      for (final Path certPath : certChain) {
        pemTrustOptions.addCertPath(certPath.toAbsolutePath().toString());
      }
      return pemTrustOptions;
    }
    return null;
  }
}

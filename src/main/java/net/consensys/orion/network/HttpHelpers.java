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

import io.vertx.core.net.TCPSSLOptions;
import java.nio.file.Path;
import org.apache.tuweni.net.tls.VertxTrustOptions;

public class HttpHelpers {

  public static void createTrustOptions(final TCPSSLOptions optionsBase, final String trustMode, final Path knownConnectionFile) {
    switch (trustMode) {
      case "whitelist":
        optionsBase.setTrustOptions(VertxTrustOptions.whitelistClients(knownConnectionFile, false));
        break;
      case "ca":
        return;
      case "tofu":
      case "insecure-tofa":
        optionsBase.setTrustOptions(VertxTrustOptions.trustClientOnFirstAccess(knownConnectionFile, false));
        break;
      case "insecure-no-validation":
      case "insecure-record":
        optionsBase.setTrustOptions(VertxTrustOptions.recordClientFingerprints(knownConnectionFile, false));
        break;
      case "ca-or-tofu":
      case "insecure-ca-or-tofa":
        optionsBase.setTrustOptions(VertxTrustOptions.trustClientOnFirstAccess(knownConnectionFile, true));
        break;
      case "ca-or-whitelist":
        optionsBase.setTrustOptions(VertxTrustOptions.whitelistClients(knownConnectionFile, true));
        break;
      case "insecure-ca-or-record":
        optionsBase.setTrustOptions(VertxTrustOptions.recordClientFingerprints(knownConnectionFile, true));
        break;
      default:
        throw new UnsupportedOperationException("\"" + trustMode + "\" option is not supported");
    }
  }
}

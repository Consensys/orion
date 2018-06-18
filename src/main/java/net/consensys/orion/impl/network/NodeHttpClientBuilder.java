package net.consensys.orion.impl.network;

import net.consensys.cava.net.tls.VertxTrustOptions;
import net.consensys.orion.api.config.Config;

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
        pemKeyCertOptions.addCertPath(config.workDir().resolve(chainCert).toString());
      }

      options.setSsl(true);
      options.setPemKeyCertOptions(pemKeyCertOptions);

      Path knownServersFile = workDir.resolve(config.tlsKnownServers());
      String clientTrustMode = config.tlsClientTrust().toLowerCase();
      switch (clientTrustMode) {
        case "whitelist":
          options.setTrustOptions(VertxTrustOptions.whitelistServers(knownServersFile, false));
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
        case "record":
          options.setTrustOptions(VertxTrustOptions.recordServerFingerprints(knownServersFile, false));
          break;
        case "ca-or-record":
          options.setTrustOptions(VertxTrustOptions.recordServerFingerprints(knownServersFile, true));
          break;
        case "ca":
          // use default trust options
          break;
        default:
          throw new UnsupportedOperationException(
              "\"" + clientTrustMode + "\" option for tlsclienttrust is not supported");
      }
    }
    return vertx.createHttpClient(options);
  }
}

package net.consensys.orion.impl.network;

import net.consensys.orion.api.cmd.OrionStartException;
import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.network.HostAndFingerprintTrustManagerFactory;
import net.consensys.orion.api.network.HostFingerprintRepository;
import net.consensys.orion.api.network.TrustManagerFactoryWrapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

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
      options.setSsl(true);
      PemKeyCertOptions pemKeyCertOptions =
          new PemKeyCertOptions().setKeyPath(config.workDir().resolve(config.tlsClientKey()).toString()).setCertPath(
              config.workDir().resolve(config.tlsClientCert()).toString());
      for (Path chainCert : config.tlsClientChain()) {
        pemKeyCertOptions.addCertPath(config.workDir().resolve(chainCert).toString());
      }
      options.setPemKeyCertOptions(pemKeyCertOptions);
      Optional<Function<HostFingerprintRepository, HostAndFingerprintTrustManagerFactory>> tmfCreator =
          Optional.empty();
      if ("ca".equals(config.tlsClientTrust())) {
        options.setVerifyHost(false);
      } else if ("tofu".equals(config.tlsClientTrust())) {
        tmfCreator = Optional.of(HostAndFingerprintTrustManagerFactory::tofu);
      } else if ("whitelist".equals(config.tlsClientTrust())) {
        tmfCreator = Optional.of(HostAndFingerprintTrustManagerFactory::whitelist);
      } else if ("ca-or-tofu".equals(config.tlsClientTrust())) {
        options.setVerifyHost(false);
        tmfCreator = Optional.of(
            hostFingerprintRepository -> HostAndFingerprintTrustManagerFactory
                .caOrTofuDefaultJDKTruststore(hostFingerprintRepository, vertx));
      } else if ("insecure-no-validation".equals(config.tlsClientTrust())) {
        tmfCreator = Optional.of(HostAndFingerprintTrustManagerFactory::insecure);
      } else {
        throw new UnsupportedOperationException(config.tlsClientTrust() + " is not supported");
      }

      tmfCreator.ifPresent(tmf -> {
        try {
          HostFingerprintRepository hostFingerprintRepository =
              new HostFingerprintRepository(config.workDir().resolve(config.tlsKnownServers()));
          options.setTrustOptions(new TrustManagerFactoryWrapper(tmf.apply(hostFingerprintRepository)));
        } catch (IOException e) {
          throw new OrionStartException(
              "Could not read the contents of " + config.workDir().resolve(config.tlsKnownServers()),
              e);
        }
      });
    }
    return vertx.createHttpClient(options);
  }

}

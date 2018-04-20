package net.consensys.orion.impl.network;

import static net.consensys.orion.impl.http.server.HttpContentType.CBOR;

import net.consensys.orion.api.cmd.OrionStartException;
import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.network.HostAndFingerprintTrustManagerFactory;
import net.consensys.orion.api.network.HostFingerprintRepository;
import net.consensys.orion.api.network.TrustManagerFactoryWrapper;
import net.consensys.orion.impl.utils.Serializer;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.PemKeyCertOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NetworkDiscovery extends AbstractVerticle {
  private static final Logger log = LogManager.getLogger();

  public static final int HTTP_CLIENT_TIMEOUT_MS = 1500;
  private static final long REFRESH_DELAY_MS = 500;
  private static final long MAX_REFRESH_DELAY_MS = 60000;

  private Optional<HttpClient> httpClient = Optional.empty();

  private final ConcurrentNetworkNodes nodes;
  private final Map<String, Discoverer> discoverers;
  private final Config config;
  private final long refreshDelayMs;
  private final int clientTimeoutMs;

  public NetworkDiscovery(ConcurrentNetworkNodes nodes, Config config, long refreshDelayMs, int clientTimeoutMs) {
    this.nodes = nodes;
    this.discoverers = new HashMap<>();
    this.config = config;
    this.refreshDelayMs = refreshDelayMs;
    this.clientTimeoutMs = clientTimeoutMs;
  }

  public NetworkDiscovery(ConcurrentNetworkNodes nodes, Config config) {
    this(nodes, config, REFRESH_DELAY_MS, HTTP_CLIENT_TIMEOUT_MS);
  }

  @Override
  public void start() {
    HttpClientOptions options =
        new HttpClientOptions().setConnectTimeout(clientTimeoutMs).setIdleTimeout(clientTimeoutMs);
    if ("strict".equals(config.tls())) {
      options.setSsl(true);
      PemKeyCertOptions pemKeyCertOptions =
          new PemKeyCertOptions().setKeyPath(config.tlsClientKey().toString()).setCertPath(
              config.tlsClientCert().toString());
      for (Path chainCert : config.tlsClientChain()) {
        pemKeyCertOptions.addCertPath(chainCert.toString());
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
          HostFingerprintRepository hostFingerprintRepository = new HostFingerprintRepository(config.tlsKnownServers());
          options.setTrustOptions(new TrustManagerFactoryWrapper(tmf.apply(hostFingerprintRepository)));
        } catch (IOException e) {
          throw new OrionStartException("Could not read the contents of " + config.tlsKnownServers(), e);
        }
      });
    }

    this.httpClient = Optional.of(vertx.createHttpClient(options));
    updateDiscoverers();
  }

  @Override
  public void stop() {
    for (Discoverer discoverer : discoverers.values()) {
      discoverer.cancel();
    }
    httpClient.ifPresent(client -> {
      client.close();
      httpClient = Optional.empty();
    });
  }

  /**
   * Should be called from the same vertx event loop either when NetworkDiscovery is deployed (via start() method) or
   * when a merge occurs in one of the Discoverer (same event loop)
   */
  private void updateDiscoverers() {
    // for each peer that we know, we start a Discoverer (on timer)
    for (URL nodeUrl : nodes.nodeURLs()) {
      String urlString = nodeUrl.toString();
      if (!discoverers.containsKey(urlString)) {
        Discoverer d = new Discoverer(nodeUrl, refreshDelayMs);
        discoverers.put(urlString, d);
        d.engageNextTimerTick();
      }
    }
  }

  public Map<String, Discoverer> discoverers() {
    return new HashMap<>(discoverers);
  }

  /**
   * Discoverer handle() is fired by a timer
   *
   * <p>
   * Its job is to call /partyInfo periodically on a specified URL and merge results if needed in NetworkDiscovery state
   */
  class Discoverer implements Handler<Long> {
    private URL nodeUrl;
    long currentRefreshDelay;
    Instant lastUpdate = Instant.MIN;
    long attempts = 0;
    private long timerId;

    Discoverer(URL nodeUrl, long refreshDelayMs) {
      this.nodeUrl = nodeUrl;
      this.currentRefreshDelay = refreshDelayMs;
    }

    @Override
    public void handle(Long timerId) {
      // This is called on timer event, in the event loop of the Verticle (NetworkDiscovery)
      // we call /partyInfo API on the peer and update NetworkDiscovery state if needed

      log.trace("calling /partyinfo on {}", nodeUrl);
      attempts++;

      httpClient
          .orElseThrow(IllegalStateException::new)
          .post(nodeUrl.getPort(), nodeUrl.getHost(), "/partyinfo", resp -> {
            if (resp.statusCode() == 200) {
              lastUpdate = Instant.now();

              resp.bodyHandler(respBody -> {
                // deserialize response
                ConcurrentNetworkNodes partyInfoResponse =
                    Serializer.deserialize(CBOR, ConcurrentNetworkNodes.class, respBody.getBytes());
                if (nodes.merge(partyInfoResponse)) {
                  log.info("merged new nodes from {} discoverer", nodeUrl);
                }
                NetworkDiscovery.this.updateDiscoverers();
              });
            } else {
              log.debug("Response code: {}", resp.statusCode());
            }
            engageNextTimerTick();
          })
          .exceptionHandler(ex -> {
            log.error("calling partyInfo on {} failed {}", nodeUrl, ex.getMessage());
            engageNextTimerTick();
          })
          .putHeader("Content-Type", "application/cbor")
          .setTimeout(clientTimeoutMs)
          .end(Buffer.buffer(Serializer.serialize(CBOR, nodes)));
    }

    public void engageNextTimerTick() {
      currentRefreshDelay = (long) ((double) currentRefreshDelay * 2.0);
      currentRefreshDelay = Math.min(currentRefreshDelay, MAX_REFRESH_DELAY_MS);

      this.timerId = vertx.setTimer(currentRefreshDelay, this);
    }

    public void cancel() {
      vertx.cancelTimer(timerId);
    }
  }
}

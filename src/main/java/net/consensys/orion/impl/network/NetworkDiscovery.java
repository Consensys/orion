package net.consensys.orion.impl.network;

import static net.consensys.orion.impl.http.server.HttpContentType.CBOR;

import net.consensys.orion.impl.utils.Serializer;

import java.net.URL;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NetworkDiscovery extends AbstractVerticle {
  private static final Logger log = LogManager.getLogger();

  public static final int HTTP_CLIENT_TIMEOUT_MS = 1500;
  private static final long REFRESH_DELAY_MS = 500;
  private static final long MAX_REFRESH_DELAY_MS = 60000;

  private HttpClient httpClient;

  private final ConcurrentNetworkNodes nodes;
  private final Map<String, Discoverer> discoverers;

  public NetworkDiscovery(ConcurrentNetworkNodes nodes) {
    this.nodes = nodes;
    this.discoverers = new HashMap<>();
  }

  @Override
  public void start() {
    HttpClientOptions options =
        new HttpClientOptions().setConnectTimeout(HTTP_CLIENT_TIMEOUT_MS).setIdleTimeout(HTTP_CLIENT_TIMEOUT_MS);
    this.httpClient = vertx.createHttpClient(options);
    updateDiscoverers();
  }

  @Override
  public void stop() {
    for (Discoverer discoverer : discoverers.values()) {
      discoverer.cancel();
    }
    if (httpClient != null) {
      httpClient.close();
      httpClient = null;
    }
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
        Discoverer d = new Discoverer(nodeUrl);
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
    public long currentRefreshDelay = REFRESH_DELAY_MS;
    public Instant lastUpdate = Instant.MIN;
    public long attempts = 0;
    private long timerId;

    Discoverer(URL nodeUrl) {
      this.nodeUrl = nodeUrl;
    }

    @Override
    public void handle(Long timerId) {
      // This is called on timer event, in the event loop of the Verticle (NetworkDiscovery)
      // we call /partyInfo API on the peer and update NetworkDiscovery state if needed

      log.trace("calling partyInfo on {}", nodeUrl);
      attempts++;

      httpClient.post(nodeUrl.getPort(), nodeUrl.getHost(), "/partyinfo", resp -> {
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
        }
        engageNextTimerTick();
      }).exceptionHandler(ex -> {
        log.error("calling partyInfo on {} failed {}", nodeUrl, ex.getMessage());
        engageNextTimerTick();
      }).putHeader("Content-Type", "application/cbor").setTimeout(HTTP_CLIENT_TIMEOUT_MS).end(
          Buffer.buffer(Serializer.serialize(CBOR, nodes)));
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

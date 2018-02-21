package net.consensys.orion.impl.network;

import static net.consensys.orion.impl.http.server.HttpContentType.CBOR;

import net.consensys.orion.api.cmd.OrionRoutes;
import net.consensys.orion.api.network.NetworkNodes;
import net.consensys.orion.impl.utils.Serializer;

import java.net.URL;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NetworkDiscovery extends AbstractVerticle {
  private static final Logger log = LogManager.getLogger();

  public static final int HTTP_CLIENT_TIMEOUT_MS = 1500;
  private static final long REFRESH_DELAY_MS = 1000;
  private static final long MAX_REFRESH_DELAY_MS = 60000;

  private final OkHttpClient httpClient;

  private final NetworkNodes nodes;
  private final Map<URL, Discoverer> discoverers;
  private final Serializer serializer;

  public NetworkDiscovery(NetworkNodes nodes, Serializer serializer) {
    this.serializer = serializer;
    this.nodes = nodes;
    this.discoverers = new HashMap<>();
    this.httpClient =
        new OkHttpClient.Builder()
            .connectTimeout(HTTP_CLIENT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(HTTP_CLIENT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .build();
  }

  @Override
  public void start() {
    updateDiscoverers();
  }

  /**
   * Should be called from the same vertx event loop either when NetworkDiscovery is deployed (via
   * start() method) or when a merge occurs in one of the Discoverer (same event loop)
   */
  protected void updateDiscoverers() {
    // for each peer that we know, we start a Discoverer (on timer)
    for (URL nodeUrl : nodes.nodeURLs()) {
      if (!discoverers.containsKey(nodeUrl)) {
        Discoverer d = new Discoverer(nodeUrl);
        discoverers.put(nodeUrl, d);
        vertx.setTimer(REFRESH_DELAY_MS, d);
      }
    }
  }

  public Map<URL, Discoverer> discoverers() {
    return discoverers;
  }

  /**
   * Discoverer handle() is fired by a timer Its job is to call /partyInfo periodically on a
   * specified URL and merge results if needed in NetworkDiscovery state
   */
  class Discoverer implements Handler<Long> {
    private URL nodeUrl;
    public long currentRefreshDelay = REFRESH_DELAY_MS;
    public Instant lastUpdate = Instant.MIN;
    public long attempts = 0;

    Discoverer(URL nodeUrl) {
      this.nodeUrl = nodeUrl;
    }

    @Override
    public void handle(Long timerId) {
      // This is called on timer event, in the event loop of the Verticle (NetworkDiscovery)
      // we call /partyInfo API on the peer and update NetworkDiscovery state if needed
      vertx.executeBlocking(
          future -> {
            // executes outside the event loop (vertx worker pool).
            Optional<NetworkNodes> result = peerPartyInfo();
            future.complete(result);
          },
          res -> {
            // executes in the event loop.

            // let's re-fire the timer.
            currentRefreshDelay = (long) ((double) currentRefreshDelay * 2.0);
            if (currentRefreshDelay > MAX_REFRESH_DELAY_MS) {
              currentRefreshDelay = MAX_REFRESH_DELAY_MS;
            }
            vertx.setTimer(currentRefreshDelay, this);

            // process the result, and merge new nodes if any
            Optional<NetworkNodes> result = (Optional<NetworkNodes>) res.result();
            if (result.isPresent() && nodes.merge(result.get())) {
              // we merged something new, let's start discovery on this new nodes
              log.info("merged new nodes from {} discoverer", nodeUrl);
            }

            // each timer tick, we update our discoverers
            // merging nodes can occur in this timer or in /partyinfo handler
            NetworkDiscovery.this.updateDiscoverers();
          });
    }

    /** calls http endpoint PartyInfo; returns Optional.empty() if error. */
    private Optional<NetworkNodes> peerPartyInfo() {
      try {
        log.trace("calling partyInfo on {}", nodeUrl);
        attempts++;

        // prepare /partyinfo payload (our known peers)
        RequestBody partyInfoBody =
            RequestBody.create(
                MediaType.parse(CBOR.httpHeaderValue), serializer.serialize(CBOR, nodes));

        // call http endpoint
        Request request =
            new Request.Builder()
                .post(partyInfoBody)
                .url(nodeUrl + OrionRoutes.PARTYINFO.substring(1))
                .build();
        Response resp = httpClient.newCall(request).execute();

        if (resp.code() == 200) {
          lastUpdate = Instant.now();
          // deserialize response
          NetworkNodes partyInfoResponse =
              serializer.deserialize(CBOR, MemoryNetworkNodes.class, resp.body().bytes());

          return Optional.of(partyInfoResponse);
        }

      } catch (Exception io) {
        // timeout or connectivity issue / serialization issue
        log.error("calling partyInfo on {} failed {}", nodeUrl, io.getMessage());
      }
      return Optional.empty();
    }
  }
}

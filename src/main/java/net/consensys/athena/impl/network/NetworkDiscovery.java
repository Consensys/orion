package net.consensys.athena.impl.network;

import net.consensys.athena.api.cmd.AthenaRoutes;
import net.consensys.athena.api.network.NetworkNodes;
import net.consensys.athena.impl.http.server.HttpContentType;
import net.consensys.athena.impl.utils.Serializer;

import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NetworkDiscovery extends AbstractVerticle {
  private static final Logger log = LogManager.getLogger();

  private static long refreshDelayMs = 1000;
  private static long maxRefreshDelayMs = 60000;
  private static int connectionTimeoutMs = 1500;

  private final OkHttpClient httpClient;

  private final NetworkNodes nodes;
  private final Map<URL, Discoverer> discoverers;
  private final Serializer serializer;

  public NetworkDiscovery(NetworkNodes nodes, Serializer serializer) {
    this.serializer = serializer;
    this.nodes = nodes;
    this.discoverers = new ConcurrentHashMap<>();
    this.httpClient =
        new OkHttpClient.Builder()
            .connectTimeout(connectionTimeoutMs, TimeUnit.MILLISECONDS)
            .build();
  }

  @Override
  public void start() {
    updateDiscoverers();
  }

  // note; this should be called from the same vertx event loop
  protected void updateDiscoverers() {
    for (URL nodeUrl : nodes.nodeURLs()) {
      if (!discoverers.containsKey(nodeUrl)) {
        Discoverer d = new Discoverer(nodeUrl);
        discoverers.put(nodeUrl, d);
        vertx.setTimer(refreshDelayMs, d);
      }
    }
  }

  class Discoverer implements Handler<Long> {
    final URL nodeUrl;
    final Request request;
    long currentRefreshDelay = refreshDelayMs;

    Discoverer(URL nodeUrl) {
      this.nodeUrl = nodeUrl;
      this.request = new Request.Builder().get().url(nodeUrl + AthenaRoutes.PARTYINFO).build();
    }

    @Override
    public void handle(Long timerId) {
      vertx.executeBlocking(
          future -> {
            // executes in the worker pool.
            Optional<NetworkNodes> result = remotePartyInfo();
            future.complete(result);
          },
          res -> {
            // executes in the event loop.
            Optional<NetworkNodes> result = (Optional<NetworkNodes>) res.result();
            if (result.isPresent() && nodes.merge(result.get())) {
              // we merged something new, let's start discovery on this new nodes
              log.info("merged new nodes from {} discoverer", nodeUrl);
              NetworkDiscovery.this.updateDiscoverers();
            }

            // let's re-fire the timer.
            currentRefreshDelay = (long) ((double) currentRefreshDelay * 1.05);
            if (currentRefreshDelay > maxRefreshDelayMs) {
              currentRefreshDelay = maxRefreshDelayMs;
            }
            vertx.setTimer(currentRefreshDelay, this);
          });
    }

    // calls http endpoint PartyInfo; returns Optional.empty() if error.
    Optional<NetworkNodes> remotePartyInfo() {
      try {
        log.debug("calling partyInfo on {}", nodeUrl);
        // call http endpoint
        Response resp = httpClient.newCall(request).execute();

        // deserialize response
        NetworkNodes partyInfoResponse =
            serializer.deserialize(
                HttpContentType.CBOR, MemoryNetworkNodes.class, resp.body().bytes());

        // return
        return Optional.of(partyInfoResponse);
      } catch (Exception io) {
        log.error("calling partyInfo on {} failed {}", nodeUrl, io.getMessage());
        // timeout or connectivity issue / serialization issue
        return Optional.empty();
      }
    }
  }
}

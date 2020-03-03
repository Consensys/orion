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

import static net.consensys.orion.http.server.HttpContentType.CBOR;

import net.consensys.orion.config.Config;
import net.consensys.orion.utils.Serializer;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NetworkDiscovery extends AbstractVerticle {
  private static final Logger log = LogManager.getLogger();

  private static final int HTTP_CLIENT_TIMEOUT_MS = 1500;
  private static final long REFRESH_DELAY_MS = 500;
  private static final long MAX_REFRESH_DELAY_MS = 60000;

  private Optional<HttpClient> httpClient = Optional.empty();

  private final PersistentNetworkNodes nodes;
  private final Map<URI, Discoverer> discoverers;
  private final Config config;
  private final long refreshDelayMs;
  private final int clientTimeoutMs;

  public NetworkDiscovery(
      final PersistentNetworkNodes nodes,
      final Config config,
      final long refreshDelayMs,
      final int clientTimeoutMs) {
    this.nodes = nodes;
    this.discoverers = new HashMap<>();
    this.config = config;
    this.refreshDelayMs = refreshDelayMs;
    this.clientTimeoutMs = clientTimeoutMs;
  }

  public NetworkDiscovery(final PersistentNetworkNodes nodes, final Config config) {
    this(nodes, config, REFRESH_DELAY_MS, HTTP_CLIENT_TIMEOUT_MS);
  }

  @Override
  public void start() {
    this.httpClient = Optional.of(NodeHttpClientBuilder.build(vertx, config, clientTimeoutMs));
    updateDiscoverers();
  }

  @Override
  public void stop() {
    for (final Discoverer discoverer : discoverers.values()) {
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
    log.trace("Updating discoverers");
    // for each peer that we know, we start a Discoverer (on timer)
    for (final URI fixedNode : config.otherNodes()) {
      discoverers.computeIfAbsent(fixedNode, this::createDiscoverer);
    }

    for (final URI nodeUri : nodes.nodeURIs()) {
      discoverers.computeIfAbsent(nodeUri, this::createDiscoverer);
    }

  }

  public Map<URI, Discoverer> discoverers() {
    return new HashMap<>(discoverers);
  }

  private Discoverer createDiscoverer(URI uri) {
    log.trace("New discoverer for {}", uri);
    final Discoverer d = new Discoverer(uri, refreshDelayMs, uri.equals(nodes.uri()));
    d.engageNextTimerTick();
    return d;
  }

  /**
   * Discoverer handle() is fired by a timer
   *
   * <p>
   * Its job is to call /partyInfo periodically on a specified URL and merge results if needed in NetworkDiscovery state
   */
  class Discoverer implements Handler<Long> {
    private final URI nodeUrl;
    long currentRefreshDelay;
    Instant lastUpdate = Instant.MIN;
    long attempts = 0;
    private long timerId;
    private final boolean self;
    private final AtomicBoolean scheduled = new AtomicBoolean(false);

    Discoverer(final URI nodeUrl, final long refreshDelayMs, final boolean self) {
      this.nodeUrl = nodeUrl;
      this.currentRefreshDelay = refreshDelayMs;
      this.self = self;
    }

    @Override
    public void handle(final Long timerId) {
      // This is called on timer event, in the event loop of the Verticle (NetworkDiscovery)
      // we call /partyInfo API on the peer and update NetworkDiscovery state if needed
      scheduled.set(false);
      if (self) {
        log.trace("updating discoverers (local discovery)");
        updateDiscoverers();
        engageNextTimerTick();
      } else {
        log.trace("calling /partyinfo on {}", nodeUrl);
        attempts++;

        httpClient
            .orElseThrow(IllegalStateException::new)
            .post(nodeUrl.getPort(), nodeUrl.getHost(), "/partyinfo", resp -> {
              if (resp.statusCode() == 200) {
                lastUpdate = Instant.now();
                resp.bodyHandler(respBody -> {
                  // deserialize response
                  ReadOnlyNetworkNodes partyInfoResponse =
                      Serializer.deserialize(CBOR, ReadOnlyNetworkNodes.class, respBody.getBytes());
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
    }

    void engageNextTimerTick() {
      if (scheduled.compareAndSet(false, true)) {
        currentRefreshDelay = (long) ((double) currentRefreshDelay * 2.0);
        currentRefreshDelay = Math.min(currentRefreshDelay, MAX_REFRESH_DELAY_MS);
        this.timerId = vertx.setTimer(currentRefreshDelay, this);
      }
    }

    @Override
    public String toString() {
      return this.nodeUrl.toString();
    }

    void cancel() {
      vertx.cancelTimer(timerId);
    }
  }
}

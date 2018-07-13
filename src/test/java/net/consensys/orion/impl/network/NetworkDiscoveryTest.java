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

package net.consensys.orion.impl.network;

import static net.consensys.orion.impl.http.server.HttpContentType.CBOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.concurrent.CompletableAsyncCompletion;
import net.consensys.cava.crypto.sodium.Box;
import net.consensys.orion.api.config.Config;
import net.consensys.orion.impl.helpers.FakePeer;
import net.consensys.orion.impl.utils.Serializer;

import java.net.URL;
import java.time.Instant;

import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.SocketPolicy;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NetworkDiscoveryTest {
  private Vertx vertx;
  private ConcurrentNetworkNodes networkNodes;
  private Config config;

  @BeforeEach
  void setUp() throws Exception {
    vertx = Vertx.vertx();
    networkNodes = new ConcurrentNetworkNodes(new URL("http://localhost1234/"));
    config = Config.load("tls='off'");
  }

  @AfterEach
  void tearDown() throws Exception {
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    vertx.close(result -> {
      if (result.succeeded()) {
        completion.complete();
      } else {
        completion.completeExceptionally(result.cause());
      }
    });
    completion.join();
  }

  @Test
  void networkDiscoveryWithNoPeers() throws Exception {
    // add peers

    // start network discovery
    NetworkDiscovery networkDiscovery = new NetworkDiscovery(networkNodes, config);
    deployVerticle(networkDiscovery).join();

    assertEquals(0, networkDiscovery.discoverers().size());
  }

  @Test
  void networkDiscoveryWithUnresponsivePeer() throws Exception {
    // add peers
    FakePeer fakePeer =
        new FakePeer(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE), Box.KeyPair.random().publicKey());
    networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());

    // start network discovery
    NetworkDiscovery networkDiscovery = new NetworkDiscovery(networkNodes, config, 50, 100);
    deployVerticle(networkDiscovery).join();

    // assert the discoverer started
    assertEquals(1, networkDiscovery.discoverers().size());

    // ensure the discoverer match our peer URL
    NetworkDiscovery.Discoverer discoverer = networkDiscovery.discoverers().get(fakePeer.getURL().toString());
    assertNotNull(discoverer);

    Thread.sleep(3 * (discoverer.currentRefreshDelay + 200));

    // ensure we didn't do any update, and we tried at least 2 times
    assertEquals(Instant.MIN, discoverer.lastUpdate);
    assertTrue(discoverer.attempts >= 2);
  }

  @Test
  void networkDiscoveryWithMerge() throws Exception {
    // empty memory nodes, lets' say one peer is alone in his network
    byte[] unknownPeerNetworkNodes =
        Serializer.serialize(CBOR, new ConcurrentNetworkNodes(new URL("http://localhost/")));
    Buffer unknownPeerBody = new Buffer();
    unknownPeerBody.write(unknownPeerNetworkNodes);
    // create a peer that's not in our current network nodes
    FakePeer unknownPeer = new FakePeer(new MockResponse().setBody(unknownPeerBody), Box.KeyPair.random().publicKey());

    // create a peer that we know, and that knows the lonely unknown peer.
    ConcurrentNetworkNodes knownPeerNetworkNodes = new ConcurrentNetworkNodes(new URL("http://localhost/"));
    knownPeerNetworkNodes.addNode(unknownPeer.publicKey, unknownPeer.getURL());
    Buffer knownPeerBody = new Buffer();
    knownPeerBody.write(Serializer.serialize(CBOR, knownPeerNetworkNodes));
    FakePeer knownPeer = new FakePeer(new MockResponse().setBody(knownPeerBody), Box.KeyPair.random().publicKey());

    // we know this peer, add it to our network nodes
    networkNodes.addNode(knownPeer.publicKey, knownPeer.getURL());

    // start network discovery
    final Instant discoveryStart = Instant.now();
    NetworkDiscovery networkDiscovery = new NetworkDiscovery(networkNodes, config, 500, 500);
    deployVerticle(networkDiscovery).join();

    // assert the discoverer started, we should only have 1 discoverer for knownPeer
    assertEquals(1, networkDiscovery.discoverers().size());

    // ensure the discoverer match our peer URL
    NetworkDiscovery.Discoverer knownPeerDiscoverer = networkDiscovery.discoverers().get(knownPeer.getURL().toString());
    assertNotNull(knownPeerDiscoverer);

    Thread.sleep(3 * (knownPeerDiscoverer.currentRefreshDelay + 1000));

    // ensure knownPeer responded and that his party info was called at least twice
    assertTrue(
        knownPeerDiscoverer.lastUpdate.isAfter(discoveryStart),
        "Update last seen: " + knownPeerDiscoverer.lastUpdate);
    assertTrue(knownPeerDiscoverer.attempts >= 2);

    // ensure we now know unknownPeer
    assertEquals(2, networkNodes.nodePKs().size());
    assertEquals(unknownPeer.getURL(), networkNodes.nodePKs().get(unknownPeer.publicKey));

    // ensure unknown peer discoverer is set and being called
    NetworkDiscovery.Discoverer unknownPeerDiscoverer =
        networkDiscovery.discoverers().get(unknownPeer.getURL().toString());
    assertNotNull(unknownPeerDiscoverer);

    assertTrue(unknownPeerDiscoverer.lastUpdate.isAfter(discoveryStart));
    assertTrue(unknownPeerDiscoverer.attempts >= 1);
  }

  private AsyncCompletion deployVerticle(Verticle verticle) {
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    vertx.deployVerticle(verticle, result -> {
      if (result.succeeded()) {
        completion.complete();
      } else {
        completion.completeExceptionally(result.cause());
      }
    });
    return completion;
  }
}

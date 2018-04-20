package net.consensys.orion.impl.network;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.consensys.orion.impl.http.server.HttpContentType.CBOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.consensys.orion.api.config.Config;
import net.consensys.orion.impl.config.MemoryConfig;
import net.consensys.orion.impl.enclave.sodium.SodiumPublicKey;
import net.consensys.orion.impl.helpers.FakePeer;
import net.consensys.orion.impl.utils.Serializer;

import java.net.URL;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.SocketPolicy;
import okio.Buffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NetworkDiscoveryTest {
  private Vertx vertx;
  private ConcurrentNetworkNodes networkNodes;
  private Config config;

  @Before
  public void setUp() throws Exception {
    vertx = Vertx.vertx();
    networkNodes = new ConcurrentNetworkNodes(new URL("http://localhost1234/"));
    config = new MemoryConfig();
    ((MemoryConfig) config).setTls("off");
  }

  @After
  public void tearDown() throws Exception {
    CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();

    vertx.close(result -> {
      if (result.succeeded()) {
        resultFuture.complete(true);
      } else {
        resultFuture.completeExceptionally(result.cause());
      }
    });

    resultFuture.get();
  }

  @Test
  public void networkDiscoveryWithNoPeers() throws Exception {
    // add peers

    // start network discovery
    NetworkDiscovery networkDiscovery = new NetworkDiscovery(networkNodes, config);
    deployVerticle(networkDiscovery).get();

    assertEquals(0, networkDiscovery.discoverers().size());
  }

  @Test
  public void networkDiscoveryWithUnresponsivePeer() throws Exception {
    // add peers
    FakePeer fakePeer = new FakePeer(
        new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE),
        new SodiumPublicKey("pk1".getBytes(UTF_8)));
    networkNodes.addNode(fakePeer.publicKey, fakePeer.getURL());

    // start network discovery
    NetworkDiscovery networkDiscovery = new NetworkDiscovery(networkNodes, config, 50, 100);
    deployVerticle(networkDiscovery).get();

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
  public void networkDiscoveryWithMerge() throws Exception {
    // empty memory nodes, lets' say one peer is alone in his network
    byte[] unknownPeerNetworkNodes =
        Serializer.serialize(CBOR, new ConcurrentNetworkNodes(new URL("http://localhost/")));
    Buffer unknownPeerBody = new Buffer();
    unknownPeerBody.write(unknownPeerNetworkNodes);
    // create a peer that's not in our current network nodes
    FakePeer unknownPeer =
        new FakePeer(new MockResponse().setBody(unknownPeerBody), new SodiumPublicKey("unknown.pk1".getBytes(UTF_8)));

    // create a peer that we know, and that knows the lonely unknown peer.
    ConcurrentNetworkNodes knownPeerNetworkNodes = new ConcurrentNetworkNodes(new URL("http://localhost/"));
    knownPeerNetworkNodes.addNode(unknownPeer.publicKey, unknownPeer.getURL());
    Buffer knownPeerBody = new Buffer();
    knownPeerBody.write(Serializer.serialize(CBOR, knownPeerNetworkNodes));
    FakePeer knownPeer =
        new FakePeer(new MockResponse().setBody(knownPeerBody), new SodiumPublicKey("known.pk1".getBytes(UTF_8)));

    // we know this peer, add it to our network nodes
    networkNodes.addNode(knownPeer.publicKey, knownPeer.getURL());

    // start network discovery
    final Instant discoveryStart = Instant.now();
    NetworkDiscovery networkDiscovery = new NetworkDiscovery(networkNodes, config, 500, 500);
    deployVerticle(networkDiscovery).get();

    // assert the discoverer started, we should only have 1 discoverer for knownPeer
    assertEquals(1, networkDiscovery.discoverers().size());

    // ensure the discoverer match our peer URL
    NetworkDiscovery.Discoverer knownPeerDiscoverer = networkDiscovery.discoverers().get(knownPeer.getURL().toString());
    assertNotNull(knownPeerDiscoverer);

    Thread.sleep(3 * (knownPeerDiscoverer.currentRefreshDelay + 1000));

    // ensure knownPeer responded and that his party info was called at least twice
    assertTrue(
        "Update last seen: " + knownPeerDiscoverer.lastUpdate,
        knownPeerDiscoverer.lastUpdate.isAfter(discoveryStart));
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

  private Future<Boolean> deployVerticle(Verticle verticle) {
    CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();

    vertx.deployVerticle(verticle, result -> {
      if (result.succeeded()) {
        resultFuture.complete(true);
      } else {
        resultFuture.completeExceptionally(result.cause());
      }
    });

    return resultFuture;
  }
}

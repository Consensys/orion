package net.consensys.orion.acceptance.send.receive;

import static io.vertx.core.Vertx.vertx;
import static net.consensys.orion.acceptance.NodeUtils.assertTransaction;
import static net.consensys.orion.acceptance.NodeUtils.freePort;
import static net.consensys.orion.acceptance.NodeUtils.joinPathsAsTomlListEntry;
import static net.consensys.orion.acceptance.NodeUtils.sendTransaction;
import static net.consensys.orion.acceptance.NodeUtils.viewTransaction;
import static net.consensys.orion.impl.http.server.HttpContentType.CBOR;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.acceptance.EthClientStub;
import net.consensys.orion.acceptance.NodeUtils;
import net.consensys.orion.api.cmd.Orion;
import net.consensys.orion.api.config.Config;
import net.consensys.orion.impl.enclave.sodium.SodiumPublicKey;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.network.ConcurrentNetworkNodes;
import net.consensys.orion.impl.utils.Serializer;

import java.net.URL;
import java.nio.file.Path;
import java.security.PublicKey;
import java.util.concurrent.TimeUnit;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Runs up a two nodes that communicates with each other. */
@ExtendWith(TempDirectoryExtension.class)
class DualNodesSendReceiveTest {

  private static final String PK_1_B_64 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private static final String PK_2_B_64 = "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";

  private Config firstNodeConfig;
  private Config secondNodeConfig;
  private int firstNodeClientPort;
  private int secondNodeClientPort;
  private ConcurrentNetworkNodes networkNodes;

  private Orion firstOrionLauncher;
  private Orion secondOrionLauncher;
  private Vertx vertx;
  private HttpClient firstHttpClient;
  private HttpClient secondHttpClient;

  @BeforeEach
  void setUpDualNodes(@TempDirectory Path tempDir) throws Exception {
    int firstNodePort = freePort();
    firstNodeClientPort = freePort();
    int secondNodePort = freePort();
    secondNodeClientPort = freePort();
    String firstNodeBaseUrl = NodeUtils.url("127.0.0.1", firstNodePort);
    String secondNodeBaseUrl = NodeUtils.url("127.0.0.1", secondNodePort);

    firstNodeConfig = NodeUtils.nodeConfig(
        tempDir,
        firstNodeBaseUrl,
        firstNodePort,
        "127.0.0.1",
        NodeUtils.url("127.0.0.1", firstNodeClientPort),
        firstNodeClientPort,
        "127.0.0.1",
        "node1",
        secondNodeBaseUrl,
        joinPathsAsTomlListEntry("src/acceptance-test/resources/key1.pub"),
        joinPathsAsTomlListEntry("src/acceptance-test/resources/key1.key"),
        "off",
        "tofu",
        "tofu");
    secondNodeConfig = NodeUtils.nodeConfig(
        tempDir,
        secondNodeBaseUrl,
        secondNodePort,
        "127.0.0.1",
        NodeUtils.url("127.0.0.1", secondNodeClientPort),
        secondNodeClientPort,
        "127.0.0.1",
        "node2",
        firstNodeBaseUrl,
        joinPathsAsTomlListEntry("src/acceptance-test/resources/key2.pub"),
        joinPathsAsTomlListEntry("src/acceptance-test/resources/key2.key"),
        "off",
        "tofu",
        "tofu");
    vertx = vertx();
    firstOrionLauncher = NodeUtils.startOrion(firstNodeConfig);
    firstHttpClient = vertx.createHttpClient();
    secondOrionLauncher = NodeUtils.startOrion(secondNodeConfig);
    secondHttpClient = vertx.createHttpClient();
    networkNodes = new ConcurrentNetworkNodes(new URL(firstNodeBaseUrl));

    PublicKey pk1 = new SodiumPublicKey(PK_1_B_64);
    PublicKey pk2 = new SodiumPublicKey(PK_2_B_64);
    networkNodes.addNode(pk1, new URL(firstNodeBaseUrl));
    networkNodes.addNode(pk2, new URL(secondNodeBaseUrl));
    // prepare /partyinfo payload (our known peers)
    RequestBody partyInfoBody =
        RequestBody.create(MediaType.parse(CBOR.httpHeaderValue), Serializer.serialize(CBOR, networkNodes));
    // call http endpoint
    OkHttpClient httpClient = new OkHttpClient();

    Request request = new Request.Builder().post(partyInfoBody).url(firstNodeBaseUrl + "/partyinfo").build();

    // first /partyinfo call may just get the one node, so wait until we get at least 2 nodes
    await().atMost(5, TimeUnit.SECONDS).until(() -> getPartyInfoResponse(httpClient, request).nodeURLs().size() == 2);

  }

  private ConcurrentNetworkNodes getPartyInfoResponse(OkHttpClient httpClient, Request request) throws Exception {
    Response resp = httpClient.newCall(request).execute();
    assertEquals(200, resp.code());

    ConcurrentNetworkNodes partyInfoResponse =
        Serializer.deserialize(HttpContentType.CBOR, ConcurrentNetworkNodes.class, resp.body().bytes());
    return partyInfoResponse;
  }

  @AfterEach
  void tearDown() throws Exception {
    firstOrionLauncher.stop();
    secondOrionLauncher.stop();
    vertx.close();
  }

  @Test
  void receiverCanView() throws Exception {
    final EthClientStub firstNode = NodeUtils.client(firstNodeClientPort, firstHttpClient);
    final EthClientStub secondNode = NodeUtils.client(secondNodeClientPort, secondHttpClient);

    final String digest = sendTransaction(firstNode, PK_1_B_64, PK_2_B_64);
    final byte[] receivedPayload = viewTransaction(secondNode, PK_2_B_64, digest);

    assertTransaction(receivedPayload);
  }

  @Test
  void senderCanView() throws Exception {
    final EthClientStub firstNode = NodeUtils.client(firstNodeConfig.clientPort(), firstHttpClient);

    final String digest = sendTransaction(firstNode, PK_1_B_64, PK_2_B_64);
    final byte[] receivedPayload = viewTransaction(firstNode, PK_1_B_64, digest);

    assertTransaction(receivedPayload);
  }
}

package net.consensys.orion.acceptance.send.receive;

import static io.vertx.core.Vertx.vertx;
import static net.consensys.orion.acceptance.NodeUtils.assertTransaction;
import static net.consensys.orion.acceptance.NodeUtils.freePort;
import static net.consensys.orion.acceptance.NodeUtils.joinPathsAsTomlListEntry;
import static net.consensys.orion.acceptance.NodeUtils.sendTransaction;
import static net.consensys.orion.acceptance.NodeUtils.viewTransaction;

import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.orion.acceptance.EthClientStub;
import net.consensys.orion.acceptance.NodeUtils;
import net.consensys.orion.api.cmd.Orion;
import net.consensys.orion.api.config.Config;

import java.nio.file.Path;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
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
        "strict",
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
        "strict",
        "tofu",
        "tofu");
    vertx = vertx();
    firstOrionLauncher = NodeUtils.startOrion(firstNodeConfig);
    firstHttpClient = vertx.createHttpClient();
    secondOrionLauncher = NodeUtils.startOrion(secondNodeConfig);
    secondHttpClient = vertx.createHttpClient();
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
    NodeUtils.ensureNetworkDiscoveryOccurs();

    final String digest = sendTransaction(firstNode, PK_1_B_64, PK_2_B_64);
    final byte[] receivedPayload = viewTransaction(secondNode, PK_2_B_64, digest);

    assertTransaction(receivedPayload);
  }

  @Test
  void senderCanView() throws Exception {
    final EthClientStub firstNode = NodeUtils.client(firstNodeConfig.clientPort(), firstHttpClient);
    NodeUtils.ensureNetworkDiscoveryOccurs();

    final String digest = sendTransaction(firstNode, PK_1_B_64, PK_2_B_64);
    final byte[] receivedPayload = viewTransaction(firstNode, PK_1_B_64, digest);

    assertTransaction(receivedPayload);
  }
}

package net.consensys.orion.acceptance.send.receive;

import static io.vertx.core.Vertx.vertx;
import static java.nio.file.Files.createTempDirectory;
import static net.consensys.orion.acceptance.NodeUtils.assertTransaction;
import static net.consensys.orion.acceptance.NodeUtils.freePort;
import static net.consensys.orion.acceptance.NodeUtils.joinPathsAsTomlListEntry;
import static net.consensys.orion.acceptance.NodeUtils.sendTransaction;
import static net.consensys.orion.acceptance.NodeUtils.viewTransaction;
import static net.consensys.util.Files.deleteRecursively;

import net.consensys.orion.acceptance.EthClientStub;
import net.consensys.orion.acceptance.NodeUtils;
import net.consensys.orion.api.cmd.Orion;
import net.consensys.orion.api.config.Config;

import java.nio.file.Path;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Runs up a two nodes that communicates with each other. */
public class DualNodesSendReceiveTest {

  private static final String PK_1_B_64 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private static final String PK_2_B_64 = "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";

  private Path tempDir;
  private Config firstNodeConfig;
  private Config secondNodeConfig;
  private int firstNodeClientPort;
  private int secondNodeClientPort;

  private Orion firstOrionLauncher;
  private Orion secondOrionLauncher;
  private Vertx vertx;
  private HttpClient firstHttpClient;
  private HttpClient secondHttpClient;

  @Before
  public void setUpDualNodes() throws Exception {
    tempDir = createTempDirectory(DualNodesSendReceiveTest.class.getSimpleName() + "-data");
    int firstNodePort = freePort();
    firstNodeClientPort = freePort();
    int secondNodePort = freePort();
    secondNodeClientPort = freePort();
    String firstNodeBaseUrl = NodeUtils.url("127.0.0.1", firstNodePort);
    String secondNodeBaseUrl = NodeUtils.url("127.0.0.1", secondNodePort);

    firstNodeConfig = NodeUtils.nodeConfig(
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

  @After
  public void tearDown() throws Exception {
    firstOrionLauncher.stop();
    secondOrionLauncher.stop();
    vertx.close();
    deleteRecursively(tempDir);
  }

  @Test
  public void receiverCanView() throws Exception {
    final EthClientStub firstNode = NodeUtils.client(firstNodeClientPort, firstHttpClient);
    final EthClientStub secondNode = NodeUtils.client(secondNodeClientPort, secondHttpClient);
    NodeUtils.ensureNetworkDiscoveryOccurs();

    final String digest = sendTransaction(firstNode, PK_1_B_64, PK_2_B_64);
    final byte[] receivedPayload = viewTransaction(secondNode, PK_2_B_64, digest);

    assertTransaction(receivedPayload);
  }

  @Test
  public void senderCanView() throws Exception {
    final EthClientStub firstNode = NodeUtils.client(firstNodeConfig.clientPort(), firstHttpClient);
    NodeUtils.ensureNetworkDiscoveryOccurs();

    final String digest = sendTransaction(firstNode, PK_1_B_64, PK_2_B_64);
    final byte[] receivedPayload = viewTransaction(firstNode, PK_1_B_64, digest);

    assertTransaction(receivedPayload);
  }
}

package net.consensys.orion.acceptance.send.receive;

import static java.nio.file.Files.createTempDirectory;
import static net.consensys.util.Files.deleteRecursively;

import net.consensys.orion.acceptance.EthNodeStub;
import net.consensys.orion.api.cmd.Orion;
import net.consensys.orion.api.config.Config;

import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/** Runs up a two nodes that communicates with each other. */
public class DualNodesSendReceiveTest extends SendReceiveBase {

  private static final String PK_1_B_64 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private static final String PK_2_B_64 = "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";
  private static final String HOST_NAME = "127.0.0.1";

  private static Path tempDir;
  private static String firstNodeClientUrl;
  private static String secondNodeClientUrl;
  private static Config firstNodeConfig;
  private static Config secondNodeConfig;

  private Orion firstOrionLauncher;
  private Orion secondOrionLauncher;

  @BeforeClass
  public static void setUpDualNodes() throws Exception {
    tempDir = createTempDirectory(DualNodesSendReceiveTest.class.getSimpleName() + "-data");
    int firstNodePort = freePort();
    int firstNodeClientPort = freePort();
    int secondNodePort = freePort();
    int secondNodeClientPort = freePort();

    String firstNodeBaseUrl = url(HOST_NAME, firstNodePort);
    firstNodeClientUrl = url(HOST_NAME, firstNodeClientPort);
    String secondNodeBaseUrl = url(HOST_NAME, secondNodePort);
    secondNodeClientUrl = url(HOST_NAME, secondNodeClientPort);

    firstNodeConfig = nodeConfig(
        firstNodeBaseUrl,
        firstNodePort,
        "127.0.0.1",
        firstNodeClientUrl,
        firstNodeClientPort,
        "127.0.0.1",
        "node1",
        secondNodeBaseUrl,
        "src/acceptance-test/resources/key1.pub",
        "src/acceptance-test/resources/key1.key");
    secondNodeConfig = nodeConfig(
        secondNodeBaseUrl,
        secondNodePort,
        "127.0.0.1",
        secondNodeClientUrl,
        secondNodeClientPort,
        "127.0.0.1",
        "node2",
        firstNodeBaseUrl,
        "src/acceptance-test/resources/key2.pub",
        "src/acceptance-test/resources/key2.key");
  }

  @AfterClass
  public static void tearDownSingleNode() throws Exception {
    deleteRecursively(tempDir);
  }

  @Before
  public void setUp() throws ExecutionException, InterruptedException {
    firstOrionLauncher = startOrion(firstNodeConfig);
    secondOrionLauncher = startOrion(secondNodeConfig);
  }

  @After
  public void tearDown() {
    firstOrionLauncher.stop();
    secondOrionLauncher.stop();
  }

  @Test
  public void receiverCanView() throws Exception {
    final EthNodeStub firstNode = node(firstNodeClientUrl);
    final EthNodeStub secondNode = node(secondNodeClientUrl);
    ensureNetworkDiscoveryOccurs();

    final String digest = sendTransaction(firstNode, PK_1_B_64, PK_2_B_64);
    final byte[] receivedPayload = viewTransaction(secondNode, PK_2_B_64, digest);

    assertTransaction(receivedPayload);
  }

  @Test
  public void senderCanView() throws Exception {
    final EthNodeStub firstNode = node(firstNodeClientUrl);
    ensureNetworkDiscoveryOccurs();

    final String digest = sendTransaction(firstNode, PK_1_B_64, PK_2_B_64);
    final byte[] receivedPayload = viewTransaction(firstNode, PK_1_B_64, digest);

    assertTransaction(receivedPayload);
  }
}

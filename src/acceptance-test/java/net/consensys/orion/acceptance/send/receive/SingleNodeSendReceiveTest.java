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

/** Runs up a single node that communicates with itself. */
public class SingleNodeSendReceiveTest extends SendReceiveBase {

  private static final String PK_1_B_64 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private static final String PK_2_B_64 = "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";
  private static final String HOST_NAME = "127.0.0.1";

  private static Path tempDir;
  private static String clientUrl;
  private static Config config;

  private Orion orionLauncher;

  @BeforeClass
  public static void setUpSingleNode() throws Exception {
    tempDir = createTempDirectory(SingleNodeSendReceiveTest.class.getSimpleName() + "-data");
    final int nodePort = freePort();
    final int clientPort = freePort();

    String baseUrl = url(HOST_NAME, nodePort);
    clientUrl = url(HOST_NAME, clientPort);

    config = nodeConfig(
        baseUrl,
        nodePort,
        "127.0.0.1",
        clientUrl,
        clientPort,
        "127.0.0.1",
        "node1",
        baseUrl,
        "src/acceptance-test/resources/key1.pub\", \"src/acceptance-test/resources/key2.pub",
        "src/acceptance-test/resources/key1.key\", \"src/acceptance-test/resources/key2.key");
  }

  @AfterClass
  public static void tearDownSingleNode() throws Exception {
    deleteRecursively(tempDir);
  }

  @Before
  public void setUp() throws ExecutionException, InterruptedException {
    orionLauncher = startOrion(config);
  }

  @After
  public void tearDown() {
    orionLauncher.stop();
  }

  /** Sender and receiver use the same key. */
  @Test
  public void keyIdentity() throws Exception {
    final EthNodeStub ethNodeStub = node(clientUrl);
    ensureNetworkDiscoveryOccurs();

    final String digest = sendTransaction(ethNodeStub, PK_2_B_64, PK_2_B_64);
    final byte[] receivedPayload = viewTransaction(ethNodeStub, PK_2_B_64, digest);

    assertTransaction(receivedPayload);
  }

  /** Different keys for the sender and receiver. */
  @Test
  public void recieverCanView() throws Exception {
    final EthNodeStub ethNodeStub = node(clientUrl);
    ensureNetworkDiscoveryOccurs();

    final String digest = sendTransaction(ethNodeStub, PK_1_B_64, PK_2_B_64);
    final byte[] receivedPayload = viewTransaction(ethNodeStub, PK_2_B_64, digest);

    assertTransaction(receivedPayload);
  }

  /** The sender key can view their transaction when not in the recipient key list. */
  @Test
  public void senderCanView() throws Exception {
    final EthNodeStub ethNodeStub = node(clientUrl);
    ensureNetworkDiscoveryOccurs();

    final String digest = sendTransaction(ethNodeStub, PK_1_B_64, PK_2_B_64);
    final byte[] receivedPayload = viewTransaction(ethNodeStub, PK_1_B_64, digest);

    assertTransaction(receivedPayload);
  }

  private EthNodeStub node() {
    return node(clientUrl);
  }
}

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
import java.util.concurrent.ExecutionException;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/** Runs up a single client that communicates with itself. */
public class SingleNodeSendReceiveTest {

  private static final String PK_1_B_64 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private static final String PK_2_B_64 = "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";
  private static final String HOST_NAME = "127.0.0.1";

  private static Path tempDir;
  private static Config config;
  private static int clientPort;

  private Orion orionLauncher;
  private Vertx vertx;
  private HttpClient httpClient;

  @BeforeClass
  public static void setUpSingleNode() throws Exception {
    tempDir = createTempDirectory(SingleNodeSendReceiveTest.class.getSimpleName() + "-data");
    final int nodePort = freePort();
    clientPort = freePort();

    String baseUrl = NodeUtils.url(HOST_NAME, nodePort);
    String clientUrl = NodeUtils.url(HOST_NAME, clientPort);

    config = NodeUtils.nodeConfig(
        baseUrl,
        nodePort,
        "127.0.0.1",
        clientUrl,
        clientPort,
        "127.0.0.1",
        "node1",
        baseUrl,
        joinPathsAsTomlListEntry("src/acceptance-test/resources/key1.pub", "src/acceptance-test/resources/key2.pub"),
        joinPathsAsTomlListEntry("src/acceptance-test/resources/key1.key", "src/acceptance-test/resources/key2.key"),
        "off",
        "tofu",
        "tofu");
  }

  @AfterClass
  public static void tearDownSingleNode() throws Exception {
    deleteRecursively(tempDir);
  }

  @Before
  public void setUp() throws ExecutionException, InterruptedException {
    vertx = vertx();
    orionLauncher = NodeUtils.startOrion(config);
    httpClient = vertx.createHttpClient();
  }

  @After
  public void tearDown() {
    orionLauncher.stop();
    vertx.close();
  }

  /** Sender and receiver use the same key. */
  @Test
  public void keyIdentity() throws Exception {
    final EthClientStub ethClientStub = NodeUtils.client(clientPort, httpClient);
    NodeUtils.ensureNetworkDiscoveryOccurs();

    final String digest = sendTransaction(ethClientStub, PK_2_B_64, PK_2_B_64);
    final byte[] receivedPayload = viewTransaction(ethClientStub, PK_2_B_64, digest);

    assertTransaction(receivedPayload);
  }

  /** Different keys for the sender and receiver. */
  @Test
  public void receiverCanView() throws Exception {
    final EthClientStub ethClientStub = NodeUtils.client(clientPort, httpClient);
    NodeUtils.ensureNetworkDiscoveryOccurs();

    final String digest = sendTransaction(ethClientStub, PK_1_B_64, PK_2_B_64);
    final byte[] receivedPayload = viewTransaction(ethClientStub, PK_2_B_64, digest);

    assertTransaction(receivedPayload);
  }

  /** The sender key can view their transaction when not in the recipient key list. */
  @Test
  public void senderCanView() throws Exception {
    final EthClientStub ethClientStub = NodeUtils.client(clientPort, httpClient);
    NodeUtils.ensureNetworkDiscoveryOccurs();

    final String digest = sendTransaction(ethClientStub, PK_1_B_64, PK_2_B_64);
    final byte[] receivedPayload = viewTransaction(ethClientStub, PK_1_B_64, digest);

    assertTransaction(receivedPayload);
  }
}

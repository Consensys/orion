package net.consensys.orion.acceptance.send;

import static io.vertx.core.Vertx.vertx;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createTempDirectory;
import static net.consensys.util.Files.deleteRecursively;
import static org.junit.Assert.assertEquals;

import net.consensys.orion.acceptance.EthClientStub;
import net.consensys.orion.acceptance.NodeUtils;
import net.consensys.orion.api.cmd.Orion;
import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.exception.OrionErrorCode;

import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import junit.framework.AssertionFailedError;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/** Runs up a single client that communicates with itself. */
public class SingleNodeSendTest {

  private static final NodeUtils nodeUtils = new NodeUtils();
  private static final byte[] originalPayload = "another wonderful transaction".getBytes(UTF_8);

  private static final String PK_1_B_64 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private static final String PK_CORRUPTED = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGoAAA=";
  private static final String PK_MISSING_PEER = "A1aVtMxLCUlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";
  private static final String HOST_NAME = "127.0.0.1";

  private static Path tempDir;

  private static Config config;

  private Vertx vertx;
  private Orion orionLauncher;
  private HttpClient httpClient;

  @BeforeClass
  public static void setUpSingleNode() throws Exception {
    tempDir = createTempDirectory(SingleNodeSendTest.class.getSimpleName() + "-data");
    int nodePort = NodeUtils.freePort();
    int ethPort = NodeUtils.freePort();

    String baseUrl = NodeUtils.url(HOST_NAME, nodePort);
    String ethUrl = NodeUtils.url(HOST_NAME, ethPort);

    config = NodeUtils.nodeConfig(
        baseUrl,
        nodePort,
        "127.0.0.1",
        ethUrl,
        ethPort,
        "127.0.0.1",
        "node1",
        baseUrl,
        NodeUtils.joinPathsAsTomlListEntry(
            "src/acceptance-test/resources/key1.pub",
            "src/acceptance-test/resources/key2.pub"),
        NodeUtils.joinPathsAsTomlListEntry(
            "src/acceptance-test/resources/key1.key",
            "src/acceptance-test/resources/key2.key"),
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
    orionLauncher = NodeUtils.startOrion(config);
    vertx = vertx();
    httpClient = vertx.createHttpClient();
  }

  @After
  public void tearDown() {
    orionLauncher.stop();
    vertx.close();
  }

  /** Try sending to a peer that does not exist. */
  @Test
  public void missingPeer() {
    final EthClientStub orionNode = client();

    final String response = sendTransactionExpectingError(orionNode, PK_1_B_64, PK_MISSING_PEER);

    assertError(OrionErrorCode.NODE_MISSING_PEER_URL, response);
  }

  /** Try sending to a peer using a corrupted public key (wrong length). */
  @Test
  public void corruptedPublicKey() {
    final EthClientStub orionNode = client();

    final String response = sendTransactionExpectingError(orionNode, PK_1_B_64, PK_CORRUPTED);

    assertError(OrionErrorCode.ENCLAVE_DECODE_PUBLIC_KEY, response);
  }

  private EthClientStub client() {
    return NodeUtils.client(config.clientPort(), httpClient);
  }

  /** Verifies the Orion error JSON matches the desired Orion code. */
  private void assertError(OrionErrorCode expected, String actual) {
    assertEquals(String.format("{\"error\":\"%s\"}", expected.code()), actual);
  }

  private String sendTransactionExpectingError(EthClientStub sender, String senderKey, String... recipientsKey) {
    return sender.sendExpectingError(originalPayload, senderKey, recipientsKey).orElseThrow(AssertionFailedError::new);
  }
}

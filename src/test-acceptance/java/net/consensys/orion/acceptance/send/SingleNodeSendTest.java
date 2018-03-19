package net.consensys.orion.acceptance.send;

import static org.junit.Assert.assertEquals;

import net.consensys.orion.acceptance.EthNodeStub;
import net.consensys.orion.acceptance.NodeUtils;
import net.consensys.orion.api.cmd.Orion;
import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.exception.OrionErrorCode;

import java.io.File;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.concurrent.ExecutionException;

import junit.framework.AssertionFailedError;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/** Runs up a single node that communicates with itself. */
public class SingleNodeSendTest {

  private static final NodeUtils nodeUtils = new NodeUtils();
  private static final byte[] originalPayload = "another wonderful transaction".getBytes();

  private static final String PK_1_B_64 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private static final String PK_CORRUPTED = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGoAAA=";
  private static final String PK_MISSING_PEER = "A1aVtMxLCUlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";
  private static final String HOST_NAME = "127.0.0.1";

  private static String baseUrl;
  private static String ethUrl;

  private static Config config;
  private static int port;

  private Orion orionLauncher;

  @AfterClass
  public static void tearDownSingleNode() throws Exception {
    final Path rootPath = Paths.get("database");
    Files.walk(rootPath, FileVisitOption.FOLLOW_LINKS)
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
  }

  @BeforeClass
  public static void setUpSingleNode() throws Exception {
    port = nodeUtils.freePort();
    final int port = nodeUtils.freePort();
    final int ethPort = nodeUtils.freePort();

    baseUrl = nodeUtils.url(HOST_NAME, port);
    ethUrl = nodeUtils.url(HOST_NAME, ethPort);

    config =
        nodeUtils.nodeConfig(
            baseUrl,
            port,
            ethUrl,
            ethPort,
            "node1",
            baseUrl,
            "src/test-acceptance/resources/key1.pub\", \"src/test-acceptance/resources/key2.pub",
            "src/test-acceptance/resources/key1.key\", \"src/test-acceptance/resources/key2.key");
  }

  @Before
  public void setUp() throws ExecutionException, InterruptedException {
    orionLauncher = nodeUtils.startOrion(config);
  }

  @After
  public void tearDown() {
    orionLauncher.stop();
  }

  /** Try sending to a peer that does not exist. */
  @Test
  public void missingPeer() {
    final EthNodeStub orionNode = node();

    final String response = sendTransactionExpectingError(orionNode, PK_1_B_64, PK_MISSING_PEER);

    assertError(OrionErrorCode.NODE_MISSING_PEER_URL, response);
  }

  /** Try sending to a peer using a corrupted public key (wrong length). */
  @Test
  public void corruptedPublicKey() {
    final EthNodeStub orionNode = node();

    final String response = sendTransactionExpectingError(orionNode, PK_1_B_64, PK_CORRUPTED);

    assertError(OrionErrorCode.ENCLAVE_DECODE_PUBLIC_KEY, response);
  }

  private EthNodeStub node() {
    return nodeUtils.node(ethUrl);
  }

  /** Verifies the Orion error JSON matches the desired Orion code. */
  private void assertError(OrionErrorCode expected, String actual) {
    assertEquals(String.format("{\"error\":\"%s\"}", expected.code()), actual);
  }

  private String sendTransactionExpectingError(
      EthNodeStub sender, String senderKey, String... recipientsKey) {
    return sender
        .sendExpectingError(originalPayload, senderKey, recipientsKey)
        .orElseThrow(AssertionFailedError::new);
  }
}

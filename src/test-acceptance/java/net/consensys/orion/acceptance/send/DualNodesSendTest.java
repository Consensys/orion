package net.consensys.orion.acceptance.send;

import static org.junit.Assert.assertEquals;

import net.consensys.orion.acceptance.proxy.ReverseProxyServer;
import net.consensys.orion.acceptance.send.receive.SendReceiveUtil;
import net.consensys.orion.api.cmd.Orion;
import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.impl.http.OrionClient;

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
import org.junit.Ignore;
import org.junit.Test;

/**
 * Send communication between two nodes with a proxy server in between to inject erroneous
 * responses.
 */
public class DualNodesSendTest {

  private static final SendReceiveUtil utils = new SendReceiveUtil();
  private static final byte[] originalPayload = "a wonderful transaction".getBytes();

  private static final String PK_1_B_64 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private static final String PK_2_B_64 = "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";
  private static final String PK_MISSING_PEER = "A1aVtMxLCUlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";
  private static final String HOST_NAME = "127.0.0.1";

  private static String firstNodeBaseUrl;
  private static String secondNodeBaseUrl;
  private static Config firstNodeConfig;
  private static Config secondNodeConfig;

  private Orion firstOrionLauncher;
  private Orion secondOrionLauncher;
  private static String proxyBaseUrl;
  private static int proxyPort;

  private static int firstNodePort;
  private ReverseProxyServer proxyServer;

  @AfterClass
  public static void tearDownDualNodes() throws Exception {
    final Path rootPath = Paths.get("database");
    Files.walk(rootPath, FileVisitOption.FOLLOW_LINKS)
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
  }

  /** Proxt set up to proxy firstNode->secondNode communication. */
  @BeforeClass
  public static void setUpDualNodes() throws Exception {
    firstNodePort = utils.freePort();
    int secondNodePort = utils.freePort();
    proxyPort = utils.freePort();

    firstNodeBaseUrl = utils.url(HOST_NAME, firstNodePort);
    secondNodeBaseUrl = utils.url(HOST_NAME, secondNodePort);
    proxyBaseUrl = utils.url(HOST_NAME, proxyPort);

    firstNodeConfig =
        utils.nodeConfig(
            firstNodeBaseUrl,
            firstNodePort,
            "node1",
            proxyBaseUrl,
            "src/test-acceptance/resources/key1.pub",
            "src/test-acceptance/resources/key1.key");
    secondNodeConfig =
        utils.nodeConfig(
            secondNodeBaseUrl,
            secondNodePort,
            "node2",
            firstNodeBaseUrl,
            "src/test-acceptance/resources/key2.pub",
            "src/test-acceptance/resources/key2.key");
  }

  @Before
  public void setUp() throws ExecutionException, InterruptedException {
    firstOrionLauncher = utils.startOrion(firstNodeConfig);
    secondOrionLauncher = utils.startOrion(secondNodeConfig);
    proxyServer = new ReverseProxyServer(HOST_NAME, proxyPort, firstNodePort);
    proxyServer.start();
  }

  @After
  public void tearDown() {
    firstOrionLauncher.stop();
    secondOrionLauncher.stop();
    proxyServer.stop();
  }

  //TODO remove later
  /** Control test: Try sending to a peer that exists. */
  @Test
  public void sendToPeer() throws InterruptedException {
    final OrionClient firstNode = firstNode();
    ensureNetworkDiscoveryOccurs();

    final String digest = sendTransaction(firstNode, PK_1_B_64, PK_2_B_64);

    // Digest (encrypted message) changes every run, just verify it's the correct size
    assertEquals(44, digest.length());
  }

  //TODO remove later
  /** Control test: Try sending to a peer that does not exist. */
  @Test
  public void proxyMissingPeer() throws InterruptedException {
    final OrionClient firstNode = firstNode();
    ensureNetworkDiscoveryOccurs();

    final String response = sendTransactionExpectingError(firstNode, PK_1_B_64, PK_MISSING_PEER);

    assertError(OrionErrorCode.NODE_MISSING_PEER_URL, response);
  }

  /** Pushing to a peer fails from an IO Exception (socket timeout). */
  @Ignore
  @Test
  public void pushingToPeerTimeout() throws InterruptedException {
    final OrionClient firstNode = firstNode();
    ensureNetworkDiscoveryOccurs();

    //TODO inject error overrides
    proxyServer.socketProblem();

    final String response = sendTransactionExpectingError(firstNode, PK_1_B_64, PK_2_B_64);

    assertError(OrionErrorCode.NODE_PUSHING_TO_PEER, response);
  }

  //TODO aggregation into a utils of these

  /** Asserts the received payload matches that sent. */
  private void assertError(OrionErrorCode expected, String actual) {
    utils.assertError(expected, actual);
  }

  private OrionClient firstNode() {
    return utils.client(firstNodeBaseUrl);
  }

  private String sendTransactionExpectingError(
      OrionClient sender, String senderKey, String... recipientsKey) {
    return sender
        .sendExpectingError(originalPayload, senderKey, recipientsKey)
        .orElseThrow(AssertionFailedError::new);
  }

  protected void ensureNetworkDiscoveryOccurs() throws InterruptedException {
    // TODO there must be a better way then sleeping & hoping network discovery occurs
    Thread.sleep(2000);
  }

  protected String sendTransaction(OrionClient sender, String senderKey, String... recipientsKey) {
    return sender
        .send(originalPayload, senderKey, recipientsKey)
        .orElseThrow(AssertionFailedError::new);
  }
}

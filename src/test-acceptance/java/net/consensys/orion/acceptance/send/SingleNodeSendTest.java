package net.consensys.orion.acceptance.send;

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
import org.junit.Test;

/** Runs up a single node that communicates with itself. */
public class SingleNodeSendTest {

  private static final SendReceiveUtil utils = new SendReceiveUtil();
  private static final byte[] originalPayload = "a wonderful transaction".getBytes();

  private static final String PK_1_B_64 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private static final String PK_CORRUPTED = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGoAAA=";
  private static final String PK_MISSING_PEER = "A1aVtMxLCUlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";
  private static final String HOST_NAME = "127.0.0.1";

  private static String baseUrl;
  private static String proxyUrl;
  private static Config config;
  private static int port;
  private static int proxyPort;

  private ReverseProxyServer proxyServer;
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

    port = utils.freePort();
    proxyPort = utils.freePort();

    proxyUrl = utils.url(HOST_NAME, proxyPort);
    baseUrl = utils.url(HOST_NAME, port);

    config =
        utils.nodeConfig(
            baseUrl,
            port,
            "node1",
            baseUrl,
            "src/test-acceptance/resources/key1.pub\", \"src/test-acceptance/resources/key2.pub",
            "src/test-acceptance/resources/key1.key\", \"src/test-acceptance/resources/key2.key");
  }

  @Before
  public void setUp() throws ExecutionException, InterruptedException {
    orionLauncher = utils.startOrion(config);
    proxyServer = new ReverseProxyServer(HOST_NAME, proxyPort, port);
    proxyServer.start();
  }

  @After
  public void tearDown() {
    orionLauncher.stop();
    proxyServer.stop();
    ;
  }

  /** Try sending to a peer that does not exist. */
  @Test
  public void missingPeer() {
    final OrionClient orionClient = client();

    final String response = sendTransactionExpectingError(orionClient, PK_1_B_64, PK_MISSING_PEER);

    assertError(OrionErrorCode.NODE_MISSING_PEER_URL, response);
  }

  /** Try sending to a peer using a corrupted public key (wrong length). */
  @Test
  public void corruptedPublicKey() {
    final OrionClient orionClient = client();

    final String response = sendTransactionExpectingError(orionClient, PK_1_B_64, PK_CORRUPTED);

    assertError(OrionErrorCode.ENCLAVE_DECODE_PUBLIC_KEY, response);
  }

  /** Try sending to a peer that does not exist. */
  @Test
  public void proxyMissingPeer() {
    //TODO checking the Reverse proxy works normally - next to inject error overrides
    final OrionClient orionClient = proxy();

    final String response = sendTransactionExpectingError(orionClient, PK_1_B_64, PK_MISSING_PEER);

    assertError(OrionErrorCode.NODE_MISSING_PEER_URL, response);
  }

  private OrionClient client() {
    return utils.client(baseUrl);
  }

  private OrionClient proxy() {
    return new OrionClient(proxyUrl);
  }

  /** Asserts the received payload matches that sent. */
  private void assertError(OrionErrorCode expected, String actual) {
    utils.assertError(expected, actual);
  }

  private String sendTransactionExpectingError(
      OrionClient sender, String senderKey, String... recipientsKey) {
    return sender
        .sendExpectingError(originalPayload, senderKey, recipientsKey)
        .orElseThrow(AssertionFailedError::new);
  }
}

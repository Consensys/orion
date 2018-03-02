package net.consensys.orion.acceptance.send.receive;

import net.consensys.orion.api.cmd.Orion;
import net.consensys.orion.api.config.Config;
import net.consensys.orion.impl.http.OrionClient;

import java.io.File;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
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

  private static String firstNodeBaseUrl;
  private static String secondNodeBaseUrl;
  private static Config firstNodeConfig;
  private static Config secondNodeConfig;

  private Orion firstOrionLauncher;
  private Orion secondOrionLauncher;

  @AfterClass
  public static void tearDownDualNodes() throws Exception {
    final Path rootPath = Paths.get("database");
    Files.walk(rootPath, FileVisitOption.FOLLOW_LINKS)
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
  }

  @BeforeClass
  public static void setUpDualNodes() throws Exception {
    int firstNodePort = utils().freePort();
    int secondNodePort = utils().freePort();

    firstNodeBaseUrl = utils().url(HOST_NAME, firstNodePort);
    secondNodeBaseUrl = utils().url(HOST_NAME, secondNodePort);

    firstNodeConfig =
        utils()
            .nodeConfig(
                firstNodeBaseUrl,
                firstNodePort,
                "node1",
                secondNodeBaseUrl,
                "src/test-acceptance/resources/key1.pub",
                "src/test-acceptance/resources/key1.key");
    secondNodeConfig =
        utils()
            .nodeConfig(
                secondNodeBaseUrl,
                secondNodePort,
                "node2",
                firstNodeBaseUrl,
                "src/test-acceptance/resources/key2.pub",
                "src/test-acceptance/resources/key2.key");
  }

  @Before
  public void setUp() throws ExecutionException, InterruptedException {
    firstOrionLauncher = utils().startOrion(firstNodeConfig);
    secondOrionLauncher = utils().startOrion(secondNodeConfig);
  }

  @After
  public void tearDown() {
    firstOrionLauncher.stop();
    secondOrionLauncher.stop();
  }

  @Test
  public void receiverCanView() throws Exception {
    final OrionClient firstNode = firstClient();
    final OrionClient secondNode = secondClient();
    ensureNetworkDiscoveryOccurs();

    final String digest = sendTransaction(firstNode, PK_1_B_64, PK_2_B_64);
    final byte[] receivedPayload = viewTransaction(secondNode, PK_2_B_64, digest);

    assertTransaction(receivedPayload);
  }

  @Test
  public void senderCanView() throws Exception {
    final OrionClient firstNode = firstClient();
    ensureNetworkDiscoveryOccurs();

    final String digest = sendTransaction(firstNode, PK_1_B_64, PK_2_B_64);
    final byte[] receivedPayload = viewTransaction(firstNode, PK_1_B_64, digest);

    assertTransaction(receivedPayload);
  }

  private OrionClient firstClient() {
    return utils().client(firstNodeBaseUrl);
  }

  private OrionClient secondClient() {
    return utils().client(secondNodeBaseUrl);
  }
}

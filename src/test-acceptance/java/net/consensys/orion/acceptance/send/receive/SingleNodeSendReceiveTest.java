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

/** Runs up a single node that communicates with itself. */
public class SingleNodeSendReceiveTest extends SendReceiveBase {

  private static final String PK_1_B_64 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private static final String PK_2_B_64 = "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";
  private static final String HOST_NAME = "127.0.0.1";

  private static String baseUrl;

  private static Config config;

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
    int port = utils().freePort();

    baseUrl = utils().url(HOST_NAME, port);

    config =
        utils()
            .nodeConfig(
                baseUrl,
                port,
                "node1",
                baseUrl,
                "src/test-acceptance/resources/key1.pub\", \"src/test-acceptance/resources/key2.pub",
                "src/test-acceptance/resources/key1.key\", \"src/test-acceptance/resources/key2.key");
  }

  @Before
  public void setUp() throws ExecutionException, InterruptedException {
    orionLauncher = utils().startOrion(config);
  }

  @After
  public void tearDown() {
    orionLauncher.stop();
  }

  /** Sender and receiver use the same key. */
  @Test
  public void keyIdentity() throws Exception {
    final OrionClient orionClient = client();
    ensureNetworkDiscoveryOccurs();

    final String digest = sendTransaction(orionClient, PK_2_B_64, PK_2_B_64);
    final byte[] receivedPayload = viewTransaction(orionClient, PK_2_B_64, digest);

    assertTransaction(receivedPayload);
  }

  /** Different keys for the sender and receiver. */
  @Test
  public void recieverCanView() throws Exception {
    final OrionClient orionClient = client();
    ensureNetworkDiscoveryOccurs();

    final String digest = sendTransaction(orionClient, PK_1_B_64, PK_2_B_64);
    final byte[] receivedPayload = viewTransaction(orionClient, PK_2_B_64, digest);

    assertTransaction(receivedPayload);
  }

  /** The sender key can view their transaction when not in the recipient key list. */
  @Test
  public void senderCanView() throws Exception {
    final OrionClient orionClient = client();
    ensureNetworkDiscoveryOccurs();

    final String digest = sendTransaction(orionClient, PK_1_B_64, PK_2_B_64);
    final byte[] receivedPayload = viewTransaction(orionClient, PK_1_B_64, digest);

    assertTransaction(receivedPayload);
  }

  private OrionClient client() {
    return utils().client(baseUrl);
  }
}

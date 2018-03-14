package net.consensys.orion.acceptance.send;

import net.consensys.orion.acceptance.send.receive.SendReceiveBase;
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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/** Runs up a single node that communicates with itself. */
public class SingleNodeSendTest extends SendReceiveBase {

  private static final String PK_1_B_64 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private static final String PK_MISSING_PEER = "A1aVtMxLCUlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";
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

  /** Try sending to a peer that does not exist. */
  @Test
  public void missingPeer() {
    final OrionClient orionClient = client();

    final String response = sendTransactionExpectingError(orionClient, PK_1_B_64, PK_MISSING_PEER);

    assertError(OrionErrorCode.NODE_MISSING_PEER_URL, response);
  }

  //TODO wrong pk size key (currently unmapped error)

  private OrionClient client() {
    return utils().client(baseUrl);
  }
}

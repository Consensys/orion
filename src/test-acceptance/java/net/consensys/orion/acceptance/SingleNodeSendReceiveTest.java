package net.consensys.orion.acceptance;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import net.consensys.orion.api.cmd.Orion;
import net.consensys.orion.api.config.Config;
import net.consensys.orion.impl.config.TomlConfigBuilder;
import net.consensys.orion.impl.http.OrionClient;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import junit.framework.AssertionFailedError;
import okhttp3.HttpUrl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/** Runs up a single node that communicates with itself. */
public class SingleNodeSendReceiveTest {

  private static final String PK_1_B_64 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private static final String PK_2_B_64 = "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";
  private static final String HOST_NAME = "127.0.0.1";

  private static final byte[] originalPayload = "a wonderful transaction".getBytes();

  private static String singleNodeBaseUrl;

  private static Config configSingleNode;

  @AfterClass
  public static void tearDown() throws Exception {

    Path rootPath = Paths.get("database");
    Files.walk(rootPath, FileVisitOption.FOLLOW_LINKS)
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
  }

  @BeforeClass
  public static void setUp() throws Exception {
    // generate base urls
    int singleNodePort = getFreePort();

    singleNodeBaseUrl =
        new HttpUrl.Builder()
            .scheme("http")
            .host(HOST_NAME)
            .port(singleNodePort)
            .build()
            .toString();

    // Single node config
    String confString =
        new StringBuilder()
            .append("url = \"")
            .append(singleNodeBaseUrl)
            .append("\"\nport = ")
            .append(singleNodePort)
            .append("\nstorage = \"leveldb:database/node0\"")
            .append("\nothernodes = [\"")
            .append(singleNodeBaseUrl)
            .append(
                "\"]\n"
                    + "publickeys = [\"src/test-acceptance/resources/key1.pub\", \"src/test-acceptance/resources/key2.pub\"]\n"
                    + "privatekeys = [\"src/test-acceptance/resources/key1.key\", \"src/test-acceptance/resources/key2.key\"]")
            .toString();

    configSingleNode =
        new TomlConfigBuilder()
            .build(new ByteArrayInputStream(confString.getBytes(StandardCharsets.UTF_8.name())));
  }

  private static int getFreePort() throws Exception {

    ServerSocket socket = new ServerSocket(0);
    int toReturn = socket.getLocalPort();
    socket.close();
    return toReturn;
  }

  /** Sender and receiver use the same key. */
  @Test
  public void keyIdentity() throws Exception {

    // setup a single node with 2 public keys
    Orion orion = new Orion();
    orion.run(configSingleNode);
    OrionClient orionClient = new OrionClient(singleNodeBaseUrl);

    // ensure the node is awake
    assertTrue(orionClient.upCheck());

    // send something to the node (from pk2 to pk2)
    String digest =
        orionClient
            .send(originalPayload, PK_2_B_64, new String[] {PK_2_B_64})
            .orElseThrow(AssertionFailedError::new);

    // call receive on the node
    byte[] receivedPayload =
        orionClient.receive(digest, PK_2_B_64).orElseThrow(AssertionFailedError::new);

    // ensure we retrieved what we originally sent.
    assertArrayEquals(originalPayload, receivedPayload);

    // stop
    orion.stop();
  }

  /** Different keys for the sender and receiver. */
  @Test
  public void recieverCanView() throws Exception {

    // setup a single node with 2 public keys
    Orion orion = new Orion();
    orion.run(configSingleNode);
    OrionClient orionClient = new OrionClient(singleNodeBaseUrl);

    // ensure the node is awake
    assertTrue(orionClient.upCheck());

    // send something to the node (from pk1 to pk2)
    String digest =
        orionClient
            .send(originalPayload, PK_1_B_64, new String[] {PK_2_B_64})
            .orElseThrow(AssertionFailedError::new);

    // call receive on the node
    byte[] receivedPayload =
        orionClient.receive(digest, PK_2_B_64).orElseThrow(AssertionFailedError::new);
    assertArrayEquals(originalPayload, receivedPayload);

    // stop
    orion.stop();
  }

  /** The sender key can view their transaction when not in the recipient key list. */
  @Test
  public void senderCanView() throws Exception {

    // setup a single node with 2 public keys
    Orion orion = new Orion();
    orion.run(configSingleNode);
    OrionClient orionClient = new OrionClient(singleNodeBaseUrl);

    // ensure the node is awake
    assertTrue(orionClient.upCheck());

    // send something to the node (from pk1 to pk2)
    String digest =
        orionClient
            .send(originalPayload, PK_1_B_64, new String[] {PK_2_B_64})
            .orElseThrow(AssertionFailedError::new);

    // call receive on the node
    byte[] senderReceivedPayload =
        orionClient.receive(digest, PK_1_B_64).orElseThrow(AssertionFailedError::new);
    assertArrayEquals(originalPayload, senderReceivedPayload);

    // stop
    orion.stop();
  }
}

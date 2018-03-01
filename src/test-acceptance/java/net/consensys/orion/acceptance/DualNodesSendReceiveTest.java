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

/** Runs up a two nodes that communicates with each other. */
public class DualNodesSendReceiveTest {

  private static final String PK_1_B_64 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private static final String PK_2_B_64 = "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";
  private static final String HOST_NAME = "127.0.0.1";
  private static final byte[] originalPayload = "a wonderful transaction".getBytes();

  private static String node1BaseUrl;
  private static String node2BaseUrl;
  private static Config configNode1;
  private static Config configNode2;

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
    int node1Port = getFreePort();
    int node2Port = getFreePort();

    node1BaseUrl =
        new HttpUrl.Builder().scheme("http").host(HOST_NAME).port(node1Port).build().toString();
    node2BaseUrl =
        new HttpUrl.Builder().scheme("http").host(HOST_NAME).port(node2Port).build().toString();

    // node1 config
    String confString =
        new StringBuilder()
            .append("url = \"")
            .append(node1BaseUrl)
            .append("\"\nport = ")
            .append(node1Port)
            .append("\nstorage = \"leveldb:database/node1\"")
            .append("\nothernodes = [\"")
            .append(node2BaseUrl)
            .append(
                "\"]\n"
                    + "publickeys = [\"src/test-acceptance/resources/key1.pub\"]\n"
                    + "privatekeys = [\"src/test-acceptance/resources/key1.key\"]")
            .toString();

    configNode1 =
        new TomlConfigBuilder()
            .build(new ByteArrayInputStream(confString.getBytes(StandardCharsets.UTF_8.name())));

    // node2 config
    confString =
        new StringBuilder()
            .append("url = \"")
            .append(node2BaseUrl)
            .append("\"\nport = ")
            .append(node2Port)
            .append("\nstorage = \"leveldb:database/node2\"")
            .append("\nothernodes = [\"")
            .append(node1BaseUrl)
            .append(
                "\"]\n"
                    + "publickeys = [\"src/test-acceptance/resources/key2.pub\"]\n"
                    + "privatekeys = [\"src/test-acceptance/resources/key2.key\"]")
            .toString();

    configNode2 =
        new TomlConfigBuilder()
            .build(new ByteArrayInputStream(confString.getBytes(StandardCharsets.UTF_8.name())));
  }

  private static int getFreePort() throws Exception {

    ServerSocket socket = new ServerSocket(0);
    int toReturn = socket.getLocalPort();
    socket.close();
    return toReturn;
  }

  @Test
  public void receiverCanView() throws Exception {

    // setup our 2 nodes
    Orion orionLauncher1 = new Orion();
    orionLauncher1.run(configNode1);
    OrionClient node1 = new OrionClient(node1BaseUrl);
    assertTrue(node1.upCheck());

    Orion orionLauncher2 = new Orion();
    orionLauncher2.run(configNode2);
    OrionClient node2 = new OrionClient(node2BaseUrl);
    assertTrue(node2.upCheck());

    // ensure network discovery ran on node 1
    Thread.sleep(1000);

    // send a transaction from node1 to node2
    String digest =
        node1
            .send(originalPayload, PK_1_B_64, new String[] {PK_2_B_64})
            .orElseThrow(AssertionFailedError::new);

    // call receive on the node 2
    byte[] receivedPayload =
        node2.receive(digest, PK_2_B_64).orElseThrow(AssertionFailedError::new);

    // ensure we retrieved what we originally sent.
    assertArrayEquals(originalPayload, receivedPayload);

    // stop
    orionLauncher1.stop();
    orionLauncher2.stop();
  }

  @Test
  public void senderCanView() throws Exception {

    // setup our 2 nodes
    Orion orionLauncher1 = new Orion();
    orionLauncher1.run(configNode1);
    OrionClient node1 = new OrionClient(node1BaseUrl);
    assertTrue(node1.upCheck());

    Orion orionLauncher2 = new Orion();
    orionLauncher2.run(configNode2);
    OrionClient node2 = new OrionClient(node2BaseUrl);
    assertTrue(node2.upCheck());

    // ensure network discovery ran on node 1
    Thread.sleep(1000);

    // send a transaction from node1 to node2
    String digest =
        node1
            .send(originalPayload, PK_1_B_64, new String[] {PK_2_B_64})
            .orElseThrow(AssertionFailedError::new);

    // call receive on the node 1
    byte[] receivedPayload =
        node1.receive(digest, PK_1_B_64).orElseThrow(AssertionFailedError::new);

    // ensure we retrieved what we originally sent.
    assertArrayEquals(originalPayload, receivedPayload);

    // stop
    orionLauncher1.stop();
    orionLauncher2.stop();
  }
}

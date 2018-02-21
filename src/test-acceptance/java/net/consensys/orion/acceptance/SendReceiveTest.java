package net.consensys.orion.acceptance;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import net.consensys.orion.api.cmd.Athena;
import net.consensys.orion.api.config.Config;
import net.consensys.orion.impl.config.TomlConfigBuilder;
import net.consensys.orion.impl.http.AthenaClient;

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

public class SendReceiveTest {

  private static final byte[] originalPayload = "a wonderful transaction".getBytes();

  private static String singleNodeBaseUrl;
  private static String node1BaseUrl;
  private static String node2BaseUrl;

  private static Config configSingleNode;
  private static Config configNode1;
  private static Config configNode2;

  private static final String PK_1_B_64 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private static final String PK_2_B_64 = "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";

  private static final String HOST_NAME = "127.0.0.1";

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
    int singleNodePort = getFreePort();
    node1BaseUrl =
        new HttpUrl.Builder().scheme("http").host(HOST_NAME).port(node1Port).build().toString();
    node2BaseUrl =
        new HttpUrl.Builder().scheme("http").host(HOST_NAME).port(node2Port).build().toString();
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

    // node1 config
    confString =
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
  public void testSingleNode() throws Exception {
    // setup a single node with 2 public keys
    Athena athena = new Athena();
    athena.run(configSingleNode);
    AthenaClient athenaClient = new AthenaClient(singleNodeBaseUrl);

    // ensure the node is awake
    assertTrue(athenaClient.upCheck());

    // send something to the node (from pk1 to pk2)
    String digest =
        athenaClient
            .send(originalPayload, PK_1_B_64, new String[] {PK_2_B_64})
            .orElseThrow(AssertionFailedError::new);

    // call receive on the node
    byte[] receivedPayload =
        athenaClient.receive(digest, PK_2_B_64).orElseThrow(AssertionFailedError::new);

    // ensure we retrieved what we originally sent.
    assertArrayEquals(originalPayload, receivedPayload);

    // stop
    athena.stop();
  }

  @Test
  public void testTwoNodes() throws Exception {
    // setup our 2 nodes
    Athena athenaLauncher1 = new Athena();
    athenaLauncher1.run(configNode1);
    AthenaClient node1 = new AthenaClient(node1BaseUrl);
    assertTrue(node1.upCheck());

    Athena athenaLauncher2 = new Athena();
    athenaLauncher2.run(configNode2);
    AthenaClient node2 = new AthenaClient(node2BaseUrl);
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
    athenaLauncher1.stop();
    athenaLauncher2.stop();
  }
}

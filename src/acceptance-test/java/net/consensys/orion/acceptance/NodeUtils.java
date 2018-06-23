package net.consensys.orion.acceptance;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.orion.api.cmd.Orion;
import net.consensys.orion.api.config.Config;
import net.consensys.orion.impl.config.TomlConfigBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

import io.vertx.core.http.HttpClient;
import junit.framework.AssertionFailedError;
import okhttp3.HttpUrl;

public class NodeUtils {

  public static String joinPathsAsTomlListEntry(String... paths) {
    StringBuilder builder = new StringBuilder();
    boolean first = true;
    for (String path : paths) {
      if (!first) {
        builder.append(",");
      }
      first = false;
      builder.append("\"").append(Paths.get(path).toAbsolutePath().toString()).append("\"");
    }
    return builder.toString();
  }

  public static String url(String host, int port) {
    return new HttpUrl.Builder().scheme("http").host(host).port(port).build().toString();
  }

  public static Config nodeConfig(
      Path tempDir,
      String nodeUrl,
      int nodePort,
      String nodeNetworkInterface,
      String clientUrl,
      int clientPort,
      String clientNetworkInterface,
      String nodeName,
      String otherNodes,
      String pubKeys,
      String privKeys,
      String tls,
      String tlsServerTrust,
      String tlsClientTrust) throws IOException {

    Path workDir = tempDir.resolve("acceptance").toAbsolutePath();
    Files.createDirectories(workDir);

    // @formatter:off
    final String confString =
          "tls=\"" + tls + "\"\n"
        + "tlsservertrust=\"" + tlsServerTrust + "\"\n"
        + "tlsclienttrust=\"" + tlsClientTrust + "\"\n"
        + "nodeurl = \"" + nodeUrl + "\"\n"
        + "nodeport = " + nodePort + "\n"
        + "nodenetworkinterface = \"" + nodeNetworkInterface + "\"\n"
        + "clienturl = \"" + clientUrl + "\"\n"
        + "clientport = " + clientPort + "\n"
        + "clientnetworkinterface = \"" + clientNetworkInterface + "\"\n"
        + "storage = \"leveldb:database/" + nodeName + "\"\n"
        + "othernodes = [\"" + otherNodes + "\"]\n"
        + "publickeys = [" + pubKeys + "]\n"
        + "privatekeys = [" + privKeys + "]\n"
        + "workdir= \"" + workDir.toString() + "\"\n";
    // @formatter:on

    return new TomlConfigBuilder().build(new ByteArrayInputStream(confString.getBytes(StandardCharsets.UTF_8.name())));
  }

  public static int freePort() throws Exception {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  public static void ensureNetworkDiscoveryOccurs() throws InterruptedException {
    // TODO there must be a better way then sleeping & hoping network discovery occurs
    Thread.sleep(2000);
  }

  /** It's the callers responsibility to stop the started Orion. */
  public static Orion startOrion(Config config) throws ExecutionException, InterruptedException {
    final Orion orion = new Orion();
    orion.run(System.out, System.err, config);
    return orion;
  }

  public static EthClientStub client(int clientPort, HttpClient httpClient) {
    final EthClientStub client = new EthClientStub(clientPort, httpClient);
    assertTrue(client.upCheck());
    return client;
  }

  private static final byte[] originalPayload = "a wonderful transaction".getBytes(UTF_8);

  public static byte[] viewTransaction(EthClientStub viewer, String viewerKey, String digest) {
    return viewer.receive(digest, viewerKey).orElseThrow(AssertionFailedError::new);
  }

  public static String sendTransaction(EthClientStub sender, String senderKey, String... recipientsKey) {
    return sender.send(originalPayload, senderKey, recipientsKey).orElseThrow(AssertionFailedError::new);
  }

  /** Asserts the received payload matches that sent. */
  public static void assertTransaction(byte[] receivedPayload) {
    assertArrayEquals(originalPayload, receivedPayload);
  }
}

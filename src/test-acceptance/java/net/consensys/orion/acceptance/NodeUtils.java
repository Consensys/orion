package net.consensys.orion.acceptance;

import static org.junit.Assert.assertTrue;

import net.consensys.orion.api.cmd.Orion;
import net.consensys.orion.api.config.Config;
import net.consensys.orion.impl.config.TomlConfigBuilder;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import junit.framework.AssertionFailedError;
import okhttp3.HttpUrl;

public class NodeUtils {
  private static final String HTTP_PROTOCOL = "http";

  public String url(String host, int port) {
    return new HttpUrl.Builder().scheme(HTTP_PROTOCOL).host(host).port(port).build().toString();
  }

  public Config nodeConfig(
      String baseUrl,
      int port,
      String privacyUrl,
      int privacyPort,
      String nodeName,
      String otherNodes,
      String pubKeys,
      String privKeys) throws UnsupportedEncodingException {

    final String confString = new StringBuilder()
        .append("url = \"")
        .append(baseUrl)
        .append("\"\nport = ")
        .append(port)
        .append("\nprivacyurl = \"")
        .append(privacyUrl)
        .append("\"\nprivacyport = ")
        .append(privacyPort)
        .append("\nstorage = \"leveldb:database/" + nodeName + "\"")
        .append("\nothernodes = [\"")
        .append(otherNodes)
        .append("\"]\n" + "publickeys = [\"" + pubKeys + "\"]\n" + "privatekeys = [\"" + privKeys + "\"]")
        .toString();

    return new TomlConfigBuilder().build(new ByteArrayInputStream(confString.getBytes(StandardCharsets.UTF_8.name())));
  }

  public int freePort() throws Exception {
    final ServerSocket socket = new ServerSocket(0);
    final int toReturn = socket.getLocalPort();
    socket.close();

    return toReturn;
  }

  public void ensureNetworkDiscoveryOccurs() throws InterruptedException {
    // TODO there must be a better way then sleeping & hoping network discovery occurs
    Thread.sleep(2000);
  }

  public String sendTransactionExpectingError(
      EthNodeStub sender,
      byte[] payload,
      String senderKey,
      String... recipientsKey) {
    return sender.sendExpectingError(payload, senderKey, recipientsKey).orElseThrow(AssertionFailedError::new);
  }

  public String sendTransaction(EthNodeStub sender, byte[] payload, String senderKey, String... recipientsKey) {
    return sender.send(payload, senderKey, recipientsKey).orElseThrow(AssertionFailedError::new);
  }

  /** It's the callers responsibility to stop the started Orion. */
  public Orion startOrion(Config config) throws ExecutionException, InterruptedException {
    final Orion orion = new Orion();
    orion.run(config);

    return orion;
  }

  public EthNodeStub node(String baseUrl) {
    final EthNodeStub client = new EthNodeStub(baseUrl);
    assertTrue(client.upCheck());
    return client;
  }
}

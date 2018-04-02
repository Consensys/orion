package net.consensys.orion.acceptance.send.receive;

import static org.junit.Assert.assertArrayEquals;

import net.consensys.orion.acceptance.EthNodeStub;
import net.consensys.orion.acceptance.NodeUtils;
import net.consensys.orion.api.cmd.Orion;
import net.consensys.orion.api.config.Config;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutionException;

import junit.framework.AssertionFailedError;

/**
 * SendReceiveBase contains the common attributes and behaviours to tests that focus on sending and receiving private
 * transactions.
 */
public class SendReceiveBase {
  private static final NodeUtils nodeUtils = new NodeUtils();
  private static final byte[] originalPayload = "a wonderful transaction".getBytes();

  protected static int freePort() throws Exception {
    return nodeUtils.freePort();
  }

  protected static String url(String host, int port) {
    return nodeUtils.url(host, port);
  }

  protected static Config nodeConfig(
      String baseUrl,
      int port,
      String privacyUrl,
      int privacyPort,
      String nodeName,
      String otherNodes,
      String pubKeys,
      String privKeys) throws UnsupportedEncodingException {
    return nodeUtils.nodeConfig(baseUrl, port, privacyUrl, privacyPort, nodeName, otherNodes, pubKeys, privKeys);
  }

  protected byte[] viewTransaction(EthNodeStub viewer, String viewerKey, String digest) {
    return viewer.receive(digest, viewerKey).orElseThrow(AssertionFailedError::new);
  }

  protected String sendTransaction(EthNodeStub sender, String senderKey, String... recipientsKey) {
    return sender.send(originalPayload, senderKey, recipientsKey).orElseThrow(AssertionFailedError::new);
  }

  /** Asserts the received payload matches that sent. */
  protected void assertTransaction(byte[] receivedPayload) {
    assertArrayEquals(originalPayload, receivedPayload);
  }

  protected void ensureNetworkDiscoveryOccurs() throws InterruptedException {
    nodeUtils.ensureNetworkDiscoveryOccurs();
  }

  protected EthNodeStub node(String baseUrl) {
    return nodeUtils.node(baseUrl);
  }

  protected Orion startOrion(Config config) throws ExecutionException, InterruptedException {
    return nodeUtils.startOrion(config);
  }
}

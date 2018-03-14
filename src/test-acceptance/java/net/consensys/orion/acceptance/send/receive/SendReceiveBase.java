package net.consensys.orion.acceptance.send.receive;

import static org.junit.Assert.assertArrayEquals;

import net.consensys.orion.impl.http.OrionClient;

import junit.framework.AssertionFailedError;

/**
 * SendReceiveBase contains the common attributes and behaviours to tests that focus on sending and
 * receiving private transactions.
 */
public class SendReceiveBase {

  private static final SendReceiveUtil utils = new SendReceiveUtil();
  private static final byte[] originalPayload = "a wonderful transaction".getBytes();

  protected static SendReceiveUtil utils() {
    return utils;
  }

  protected byte[] viewTransaction(OrionClient viewer, String viewerKey, String digest) {
    return viewer.receive(digest, viewerKey).orElseThrow(AssertionFailedError::new);
  }

  protected String sendTransaction(OrionClient sender, String senderKey, String... recipientsKey) {
    return sender
        .send(originalPayload, senderKey, recipientsKey)
        .orElseThrow(AssertionFailedError::new);
  }

  /** Asserts the received payload matches that sent. */
  protected void assertTransaction(byte[] receivedPayload) {
    assertArrayEquals(originalPayload, receivedPayload);
  }

  protected void ensureNetworkDiscoveryOccurs() throws InterruptedException {
    // TODO there must be a better way then sleeping & hoping network discovery occurs
    Thread.sleep(1000);
  }
}

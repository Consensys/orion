package net.consensys.athena.acceptance;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import net.consensys.athena.api.cmd.Athena;
import net.consensys.athena.impl.http.AthenaClient;

import org.junit.Test;

public class SendReceiveTest {

  private static final byte[] originalPayload = "a wonderful transaction".getBytes();

  private final String singleNodeConfig =
      getClass().getClassLoader().getResource("singlenode.conf").getPath();
  private final String node1Config =
      getClass().getClassLoader().getResource("node1.conf").getPath();
  private final String node2Config =
      getClass().getClassLoader().getResource("node2.conf").getPath();

  private static final String singleNodeBaseUrl = "http://127.0.0.1:9001";
  private static final String node1BaseUrl = "http://127.0.0.1:9002";
  private static final String node2BaseUrl = "http://127.0.0.1:9003";

  private static final String pk1b64 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  private static final String pk2b64 = "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";

  @Test
  public void testSingleNode() throws Exception {
    // setup a single node with 2 public keys
    Athena athena = new Athena();
    athena.run(new String[] {singleNodeConfig});
    AthenaClient athenaClient = new AthenaClient(singleNodeBaseUrl);

    // ensure the node is awake
    assertTrue(athenaClient.upCheck());

    // send something to the node (from pk1 to pk2)
    String digest = athenaClient.send(originalPayload, pk1b64, new String[] {pk2b64});

    // call receive on the node
    byte[] receivedPayload = athenaClient.receive(digest, pk2b64);

    // ensure we retrieved what we originally sent.
    assertArrayEquals(originalPayload, receivedPayload);
  }

  @Test
  public void testTwoNodes() throws Exception {
    // setup our 2 nodes
    new Athena().run(new String[] {node1Config});
    AthenaClient node1 = new AthenaClient(node1BaseUrl);
    assertTrue(node1.upCheck());

    new Athena().run(new String[] {node2Config});
    AthenaClient node2 = new AthenaClient(node2BaseUrl);
    assertTrue(node2.upCheck());

    // ensure network discovery ran on node 1
    Thread.sleep(1000);

    // send a transaction from node1 to node2
    String digest = node1.send(originalPayload, pk1b64, new String[] {pk2b64});

    // call receive on the node 2
    byte[] receivedPayload = node2.receive(digest, pk2b64);

    // ensure we retrieved what we originally sent.
    assertArrayEquals(originalPayload, receivedPayload);
  }
}

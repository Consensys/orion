package net.consensys.athena.acceptance;

import net.consensys.athena.api.cmd.Athena;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SendReceiveTest {

  @Before
  public void setUp() throws Exception {
    // generate public / private keys
    Athena athena = new Athena();
    athena.run(new String[] {"-g", "key1"});
    athena.run(new String[] {"-g", "key2"});
  }

  @After
  public void tearDown() {
    try {
      new File("key1.pub").delete();
      new File("key1.key").delete();
      new File("key2.pub").delete();
      new File("key2.key").delete();
    } catch (Exception e) {

    }
  }

  @Test
  public void testSingleNode() throws Exception {
    // setup a single node with 2 public keys

    String configFilePath =
        this.getClass().getClassLoader().getResource("singlenode.conf").getPath();

    Athena athena = new Athena();
    athena.run(new String[] {configFilePath});

    // send something to the node

    // call receive on the node
  }
}

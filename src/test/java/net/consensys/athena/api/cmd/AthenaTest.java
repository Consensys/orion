package net.consensys.athena.api.cmd;

import net.consensys.athena.impl.http.server.netty.NettyServer;

import java.io.InputStream;

import org.junit.Test;

public class AthenaTest {

  @Test
  public void testServerStartWithFullConfig() throws Exception {
    InputStream configAsStream =
        this.getClass().getClassLoader().getResourceAsStream("fullConfigTest.toml");
    NettyServer server = Athena.startServer(configAsStream);

    server.stop();
  }
}

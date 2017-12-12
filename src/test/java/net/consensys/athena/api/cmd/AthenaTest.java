package net.consensys.athena.api.cmd;

import net.consensys.athena.api.config.Config;
import net.consensys.athena.impl.config.TomlConfigBuilder;
import net.consensys.athena.impl.http.server.netty.NettyServer;

import java.util.Optional;

import org.junit.Test;

public class AthenaTest {

  @Test
  public void testServerStartWithFullConfig() throws Exception {
    TomlConfigBuilder configBuilder = new TomlConfigBuilder();
    Config config =
        configBuilder.build(
            this.getClass().getClassLoader().getResourceAsStream("fullConfigTest.toml"));
    NettyServer server = Athena.startServer(config);

    server.stop();
  }

  @Test
  public void testLoadConfigForTheStandardConfig() throws Exception {
    Config config = Athena.loadConfig(Optional.of("src/main/resources/default.conf"));
  }

  @Test
  public void testDefaultConfigIsUsedWhenNoneProvided() throws Exception {
    Config config = Athena.loadConfig(Optional.empty());
  }
}

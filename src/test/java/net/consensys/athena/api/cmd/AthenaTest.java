package net.consensys.athena.api.cmd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.consensys.athena.api.config.Config;
import net.consensys.athena.impl.config.TomlConfigBuilder;
import net.consensys.athena.impl.http.server.netty.NettyServer;
import net.consensys.athena.impl.http.server.netty.NettySettings;

import java.io.File;
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

    NettySettings settings = server.getSettings();

    assertEquals(Optional.of(9001), settings.getHttpPort());

    File expectedSocket = new File("athena.ipc");
    assertEquals(Optional.of(expectedSocket), settings.getDomainSocketPath());

    server.stop();
  }

  @Test
  public void testLoadConfigForTheStandardConfig() throws Exception {
    Config config = Athena.loadConfig(Optional.of("src/main/resources/sample.conf"));
    assertEquals(9001, config.port());

    File expectedSocket = new File("athena.ipc");
    assertTrue(config.socket().isPresent());
    assertEquals(expectedSocket, config.socket().get());
  }

  @Test
  public void testDefaultConfigIsUsedWhenNoneProvided() throws Exception {
    Config config = Athena.loadConfig(Optional.empty());

    assertEquals(8080, config.port());
    assertFalse(config.socket().isPresent());
  }
}

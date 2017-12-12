package net.consensys.athena.api.cmd;

import static java.util.Optional.empty;

import net.consensys.athena.api.config.Config;
import net.consensys.athena.impl.config.TomlConfigBuilder;
import net.consensys.athena.impl.http.server.Serializer;
import net.consensys.athena.impl.http.server.netty.DefaultNettyServer;
import net.consensys.athena.impl.http.server.netty.NettyServer;
import net.consensys.athena.impl.http.server.netty.NettySettings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

public class Athena {

  private static final int DEFAULT_HTTP_PORT = 8080;

  public static void main(String[] args) throws Exception {
    Optional<String> configFileName = args.length > 0 ? Optional.of(args[0]) : Optional.empty();

    Config config = loadConfig(configFileName);
    // start http server
    NettyServer server = startServer(config);
    joinServer(server);
  }

  static Config loadConfig(Optional<String> configFileName) throws FileNotFoundException {
    InputStream configAsStream;
    if (configFileName.isPresent()) {
      configAsStream = new FileInputStream(new File(configFileName.get()));
    } else {
      configAsStream = Athena.class.getResourceAsStream("default.conf");
    }

    TomlConfigBuilder configBuilder = new TomlConfigBuilder();

    return configBuilder.build(configAsStream);
  }

  private static void joinServer(NettyServer server) throws InterruptedException {
    //Don't expect this method to be tested.
    server.join();
  }

  @NotNull
  static NettyServer startServer(Config config) throws InterruptedException {
    NettySettings settings =
        new NettySettings(
            config.socket(),
            Optional.of((int) config.port()),
            empty(),
            new AthenaRouter(),
            new Serializer(new ObjectMapper()));
    NettyServer server = new DefaultNettyServer(settings);
    server.start();
    return server;
  }
}

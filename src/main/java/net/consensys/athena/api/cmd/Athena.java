package net.consensys.athena.api.cmd;

import static java.util.Optional.empty;

import net.consensys.athena.api.config.Config;
import net.consensys.athena.impl.config.TomlConfigBuilder;
import net.consensys.athena.impl.http.server.Serializer;
import net.consensys.athena.impl.http.server.netty.DefaultNettyServer;
import net.consensys.athena.impl.http.server.netty.NettyServer;
import net.consensys.athena.impl.http.server.netty.NettySettings;

import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

public class Athena {

  private static final int DEFAULT_HTTP_PORT = 8080;

  public static void main(String[] args) throws InterruptedException {
    String configFileName = args.length > 0 ? args[0] : "default.conf";
    InputStream configAsStream =
        MethodHandles.lookup().lookupClass().getResourceAsStream(configFileName);

    // start http server
    NettyServer server = startServer(configAsStream);
    joinServer(server);
  }

  private static void joinServer(NettyServer server) throws InterruptedException {
    //Don't expect this method to be tested.
    server.join();
  }

  @NotNull
  static NettyServer startServer(InputStream configFile) throws InterruptedException {

    TomlConfigBuilder configBuilder = new TomlConfigBuilder();

    Config config = configBuilder.build(configFile);

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

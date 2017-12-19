package net.consensys.athena.api.cmd;

import static java.util.Optional.empty;

import net.consensys.athena.api.config.Config;
import net.consensys.athena.api.network.NetworkNodes;
import net.consensys.athena.impl.config.TomlConfigBuilder;
import net.consensys.athena.impl.http.server.Serializer;
import net.consensys.athena.impl.http.server.netty.DefaultNettyServer;
import net.consensys.athena.impl.http.server.netty.NettyServer;
import net.consensys.athena.impl.http.server.netty.NettySettings;
import net.consensys.athena.impl.network.MemoryNetworkNodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import org.jetbrains.annotations.NotNull;

public class Athena {

  private static NetworkNodes networkNodes;

  public static void main(String[] args) throws Exception {
    Optional<String> configFileName = args.length > 0 ? Optional.of(args[0]) : Optional.empty();

    Config config = loadConfig(configFileName);
    networkNodes = new MemoryNetworkNodes(config);

    // start http server
    NettyServer server = startServer(config);
    joinServer(server);
  }

  static Config loadConfig(Optional<String> configFileName) throws FileNotFoundException {
    InputStream configAsStream;
    if (configFileName.isPresent()) {
      configAsStream = new FileInputStream(new File(configFileName.get()));
    } else {
      configAsStream = Athena.class.getResourceAsStream("/default.conf");
    }

    TomlConfigBuilder configBuilder = new TomlConfigBuilder();

    return configBuilder.build(configAsStream);
  }

  private static void joinServer(NettyServer server) throws InterruptedException {
    server.join();
  }

  @NotNull
  static NettyServer startServer(Config config) throws InterruptedException {
    NettySettings settings =
        new NettySettings(
            config.socket(),
            Optional.of((int) config.port()),
            empty(),
            new AthenaRouter(networkNodes),
            new Serializer(new ObjectMapper(), new ObjectMapper(new CBORFactory())));
    NettyServer server = new DefaultNettyServer(settings);
    server.start();
    return server;
  }
}

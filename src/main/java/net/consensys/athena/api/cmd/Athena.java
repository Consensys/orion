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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class Athena {

  private static final Logger log = LogManager.getLogger();

  private static NetworkNodes networkNodes;

  public static void main(String[] args) throws Exception {
    log.info("starting athena");
    Optional<String> configFileName = args.length > 0 ? Optional.of(args[0]) : Optional.empty();

    Config config = loadConfig(configFileName);
    networkNodes = new MemoryNetworkNodes(config);

    try {
      NettyServer server = startServer(config);
      joinServer(server);
    } catch (InterruptedException ie) {
      log.error(ie.getMessage());
      throw ie;
    } finally {
      log.warn("netty server stopped");
    }
  }

  static Config loadConfig(Optional<String> configFileName) throws FileNotFoundException {
    InputStream configAsStream;
    if (configFileName.isPresent()) {
      log.info("using {} provided config file", configFileName.get());
      configAsStream = new FileInputStream(new File(configFileName.get()));
    } else {
      log.warn("no config file provided, using default.conf");
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
    ObjectMapper jsonObjectMapper = new ObjectMapper();
    Serializer serializer = new Serializer(jsonObjectMapper, new ObjectMapper(new CBORFactory()));
    NettySettings settings =
        new NettySettings(
            config.socket(),
            Optional.of((int) config.port()),
            empty(),
            new AthenaRouter(networkNodes, config, serializer, jsonObjectMapper),
            serializer);
    NettyServer server = new DefaultNettyServer(settings);

    log.info("starting netty server");
    server.start();
    return server;
  }
}

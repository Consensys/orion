package net.consensys.athena.api.cmd;

import static java.util.Optional.empty;

import net.consensys.athena.api.config.Config;
import net.consensys.athena.api.enclave.KeyConfig;
import net.consensys.athena.api.network.NetworkNodes;
import net.consensys.athena.impl.cmd.AthenaArguments;
import net.consensys.athena.impl.config.TomlConfigBuilder;
import net.consensys.athena.impl.enclave.sodium.SodiumFileKeyStore;
import net.consensys.athena.impl.http.data.Serializer;
import net.consensys.athena.impl.http.server.netty.DefaultNettyServer;
import net.consensys.athena.impl.http.server.netty.NettyServer;
import net.consensys.athena.impl.http.server.netty.NettySettings;
import net.consensys.athena.impl.network.MemoryNetworkNodes;

import java.io.*;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Athena {

  private static final Logger log = LogManager.getLogger();

  private static NetworkNodes networkNodes;

  public static void main(String[] args) throws Exception {
    Athena athena = new Athena();
    athena.run(args);
  }

  public void run(String[] args) throws FileNotFoundException, InterruptedException {
    log.info("starting athena");
    AthenaArguments arguments = new AthenaArguments(args);

    if (!arguments.argumentExit()) {
      Config config = loadConfig(arguments.configFileName());

      if (arguments.keysToGenerate().isPresent()) {
        log.info("Generating Key Pairs");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(Include.NON_NULL);

        SodiumFileKeyStore keyStore = new SodiumFileKeyStore(config, objectMapper);

        for (int i = 0; i < arguments.keysToGenerate().get().length; i++) {
          keyStore.generateKeyPair(
              new KeyConfig(arguments.keysToGenerate().get()[i], Optional.empty()));
        }

      } else {
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
    }
  }

  Config loadConfig(Optional<String> configFileName) throws FileNotFoundException {
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

  private void joinServer(NettyServer server) throws InterruptedException {
    server.join();
  }

  NettyServer startServer(Config config) throws InterruptedException {
    ObjectMapper jsonObjectMapper = new ObjectMapper();
    jsonObjectMapper.setSerializationInclusion(Include.NON_NULL);

    ObjectMapper cborObjectMapper = new ObjectMapper(new CBORFactory());
    cborObjectMapper.setSerializationInclusion(Include.NON_NULL);
    Serializer serializer = new Serializer(jsonObjectMapper, cborObjectMapper);
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

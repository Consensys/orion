package net.consensys.athena.api.cmd;

import static java.util.Optional.empty;

import net.consensys.athena.api.config.Config;
import net.consensys.athena.api.network.NetworkNodes;
import net.consensys.athena.impl.config.TomlConfigBuilder;
import net.consensys.athena.impl.enclave.sodium.SodiumFileKeyStore;
import net.consensys.athena.impl.http.data.Serializer;
import net.consensys.athena.impl.http.server.netty.DefaultNettyServer;
import net.consensys.athena.impl.http.server.netty.NettyServer;
import net.consensys.athena.impl.http.server.netty.NettySettings;
import net.consensys.athena.impl.network.MemoryNetworkNodes;

import java.io.*;
import java.util.Optional;

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

    Optional<String> configFileName = Optional.empty();
    Optional<String[]> keysToGenerate = Optional.empty();

    //Process Arguments
    // Usage Athena [--generatekeys|-g names] [--version | -v] [--help | -h] [config]
    // names - comma seperated list of key file prefixes (can include directory information) to generate key(s) for
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--generatekeys":
        case "-g":
          String keys = args[++i];
          keysToGenerate = Optional.of(keys.split(","));
          break;
        default:
          configFileName = Optional.of(args[i]);
      }
    }

    Config config = loadConfig(configFileName);

    if (keysToGenerate.isPresent()) {
      ObjectMapper objectMapper = new ObjectMapper();
      SodiumFileKeyStore keyStore = new SodiumFileKeyStore(config, objectMapper);
      Console console = System.console();
      if (console == null) {
        System.out.println("Unable to get a Console instance");
        System.exit(0);
      }
      char[] pwd;

      for (int i = 0; i < keysToGenerate.get().length; i++) {
        //Prompt for Password from user
        pwd = console.readPassword("Enter password for key pair %s", keysToGenerate.get()[i]);
        Optional<String> password = pwd.length > 0 ? Optional.of(new String(pwd)) : Optional.empty();
        System.out.println(
            "Password for key [" + keysToGenerate.get()[i] + "] - [" + password + "]");

        //keyStore.generateKeyPair(new KeyConfig(keysToGenerate.get()[i], password));
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

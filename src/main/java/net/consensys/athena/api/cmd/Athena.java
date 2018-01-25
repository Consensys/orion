package net.consensys.athena.api.cmd;

import static io.vertx.core.Vertx.vertx;
import static java.util.Optional.empty;

import net.consensys.athena.api.config.Config;
import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.KeyConfig;
import net.consensys.athena.api.network.NetworkNodes;
import net.consensys.athena.impl.cmd.AthenaArguments;
import net.consensys.athena.impl.config.TomlConfigBuilder;
import net.consensys.athena.impl.enclave.sodium.LibSodiumEnclave;
import net.consensys.athena.impl.enclave.sodium.SodiumFileKeyStore;
import net.consensys.athena.impl.http.server.HttpServerSettings;
import net.consensys.athena.impl.http.server.vertx.VertxServer;
import net.consensys.athena.impl.network.MemoryNetworkNodes;
import net.consensys.athena.impl.network.NetworkDiscovery;
import net.consensys.athena.impl.utils.Serializer;

import java.io.*;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Athena {

  private static final Logger log = LogManager.getLogger();
  public static final String name = "athena";

  private static final Serializer serializer = new Serializer();
  private static final Vertx vertx = vertx();

  public static void main(String[] args) throws Exception {
    log.info("starting athena");
    Athena athena = new Athena();
    athena.run(args);
  }

  public void run(String[] args) throws FileNotFoundException {
    // parsing arguments
    AthenaArguments arguments = new AthenaArguments(args);

    if (arguments.argumentExit()) {
      return;
    }

    if (arguments.displayVersion()) {
      displayVersion();
      return;
    }

    // load config file
    Config config = loadConfig(arguments.configFileName());

    // generate key pair and exit
    if (arguments.keysToGenerate().isPresent()) {
      runGenerateKeyPairs(config, arguments.keysToGenerate().get());
      return;
    }

    // start our API server
    runApiServer(config);
  }

  private void runApiServer(Config config) {
    NetworkNodes networkNodes = new MemoryNetworkNodes(config);
    Enclave enclave = new LibSodiumEnclave(config, new SodiumFileKeyStore(config, serializer));

    AthenaRoutes routes = new AthenaRoutes(vertx, networkNodes, serializer, enclave);

    HttpServerSettings httpSettings =
        new HttpServerSettings(config.socket(), Optional.of((int) config.port()), empty(), null);

    VertxServer httpServer = new VertxServer(vertx, routes.getRouter(), httpSettings);
    httpServer.start();

    NetworkDiscovery discovery = new NetworkDiscovery(networkNodes, serializer);
    vertx.deployVerticle(discovery);

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    httpServer.stop().get();
                  } catch (Exception e) {

                  } finally {
                    vertx.close();
                  }
                }));
  }

  private void displayVersion() {
    InputStream versionAsStream = Athena.class.getResourceAsStream("/version.txt");
    BufferedReader buffer = new BufferedReader(new InputStreamReader(versionAsStream));
    String contents = buffer.lines().collect(Collectors.joining("\n"));
    System.out.println(contents);
  }

  private void runGenerateKeyPairs(Config config, String[] keysToGenerate) {
    log.info("generating Key Pairs");

    SodiumFileKeyStore keyStore = new SodiumFileKeyStore(config, serializer);

    Scanner scanner = new Scanner(System.in);

    for (String keyName : keysToGenerate) {

      //Prompt for Password from user
      System.out.format("Enter password for key pair [%s] : ", keyName);
      String pwd = scanner.nextLine().trim();
      Optional<String> password = pwd.length() > 0 ? Optional.of(pwd) : Optional.empty();

      log.debug("Password for key [" + keyName + "] - [" + password + "]");

      keyStore.generateKeyPair(new KeyConfig(keyName, password));
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
}

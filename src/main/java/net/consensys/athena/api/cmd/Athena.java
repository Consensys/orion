package net.consensys.athena.api.cmd;

import static io.vertx.core.Vertx.vertx;
import static java.util.Optional.empty;

import net.consensys.athena.api.config.Config;
import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.enclave.KeyConfig;
import net.consensys.athena.api.network.NetworkNodes;
import net.consensys.athena.api.storage.StorageEngine;
import net.consensys.athena.impl.cmd.AthenaArguments;
import net.consensys.athena.impl.config.TomlConfigBuilder;
import net.consensys.athena.impl.enclave.sodium.LibSodiumEnclave;
import net.consensys.athena.impl.enclave.sodium.SodiumFileKeyStore;
import net.consensys.athena.impl.http.server.HttpServerSettings;
import net.consensys.athena.impl.http.server.vertx.VertxServer;
import net.consensys.athena.impl.network.MemoryNetworkNodes;
import net.consensys.athena.impl.network.NetworkDiscovery;
import net.consensys.athena.impl.storage.file.MapDbStorage;
import net.consensys.athena.impl.utils.Serializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Athena {

  private static final Logger log = LogManager.getLogger();

  private static final Serializer serializer = new Serializer();

  private final Vertx vertx = vertx();
  private StorageEngine<EncryptedPayload> storageEngine;

  public static void main(String[] args) throws Exception {
    log.info("starting athena");
    Athena athena = new Athena();
    athena.run(args);
  }

  public void stop() {
    CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();

    vertx.close(
        result -> {
          if (result.succeeded()) {
            resultFuture.complete(true);
          } else {
            resultFuture.completeExceptionally(result.cause());
          }
        });

    try {
      resultFuture.get();

    } catch (InterruptedException | ExecutionException io) {
      log.error(io.getMessage());
    }

    if (storageEngine != null) {
      storageEngine.close();
    }
  }

  public void run(String... args)
      throws FileNotFoundException, ExecutionException, InterruptedException {
    // parsing arguments
    AthenaArguments arguments = new AthenaArguments(args);

    if (arguments.argumentExit()) {
      return;
    }

    // load config file
    Config config = loadConfig(arguments.configFileName());

    // generate key pair and exit
    if (arguments.keysToGenerate().isPresent()) {
      generateKeyPairs(config, arguments.keysToGenerate().get());
      return;
    }

    // start our API server
    run(config);
  }

  public void run(Config config) throws ExecutionException, InterruptedException {
    SodiumFileKeyStore keyStore = new SodiumFileKeyStore(config, serializer);
    NetworkNodes networkNodes = new MemoryNetworkNodes(config, keyStore.nodeKeys());
    Enclave enclave = new LibSodiumEnclave(config, keyStore);

    // storage path
    String storagePath = config.workDir().orElse(new File(".")).getPath() + "/";
    String configStorage = config.storage();
    if (configStorage.startsWith("dir:")) {
      storagePath += configStorage.substring(4) + "/";
    }

    // if path doesn't exist, create it.
    File dirStoragePath = new File(storagePath);
    log.info("using storage path {}", storagePath);
    if (!dirStoragePath.exists()) {
      log.warn("storage path {} doesn't exist, creating...", storagePath);
      if (!dirStoragePath.mkdirs()) {
        log.error("couldn't create storage path {}", storagePath);
        System.exit(-1);
      }
    }

    // create our storage engine
    storageEngine = new MapDbStorage(storagePath + "routerdb");
    AthenaRoutes routes = new AthenaRoutes(vertx, networkNodes, serializer, enclave, storageEngine);

    HttpServerSettings httpSettings =
        new HttpServerSettings(config.socket(), Optional.of((int) config.port()), empty(), null);

    VertxServer httpServer = new VertxServer(vertx, routes.getRouter(), httpSettings);
    httpServer.start().get();

    NetworkDiscovery discovery = new NetworkDiscovery(networkNodes, serializer);
    vertx.deployVerticle(discovery);

    Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
  }

  private void generateKeyPairs(Config config, String[] keysToGenerate) {
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

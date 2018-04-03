package net.consensys.orion.api.cmd;

import static io.vertx.core.Vertx.vertx;
import static net.consensys.orion.impl.http.server.HttpContentType.APPLICATION_OCTET_STREAM;
import static net.consensys.orion.impl.http.server.HttpContentType.CBOR;
import static net.consensys.orion.impl.http.server.HttpContentType.JSON;
import static net.consensys.orion.impl.http.server.HttpContentType.TEXT;

import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.config.ConfigException;
import net.consensys.orion.api.enclave.Enclave;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.enclave.KeyConfig;
import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.api.storage.Storage;
import net.consensys.orion.api.storage.StorageEngine;
import net.consensys.orion.api.storage.StorageKeyBuilder;
import net.consensys.orion.impl.cmd.OrionArguments;
import net.consensys.orion.impl.config.TomlConfigBuilder;
import net.consensys.orion.impl.enclave.sodium.LibSodiumEnclave;
import net.consensys.orion.impl.enclave.sodium.SodiumEncryptedPayload;
import net.consensys.orion.impl.enclave.sodium.SodiumFileKeyStore;
import net.consensys.orion.impl.http.handler.partyinfo.PartyInfoHandler;
import net.consensys.orion.impl.http.handler.push.PushHandler;
import net.consensys.orion.impl.http.handler.receive.ReceiveHandler;
import net.consensys.orion.impl.http.handler.send.SendHandler;
import net.consensys.orion.impl.http.handler.upcheck.UpcheckHandler;
import net.consensys.orion.impl.http.server.vertx.HttpErrorHandler;
import net.consensys.orion.impl.http.server.vertx.VertxServer;
import net.consensys.orion.impl.network.ConcurrentNetworkNodes;
import net.consensys.orion.impl.network.NetworkDiscovery;
import net.consensys.orion.impl.storage.EncryptedPayloadStorage;
import net.consensys.orion.impl.storage.Sha512_256StorageKeyBuilder;
import net.consensys.orion.impl.storage.file.MapDbStorage;
import net.consensys.orion.impl.storage.leveldb.LevelDbStorage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Orion {

  private static final Logger log = LogManager.getLogger();
  public static final String name = "orion";

  private final Vertx vertx = vertx();
  private StorageEngine<EncryptedPayload> storageEngine;

  public static void main(String[] args) throws Exception {
    log.info("starting orion");
    Orion orion = new Orion();
    orion.run(args);
  }

  public static void configureRoutes(
      Vertx vertx,
      ConcurrentNetworkNodes networkNodes,
      Enclave enclave,
      Storage<EncryptedPayload> storage,
      Router publicRouter,
      Router privateRouter) {


    // sets response content-type from Accept header
    // and handle errors

    LoggerHandler loggerHandler = LoggerHandler.create();

    //Setup Public APIs
    publicRouter
        .route()
        .handler(BodyHandler.create())
        .handler(loggerHandler)
        .handler(ResponseContentTypeHandler.create())
        .failureHandler(new HttpErrorHandler());

    publicRouter.get("/upcheck").produces(TEXT.httpHeaderValue).handler(new UpcheckHandler());

    publicRouter.post("/partyinfo").produces(CBOR.httpHeaderValue).consumes(CBOR.httpHeaderValue).handler(
        new PartyInfoHandler(networkNodes));

    publicRouter.post("/push").produces(TEXT.httpHeaderValue).consumes(CBOR.httpHeaderValue).handler(
        new PushHandler(storage));

    //Setup Private APIs
    privateRouter
        .route()
        .handler(BodyHandler.create())
        .handler(loggerHandler)
        .handler(ResponseContentTypeHandler.create())
        .failureHandler(new HttpErrorHandler());

    privateRouter.get("/upcheck").produces(TEXT.httpHeaderValue).handler(new UpcheckHandler());

    privateRouter.post("/send").produces(JSON.httpHeaderValue).consumes(JSON.httpHeaderValue).handler(
        new SendHandler(vertx, enclave, storage, networkNodes, JSON));
    privateRouter
        .post("/sendraw")
        .produces(APPLICATION_OCTET_STREAM.httpHeaderValue)
        .consumes(APPLICATION_OCTET_STREAM.httpHeaderValue)
        .handler(new SendHandler(vertx, enclave, storage, networkNodes, APPLICATION_OCTET_STREAM));

    privateRouter.post("/receive").produces(JSON.httpHeaderValue).consumes(JSON.httpHeaderValue).handler(
        new ReceiveHandler(enclave, storage, JSON));
    privateRouter
        .post("/receiveraw")
        .produces(APPLICATION_OCTET_STREAM.httpHeaderValue)
        .consumes(APPLICATION_OCTET_STREAM.httpHeaderValue)
        .handler(new ReceiveHandler(enclave, storage, APPLICATION_OCTET_STREAM));
  }

  public void stop() {
    CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();

    vertx.close(result -> {
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

  public void run(String... args) throws FileNotFoundException, ExecutionException, InterruptedException {
    // parsing arguments
    OrionArguments arguments = new OrionArguments(args);

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
      generateKeyPairs(config, arguments.keysToGenerate().get());
      return;
    }

    // start our API server
    run(config);
  }

  public void run(Config config) throws ExecutionException, InterruptedException {
    SodiumFileKeyStore keyStore = new SodiumFileKeyStore(config);
    ConcurrentNetworkNodes networkNodes = new ConcurrentNetworkNodes(config, keyStore.nodeKeys());
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
    storageEngine = createStorageEngine(config, storagePath);
    // Vertx routers
    Router publicRouter = Router.router(vertx);
    Router privateRouter = Router.router(vertx);
    // controller dependencies
    StorageKeyBuilder keyBuilder = new Sha512_256StorageKeyBuilder(enclave);
    EncryptedPayloadStorage storage = new EncryptedPayloadStorage(storageEngine, keyBuilder);
    configureRoutes(vertx, networkNodes, enclave, storage, publicRouter, privateRouter);

    // asynchronously start the vertx http server for public API
    CompletableFuture<Boolean> publicFuture = startHttpServerAsync(config.port(), vertx, publicRouter);

    // asynchronously start the vertx http server for private API
    CompletableFuture<Boolean> privateFuture = startHttpServerAsync(config.privacyPort(), vertx, privateRouter);

    // Block and wait for the two servers to start.
    CompletableFuture.allOf(publicFuture, privateFuture).get();

    // start network discovery of other peers
    NetworkDiscovery discovery = new NetworkDiscovery(networkNodes);
    vertx.deployVerticle(discovery);

    // set shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
  }

  private CompletableFuture<Boolean> startHttpServerAsync(int port, Vertx vertx, Router publicRouter) {
    HttpServerOptions publicServerOptions = new HttpServerOptions();
    publicServerOptions.setPort(port);

    VertxServer publicHTTPServer = new VertxServer(vertx, publicRouter, publicServerOptions);
    return (CompletableFuture<Boolean>) publicHTTPServer.start();
  }

  private StorageEngine<EncryptedPayload> createStorageEngine(Config config, String storagePath) {
    String storage = config.storage();
    String dbPath = "routerdb";
    String[] storageOptions = storage.split(":");
    if (storageOptions.length > 1) {
      dbPath = storageOptions[1];
    }
    new File(storagePath + dbPath).mkdirs();
    if (storage.startsWith("mapdb")) {
      return new MapDbStorage<>(SodiumEncryptedPayload.class, storagePath + dbPath);
    } else if (storage.startsWith("leveldb")) {
      return new LevelDbStorage<>(SodiumEncryptedPayload.class, storagePath + dbPath);
    } else {
      throw new ConfigException(
          OrionErrorCode.CONFIGURATION_STORAGE_MECHANISM,
          "unsupported storage mechanism: " + storage);
    }
  }

  private void displayVersion() {
    try (InputStream versionAsStream = Orion.class.getResourceAsStream("/version.txt");
        BufferedReader buffer = new BufferedReader(new InputStreamReader(versionAsStream));) {
      String contents = buffer.lines().collect(Collectors.joining("\n"));
      System.out.println(contents);
    } catch (IOException e) {
      log.error("Read of Version file failed", e);
    }
  }

  private void generateKeyPairs(Config config, String[] keysToGenerate) {
    log.info("generating Key Pairs");

    SodiumFileKeyStore keyStore = new SodiumFileKeyStore(config);

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
      configAsStream = Orion.class.getResourceAsStream("/default.conf");
    }

    TomlConfigBuilder configBuilder = new TomlConfigBuilder();

    return configBuilder.build(configAsStream);
  }
}

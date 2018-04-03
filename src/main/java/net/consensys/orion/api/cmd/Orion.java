package net.consensys.orion.api.cmd;

import static io.vertx.core.Vertx.vertx;
import static java.nio.charset.StandardCharsets.UTF_8;
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
import net.consensys.orion.api.exception.OrionException;
import net.consensys.orion.api.exception.OrionStartException;
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
import net.consensys.orion.impl.network.ConcurrentNetworkNodes;
import net.consensys.orion.impl.network.NetworkDiscovery;
import net.consensys.orion.impl.storage.EncryptedPayloadStorage;
import net.consensys.orion.impl.storage.Sha512_256StorageKeyBuilder;
import net.consensys.orion.impl.storage.file.MapDbStorage;
import net.consensys.orion.impl.storage.leveldb.LevelDbStorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
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

  private final Vertx vertx;
  private StorageEngine<EncryptedPayload> storageEngine;
  private NetworkDiscovery discovery;
  private HttpServer publicHTTPServer;
  private HttpServer privateHTTPServer;

  public static void main(String[] args) {
    log.info("starting orion");
    Orion orion = new Orion();
    try {
      orion.run(System.out, System.err, args);
    } catch (OrionStartException e) {
      System.err.println("Orion failed to start: " + e.getMessage());
      System.exit(1);
    } catch (ConfigException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    } catch (Throwable t) {
      log.error("Unexpected exception upon starting Orion", t);
      System.err.println(
          "An unexpected exception was reported while starting Orion. Please refer to the logs for more information");
      System.exit(1);
    }
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

  public Orion() {
    this(vertx());
  }

  Orion(Vertx vertx) {
    this.vertx = vertx;
  }

  public void stop() {
    CompletableFuture<Boolean> publicServerFuture = new CompletableFuture<>();
    CompletableFuture<Boolean> privateServerFuture = new CompletableFuture<>();
    CompletableFuture<Boolean> discoveryFuture = new CompletableFuture<>();
    publicHTTPServer.close(result -> {
      if (result.succeeded()) {
        publicServerFuture.complete(true);
      } else {
        publicServerFuture.completeExceptionally(result.cause());
      }
    });
    privateHTTPServer.close(result -> {
      if (result.succeeded()) {
        privateServerFuture.complete(true);
      } else {
        privateServerFuture.completeExceptionally(result.cause());
      }
    });
    try {
      Future<Void> future = Future.future();
      future.setHandler(result -> {
        if (result.succeeded()) {
          discoveryFuture.complete(true);
        } else {
          discoveryFuture.completeExceptionally(result.cause());
        }
      });
      discovery.stop(future);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    try {
      CompletableFuture.allOf(publicServerFuture, privateServerFuture, discoveryFuture).get();
    } catch (InterruptedException | ExecutionException e) {
      log.error("Error stopping vert.x HTTP servers and discovery", e);
    }

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
      log.error("Error stopping vert.x", io);
    }

    if (storageEngine != null) {
      storageEngine.close();
    }
  }

  public void run(PrintStream out, PrintStream err, String... args) {
    // parsing arguments
    OrionArguments arguments = new OrionArguments(out, err, args);

    if (arguments.argumentExit()) {
      return;
    }

    // load config file
    Config config = loadConfig(arguments.configFileName().map(fileName -> {
      File file = new File(fileName);
      if (!file.exists()) {
        throw new OrionException(OrionErrorCode.CONFIG_FILE_MISSING);
      }
      return file;
    }));

    // generate key pair and exit
    if (arguments.keysToGenerate().isPresent()) {
      generateKeyPairs(out, err, config, arguments.keysToGenerate().get());
      return;
    }

    run(out, err, config);
  }

  public void run(PrintStream out, PrintStream err, Config config) {
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
        log.error("Couldn't create storage path {}", storagePath);
        err.println("Couldn't create storage path " + storagePath);
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
    CompletableFuture<Boolean> publicFuture = new CompletableFuture<>();
    publicHTTPServer = vertx
        .createHttpServer(new HttpServerOptions().setPort(config.port()))
        .requestHandler(publicRouter::accept)
        .listen(completeFutureInHandler(publicFuture));

    CompletableFuture<Boolean> privateFuture = new CompletableFuture<>();
    privateHTTPServer = vertx
        .createHttpServer(new HttpServerOptions().setPort(config.privacyPort()))
        .requestHandler(privateRouter::accept)
        .listen(completeFutureInHandler(privateFuture));

    // start network discovery of other peers
    discovery = new NetworkDiscovery(networkNodes);
    CompletableFuture<Boolean> verticleFuture = new CompletableFuture<>();
    vertx.deployVerticle(discovery, result -> {
      if (result.succeeded()) {
        verticleFuture.complete(true);
      } else {
        verticleFuture.completeExceptionally(result.cause());
      }
    });

    try {
      CompletableFuture.allOf(publicFuture, privateFuture, verticleFuture).get();
    } catch (ExecutionException e) {
      log.error("Error reported while starting Orion", e.getCause());
      throw new OrionStartException(OrionErrorCode.SERVICE_START_ERROR, e.getCause().getMessage());
    } catch (InterruptedException e) {
      throw new OrionStartException(
          OrionErrorCode.SERVICE_START_INTERRUPTED,
          "Orion was interrupted while starting services");
    }

    // set shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
  }

  private Handler<AsyncResult<HttpServer>> completeFutureInHandler(CompletableFuture<Boolean> future) {
    return result -> {
      if (result.succeeded()) {
        future.complete(true);
      } else {
        future.completeExceptionally(result.cause());
      }
    };
  }

  private StorageEngine<EncryptedPayload> createStorageEngine(Config config, String storagePath) {
    String storage = config.storage();
    String dbPath = "routerdb";
    String[] storageOptions = storage.split(":", 2);
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

  private void generateKeyPairs(PrintStream out, PrintStream err, Config config, String[] keysToGenerate) {
    log.info("generating Key Pairs");

    SodiumFileKeyStore keyStore = new SodiumFileKeyStore(config);

    Scanner scanner = new Scanner(System.in, UTF_8.name());

    for (String keyName : keysToGenerate) {

      //Prompt for Password from user
      out.format("Enter password for key pair [%s] : ", keyName);
      String pwd = scanner.nextLine().trim();
      Optional<String> password = pwd.length() > 0 ? Optional.of(pwd) : Optional.empty();

      out.println("Password for key [" + keyName + "] - [" + password + "]");

      keyStore.generateKeyPair(new KeyConfig(keyName, password));
    }
  }

  Config loadConfig(Optional<File> configFile) {
    InputStream configAsStream;
    if (configFile.isPresent()) {
      log.info("using {} provided config file", configFile.get().getAbsolutePath());
      try {
        configAsStream = new FileInputStream(configFile.get());
      } catch (FileNotFoundException e) {
        throw new RuntimeException(e);
      }
    } else {
      log.warn("no config file provided, using default.conf");
      configAsStream = Orion.class.getResourceAsStream("/default.conf");
    }

    TomlConfigBuilder configBuilder = new TomlConfigBuilder();

    return configBuilder.build(configAsStream);
  }
}

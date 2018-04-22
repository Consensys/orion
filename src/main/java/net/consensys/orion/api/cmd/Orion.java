package net.consensys.orion.api.cmd;

import static io.vertx.core.Vertx.vertx;
import static java.nio.charset.StandardCharsets.UTF_8;
import static net.consensys.orion.api.network.HostAndFingerprintTrustManagerFactory.*;
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
import net.consensys.orion.api.network.HostAndFingerprintTrustManagerFactory;
import net.consensys.orion.api.network.HostFingerprintRepository;
import net.consensys.orion.api.network.TrustManagerFactoryWrapper;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.ResponseContentTypeHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Orion {

  private static final Logger log = LogManager.getLogger();
  public static final String name = "orion";

  static {
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");
  }

  private final Vertx vertx;
  private StorageEngine<EncryptedPayload> storageEngine;
  private NetworkDiscovery discovery;
  private HttpServer nodeHTTPServer;
  private HttpServer clientHTTPServer;

  public static void main(String[] args) {
    log.info("starting orion");
    Orion orion = new Orion();
    try {
      orion.run(System.out, System.err, args);
    } catch (OrionStartException | ConfigException e) {
      log.error(e.getMessage(), e.getCause());
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
      Router nodeRouter,
      Router clientRouter) {

    LoggerHandler loggerHandler = LoggerHandler.create();

    //Setup Orion node APIs
    nodeRouter
        .route()
        .handler(BodyHandler.create())
        .handler(loggerHandler)
        .handler(ResponseContentTypeHandler.create())
        .failureHandler(new HttpErrorHandler());

    nodeRouter.get("/upcheck").produces(TEXT.httpHeaderValue).handler(new UpcheckHandler());

    nodeRouter.post("/partyinfo").produces(CBOR.httpHeaderValue).consumes(CBOR.httpHeaderValue).handler(
        new PartyInfoHandler(networkNodes));

    nodeRouter.post("/push").produces(TEXT.httpHeaderValue).consumes(CBOR.httpHeaderValue).handler(
        new PushHandler(storage));

    //Setup client APIs
    clientRouter
        .route()
        .handler(BodyHandler.create())
        .handler(loggerHandler)
        .handler(ResponseContentTypeHandler.create())
        .failureHandler(new HttpErrorHandler());

    clientRouter.get("/upcheck").produces(TEXT.httpHeaderValue).handler(new UpcheckHandler());

    clientRouter.post("/send").produces(JSON.httpHeaderValue).consumes(JSON.httpHeaderValue).handler(
        new SendHandler(vertx, enclave, storage, networkNodes, JSON));
    clientRouter
        .post("/sendraw")
        .produces(APPLICATION_OCTET_STREAM.httpHeaderValue)
        .consumes(APPLICATION_OCTET_STREAM.httpHeaderValue)
        .handler(new SendHandler(vertx, enclave, storage, networkNodes, APPLICATION_OCTET_STREAM));

    clientRouter.post("/receive").produces(JSON.httpHeaderValue).consumes(JSON.httpHeaderValue).handler(
        new ReceiveHandler(enclave, storage, JSON));
    clientRouter
        .post("/receiveraw")
        .produces(APPLICATION_OCTET_STREAM.httpHeaderValue)
        .consumes(APPLICATION_OCTET_STREAM.httpHeaderValue)
        .handler(new ReceiveHandler(enclave, storage, APPLICATION_OCTET_STREAM));
  }

  public Orion() {
    this(vertx());
  }

  public Orion(Vertx vertx) {
    this.vertx = vertx;
  }

  private AtomicBoolean isRunning = new AtomicBoolean(false);

  public void stop() {
    if (!isRunning.compareAndSet(true, false)) {
      return;
    }
    CompletableFuture<Boolean> publicServerFuture = new CompletableFuture<>();
    CompletableFuture<Boolean> privateServerFuture = new CompletableFuture<>();
    CompletableFuture<Boolean> discoveryFuture = new CompletableFuture<>();
    nodeHTTPServer.close(result -> {
      if (result.succeeded()) {
        publicServerFuture.complete(true);
      } else {
        publicServerFuture.completeExceptionally(result.cause());
      }
    });
    clientHTTPServer.close(result -> {
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
    Config config;
    config = loadConfig(arguments.configFileName().map(fileName -> {
      Path configFile = Paths.get(fileName);
      if (!Files.exists(configFile)) {
        throw new OrionException(OrionErrorCode.CONFIG_FILE_MISSING);
      }
      return configFile;
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

    Path workDir = config.workDir();
    log.info("using working directory {}", workDir);

    try {
      Files.createDirectories(workDir);
    } catch (IOException ex) {
      throw new OrionStartException("Couldn't create working directory '" + workDir + "': " + ex.getMessage(), ex);
    }

    // create our storage engine
    storageEngine = createStorageEngine(config, workDir);

    // Vertx routers
    Router nodeRouter = Router.router(vertx).exceptionHandler(log::error);
    Router clientRouter = Router.router(vertx).exceptionHandler(log::error);

    // controller dependencies
    StorageKeyBuilder keyBuilder = new Sha512_256StorageKeyBuilder(enclave);
    EncryptedPayloadStorage storage = new EncryptedPayloadStorage(storageEngine, keyBuilder);
    configureRoutes(vertx, networkNodes, enclave, storage, nodeRouter, clientRouter);

    // asynchronously start the vertx http server for public API
    CompletableFuture<Boolean> nodeFuture = new CompletableFuture<>();
    HttpServerOptions options = new HttpServerOptions()
        .setPort(config.nodePort())
        .setHost(config.nodeNetworkInterface())
        .setCompressionSupported(true);
    if (!"off".equals(config.tls())) {
      options.setSsl(true);
      options.setClientAuth(ClientAuth.REQUIRED);

      PemKeyCertOptions pemKeyCertOptions =
          new PemKeyCertOptions().setKeyPath(config.tlsServerKey().toString()).setCertPath(
              config.tlsServerCert().toString());
      options.setPemKeyCertOptions(pemKeyCertOptions);


      Optional<Function<HostFingerprintRepository, HostAndFingerprintTrustManagerFactory>> tmfCreator =
          Optional.empty();
      if ("ca".equals(config.tlsServerTrust())) {
      } else if ("tofu".equals(config.tlsServerTrust())) {
        tmfCreator = Optional.of(HostAndFingerprintTrustManagerFactory::tofu);
      } else if ("whitelist".equals(config.tlsServerTrust())) {
        tmfCreator = Optional.of(HostAndFingerprintTrustManagerFactory::whitelist);
      } else if ("ca-or-tofu".equals(config.tlsServerTrust())) {
        tmfCreator = Optional.of(
            hostFingerprintRepository -> HostAndFingerprintTrustManagerFactory
                .caOrTofuDefaultJDKTruststore(hostFingerprintRepository, vertx));
      } else if ("insecure-no-validation".equals(config.tlsServerTrust())) {
        tmfCreator = Optional.of(HostAndFingerprintTrustManagerFactory::insecure);
      } else {
        throw new UnsupportedOperationException(config.tlsServerTrust() + " is not supported");
      }

      tmfCreator.ifPresent(tmf -> {
        try {
          HostFingerprintRepository hostFingerprintRepository = new HostFingerprintRepository(config.tlsKnownClients());
          options.setTrustOptions(new TrustManagerFactoryWrapper(tmf.apply(hostFingerprintRepository)));
        } catch (IOException e) {
          throw new OrionStartException("Could not read the contents of " + config.tlsKnownClients(), e);
        }
      });
    }

    nodeHTTPServer =
        vertx.createHttpServer(options).requestHandler(nodeRouter::accept).exceptionHandler(log::error).listen(
            completeFutureInHandler(nodeFuture));
    CompletableFuture<Boolean> clientFuture = new CompletableFuture<>();
    HttpServerOptions clientOptions =
        new HttpServerOptions().setPort(config.clientPort()).setHost(config.clientNetworkInterface());
    clientHTTPServer =
        vertx.createHttpServer(clientOptions).requestHandler(clientRouter::accept).exceptionHandler(log::error).listen(
            completeFutureInHandler(clientFuture));

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
      CompletableFuture.allOf(nodeFuture, clientFuture, verticleFuture).get();
    } catch (ExecutionException e) {
      throw new OrionStartException("Orion failed to start: " + e.getCause().getMessage(), e.getCause());
    } catch (InterruptedException e) {
      throw new OrionStartException("Orion was interrupted while starting services");
    }

    // set shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    isRunning.set(true);
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

  private StorageEngine<EncryptedPayload> createStorageEngine(Config config, Path storagePath) {
    String storage = config.storage();
    String dbDir = "routerdb";
    String[] storageOptions = storage.split(":", 2);
    if (storageOptions.length > 1) {
      dbDir = storageOptions[1];
    }

    Path dbPath = storagePath.resolve(dbDir);
    try {
      Files.createDirectories(dbPath);
    } catch (IOException ex) {
      throw new OrionStartException("Couldn't create storage path '" + dbPath + "': " + ex.getMessage(), ex);
    }

    if (storage.startsWith("mapdb")) {
      return new MapDbStorage<>(SodiumEncryptedPayload.class, dbPath);
    } else if (storage.startsWith("leveldb")) {
      return new LevelDbStorage<>(SodiumEncryptedPayload.class, dbPath);
    } else {
      throw new OrionStartException("unsupported storage mechanism: " + storage);
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

  Config loadConfig(Optional<Path> configFile) {
    InputStream configAsStream;
    if (configFile.isPresent()) {
      log.info("using {} provided config file", configFile.get().toAbsolutePath());
      try {
        configAsStream = Files.newInputStream(configFile.get());
      } catch (IOException ex) {
        throw new OrionStartException("Could not open " + configFile.get() + ": " + ex.getMessage(), ex);
      }
    } else {
      log.warn("no config file provided, using default");
      configAsStream = this.getClass().getResourceAsStream("/default.conf");
    }

    return new TomlConfigBuilder().build(configAsStream);
  }
}

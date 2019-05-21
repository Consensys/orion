/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.orion.cmd;

import static io.vertx.core.Vertx.vertx;
import static java.nio.charset.StandardCharsets.UTF_8;
import static net.consensys.orion.http.server.HttpContentType.APPLICATION_OCTET_STREAM;
import static net.consensys.orion.http.server.HttpContentType.CBOR;
import static net.consensys.orion.http.server.HttpContentType.JSON;
import static net.consensys.orion.http.server.HttpContentType.ORION;
import static net.consensys.orion.http.server.HttpContentType.TEXT;

import net.consensys.cava.crypto.sodium.Sodium;
import net.consensys.cava.kv.KeyValueStore;
import net.consensys.cava.kv.LevelDBKeyValueStore;
import net.consensys.cava.kv.MapDBKeyValueStore;
import net.consensys.cava.kv.SQLKeyValueStore;
import net.consensys.cava.net.tls.VertxTrustOptions;
import net.consensys.orion.config.Config;
import net.consensys.orion.config.ConfigException;
import net.consensys.orion.enclave.Enclave;
import net.consensys.orion.enclave.EncryptedPayload;
import net.consensys.orion.enclave.sodium.FileKeyStore;
import net.consensys.orion.enclave.sodium.SodiumEnclave;
import net.consensys.orion.http.handler.partyinfo.PartyInfoHandler;
import net.consensys.orion.http.handler.privacy.PrivacyGroupHandler;
import net.consensys.orion.http.handler.push.PushHandler;
import net.consensys.orion.http.handler.receive.ReceiveHandler;
import net.consensys.orion.http.handler.send.SendHandler;
import net.consensys.orion.http.handler.upcheck.UpcheckHandler;
import net.consensys.orion.http.server.vertx.HttpErrorHandler;
import net.consensys.orion.network.ConcurrentNetworkNodes;
import net.consensys.orion.network.NetworkDiscovery;
import net.consensys.orion.storage.EncryptedPayloadStorage;
import net.consensys.orion.storage.PrivacyGroupStorage;
import net.consensys.orion.storage.Sha512_256StorageKeyBuilder;
import net.consensys.orion.storage.Storage;
import net.consensys.orion.storage.StorageKeyBuilder;
import net.consensys.orion.utils.TLS;

import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;

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

  static final String NAME = "orion";
  private static final Logger log = LogManager.getLogger();

  static {
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");
  }

  static {
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
  }

  private final Vertx vertx;
  private KeyValueStore storage;
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
      Storage<String[]> privacyGroupStorage,
      Router nodeRouter,
      Router clientRouter,
      Config config) {

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
        new SendHandler(vertx, enclave, storage, privacyGroupStorage, networkNodes, JSON, config));
    clientRouter
        .post("/sendraw")
        .produces(APPLICATION_OCTET_STREAM.httpHeaderValue)
        .consumes(APPLICATION_OCTET_STREAM.httpHeaderValue)
        .handler(
            new SendHandler(
                vertx,
                enclave,
                storage,
                privacyGroupStorage,
                networkNodes,
                APPLICATION_OCTET_STREAM,
                config));

    clientRouter.post("/receive").produces(JSON.httpHeaderValue).consumes(JSON.httpHeaderValue).handler(
        new ReceiveHandler(enclave, storage, JSON));
    clientRouter.post("/receive").produces(ORION.httpHeaderValue).consumes(ORION.httpHeaderValue).handler(
        new ReceiveHandler(enclave, storage, ORION));
    clientRouter
        .post("/receiveraw")
        .produces(APPLICATION_OCTET_STREAM.httpHeaderValue)
        .consumes(APPLICATION_OCTET_STREAM.httpHeaderValue)
        .handler(new ReceiveHandler(enclave, storage, APPLICATION_OCTET_STREAM));
    clientRouter.post("/privacyGroupId").consumes(JSON.httpHeaderValue).produces(JSON.httpHeaderValue).handler(
        new PrivacyGroupHandler(privacyGroupStorage));
  }

  public Orion() {
    this(vertx());
  }

  public Orion(Vertx vertx) {
    this.vertx = vertx;
  }

  private final AtomicBoolean isRunning = new AtomicBoolean(false);

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

    if (storage != null) {
      try {
        storage.close();
      } catch (IOException e) {
        log.error("Error closing storage", e);
      }
    }
  }

  public void run(PrintStream out, PrintStream err, String... args) {
    // parsing arguments
    OrionArguments arguments = new OrionArguments(out, err, args);

    if (arguments.argumentExit()) {
      return;
    }

    // load config file
    Config config = loadConfig(arguments.configFileName().map(Paths::get).orElse(null));

    // generate key pair and exit
    if (arguments.keysToGenerate().isPresent()) {
      try {
        generateKeyPairs(out, err, config, arguments.keysToGenerate().get());
      } catch (IOException ex) {
        throw new OrionStartException(ex.getMessage(), ex);
      }
      return;
    }

    run(out, err, config);
  }

  public void run(PrintStream out, PrintStream err, Config config) {
    Path libSodiumPath = config.libSodiumPath();
    if (libSodiumPath != null) {
      Sodium.loadLibrary(libSodiumPath);
    }

    FileKeyStore keyStore;
    try {
      keyStore = new FileKeyStore(config);
    } catch (IOException ex) {
      throw new OrionStartException(ex.getMessage(), ex);
    }
    ConcurrentNetworkNodes networkNodes = new ConcurrentNetworkNodes(config, keyStore.nodeKeys());

    Enclave enclave = new SodiumEnclave(keyStore);

    Path workDir = config.workDir();
    log.info("using working directory {}", workDir);

    try {
      Files.createDirectories(workDir);
    } catch (IOException ex) {
      throw new OrionStartException("Couldn't create working directory '" + workDir + "': " + ex.getMessage(), ex);
    }

    if (!"off".equals(config.tls())) {
      // verify server TLS cert and key
      Path tlsServerCert = config.tlsServerCert();
      Path tlsServerKey = config.tlsServerKey();

      try {
        TLS.createSelfSignedCertificateIfMissing(tlsServerKey, tlsServerCert, config);
      } catch (IOException e) {
        throw new OrionStartException(
            "An error occurred while writing the server TLS certificate files: " + e.getMessage(),
            e);
      }
      if (!Files.exists(tlsServerCert)) {
        throw new OrionStartException("Missing server TLS certificate file \"" + tlsServerCert + "\"");
      } else if (!Files.exists(tlsServerKey)) {
        throw new OrionStartException("Missing server TLS key file \"" + tlsServerKey + "\"");
      }

      // verify client TLS cert and key
      Path tlsClientCert = config.tlsClientCert();
      Path tlsClientKey = config.tlsClientKey();

      try {
        TLS.createSelfSignedCertificateIfMissing(tlsClientKey, tlsClientCert, config);
      } catch (IOException e) {
        throw new OrionStartException(
            "An error occurred while writing the client TLS certificate files: " + e.getMessage(),
            e);
      }
      if (!Files.exists(tlsClientCert)) {
        throw new OrionStartException("Missing client TLS certificate file \"" + tlsClientCert + "\"");
      } else if (!Files.exists(tlsClientKey)) {
        throw new OrionStartException("Missing client TLS key file \"" + tlsClientKey + "\"");
      }
    }

    // create our storage engine
    storage = createStorage(config.storage(), workDir);

    // Vertx routers
    Router nodeRouter = Router.router(vertx).exceptionHandler(log::error);
    Router clientRouter = Router.router(vertx).exceptionHandler(log::error);

    // controller dependencies
    StorageKeyBuilder keyBuilder = new Sha512_256StorageKeyBuilder();
    EncryptedPayloadStorage encryptedStorage = new EncryptedPayloadStorage(storage, keyBuilder);
    PrivacyGroupStorage privacyGroupStorage = new PrivacyGroupStorage(storage, enclave);
    configureRoutes(
        vertx,
        networkNodes,
        enclave,
        encryptedStorage,
        privacyGroupStorage,
        nodeRouter,
        clientRouter,
        config);

    // asynchronously start the vertx http server for public API
    CompletableFuture<Boolean> nodeFuture = new CompletableFuture<>();
    HttpServerOptions options = new HttpServerOptions()
        .setPort(config.nodePort())
        .setHost(config.nodeNetworkInterface())
        .setCompressionSupported(true);

    if (!"off".equals(config.tls())) {
      Path tlsServerCert = workDir.resolve(config.tlsServerCert());
      Path tlsServerKey = workDir.resolve(config.tlsServerKey());
      PemKeyCertOptions pemKeyCertOptions =
          new PemKeyCertOptions().setKeyPath(tlsServerKey.toString()).setCertPath(tlsServerCert.toString());
      for (Path chainCert : config.tlsServerChain()) {
        pemKeyCertOptions.addCertPath(chainCert.toAbsolutePath().toString());
      }

      options.setSsl(true);
      options.setClientAuth(ClientAuth.REQUIRED);
      options.setPemKeyCertOptions(pemKeyCertOptions);

      Path knownClientsFile = config.tlsKnownClients();
      String serverTrustMode = config.tlsServerTrust().toLowerCase();
      switch (serverTrustMode) {
        case "whitelist":
          options.setTrustOptions(VertxTrustOptions.whitelistClients(knownClientsFile, false));
          break;
        case "ca":
          // use default trust options
          break;
        case "ca-or-whitelist":
          options.setTrustOptions(VertxTrustOptions.whitelistClients(knownClientsFile, true));
          break;
        case "tofu":
        case "insecure-tofa":
          options.setTrustOptions(VertxTrustOptions.trustClientOnFirstAccess(knownClientsFile, false));
          break;
        case "ca-or-tofu":
        case "insecure-ca-or-tofa":
          options.setTrustOptions(VertxTrustOptions.trustClientOnFirstAccess(knownClientsFile, true));
          break;
        case "insecure-no-validation":
        case "insecure-record":
          options.setTrustOptions(VertxTrustOptions.recordClientFingerprints(knownClientsFile, false));
          break;
        case "insecure-ca-or-record":
          options.setTrustOptions(VertxTrustOptions.recordClientFingerprints(knownClientsFile, true));
          break;
        default:
          throw new UnsupportedOperationException(
              "\"" + serverTrustMode + "\" option for tlsservertrust is not supported");
      }
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

    CompletableFuture<Boolean> verticleFuture = new CompletableFuture<>();
    // start network discovery of other peers
    discovery = new NetworkDiscovery(networkNodes, config);
    vertx.deployVerticle(discovery, result -> {
      if (result.succeeded()) {
        verticleFuture.complete(true);
      } else {
        verticleFuture.completeExceptionally(result.cause());
      }
    });

    try {
      CompletableFuture.allOf(nodeFuture, clientFuture, verticleFuture).get();
      // if there is not a node url in the config, then grab the actual port and use it to set the node url.
      if (!config.nodeUrl().isPresent()) {
        networkNodes.setNodeUrl(
            new URL("http", config.nodeNetworkInterface(), nodeHTTPServer.actualPort(), ""),
            keyStore.nodeKeys());
      }
    } catch (ExecutionException | MalformedURLException e) {
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

  private KeyValueStore createStorage(String storage, Path storagePath) {
    String db = "routerdb";
    String[] storageOptions = storage.split(":", 2);
    if (storageOptions.length > 1) {
      db = storageOptions[1];
    }

    if (storage.toLowerCase().startsWith("mapdb")) {
      try {
        return MapDBKeyValueStore.open(storagePath.resolve(db));
      } catch (IOException e) {
        throw new OrionStartException("Couldn't create MapDB store: " + db, e);
      }
    } else if (storage.toLowerCase().startsWith("leveldb")) {
      try {
        return LevelDBKeyValueStore.open(storagePath.resolve(db));
      } catch (IOException e) {
        throw new OrionStartException("Couldn't create LevelDB store: " + db, e);
      }
    } else if (storage.toLowerCase().startsWith("sql")) {
      try {
        return SQLKeyValueStore.open(db);
      } catch (IOException e) {
        throw new OrionStartException("Couldn't create SQL-backed store: " + db, e);
      }
    } else {
      throw new OrionStartException("unsupported storage mechanism: " + storage);
    }
  }

  @SuppressWarnings("unused")
  private void generateKeyPairs(PrintStream out, PrintStream err, Config config, String[] keysToGenerate)
      throws IOException {
    log.info("generating Key Pairs");

    Path libSodiumPath = config.libSodiumPath();
    if (libSodiumPath != null) {
      Sodium.loadLibrary(libSodiumPath);
    }
    FileKeyStore keyStore = new FileKeyStore(config);

    Scanner scanner = new Scanner(System.in, UTF_8.name());

    for (String keyName : keysToGenerate) {
      Path basePath = Paths.get(keyName);

      //Prompt for Password from user
      out.format("Enter password for key pair [%s] : ", keyName);
      String pwd = scanner.nextLine().trim();
      keyStore.generateKeyPair(basePath, pwd.length() > 0 ? pwd : null);
    }
  }

  private static Config loadConfig(@Nullable Path configFile) {
    if (configFile == null) {
      log.warn("no config file provided, using default");
      return Config.defaultConfig();
    }
    log.info("using {} provided config file", configFile.toAbsolutePath());
    try {
      return Config.load(configFile);
    } catch (IOException e) {
      throw new OrionStartException("Could not open '" + configFile.toAbsolutePath() + "': " + e.getMessage(), e);
    }
  }

  public int nodePort() {
    return nodeHTTPServer.actualPort();
  }

  public int clientPort() {
    return clientHTTPServer.actualPort();
  }

  public void addPeer(final URL url) {
    discovery.addPeer(url);
  }
}

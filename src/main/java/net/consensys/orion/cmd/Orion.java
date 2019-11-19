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
import net.consensys.cava.net.tls.VertxTrustOptions;
import net.consensys.orion.config.Config;
import net.consensys.orion.config.ConfigException;
import net.consensys.orion.enclave.Enclave;
import net.consensys.orion.enclave.EncryptedPayload;
import net.consensys.orion.enclave.PrivacyGroupPayload;
import net.consensys.orion.enclave.QueryPrivacyGroupPayload;
import net.consensys.orion.enclave.sodium.FileKeyStore;
import net.consensys.orion.enclave.sodium.SodiumEnclave;
import net.consensys.orion.http.handler.knownnodes.KnownNodesHandler;
import net.consensys.orion.http.handler.partyinfo.PartyInfoHandler;
import net.consensys.orion.http.handler.privacy.CreatePrivacyGroupHandler;
import net.consensys.orion.http.handler.privacy.DeletePrivacyGroupHandler;
import net.consensys.orion.http.handler.privacy.FindPrivacyGroupHandler;
import net.consensys.orion.http.handler.push.PushHandler;
import net.consensys.orion.http.handler.push.PushPrivacyGroupHandler;
import net.consensys.orion.http.handler.receive.ReceiveHandler;
import net.consensys.orion.http.handler.send.SendHandler;
import net.consensys.orion.http.handler.sendraw.SendRawHandler;
import net.consensys.orion.http.handler.upcheck.UpcheckHandler;
import net.consensys.orion.http.server.vertx.HttpErrorHandler;
import net.consensys.orion.network.ConcurrentNetworkNodes;
import net.consensys.orion.network.NetworkDiscovery;
import net.consensys.orion.payload.DistributePayloadManager;
import net.consensys.orion.storage.EncryptedPayloadStorage;
import net.consensys.orion.storage.JpaEntityManagerProvider;
import net.consensys.orion.storage.OrionSQLKeyValueStore;
import net.consensys.orion.storage.PrivacyGroupStorage;
import net.consensys.orion.storage.QueryPrivacyGroupStorage;
import net.consensys.orion.storage.Sha512_256StorageKeyBuilder;
import net.consensys.orion.storage.Storage;
import net.consensys.orion.storage.StorageKeyBuilder;
import net.consensys.orion.utils.TLS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.util.Properties;
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
import io.vertx.core.net.PemTrustOptions;
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

  public static void main(final String[] args) {
    log.info("starting orion");
    final Orion orion = new Orion();
    try {
      orion.run(System.out, System.err, args);
    } catch (final OrionStartException | ConfigException e) {
      log.error(e.getMessage(), e.getCause());
      System.err.println(e.getMessage());
      System.exit(1);
    } catch (final Throwable t) {
      log.error("Unexpected exception upon starting Orion", t);
      System.err.println(
          "An unexpected exception was reported while starting Orion. Please refer to the logs for more information");
      System.exit(1);
    }
  }

  public static void configureRoutes(
      final Vertx vertx,
      final ConcurrentNetworkNodes networkNodes,
      final Enclave enclave,
      final Storage<EncryptedPayload> storage,
      final Storage<PrivacyGroupPayload> privacyGroupStorage,
      final Storage<QueryPrivacyGroupPayload> queryPrivacyGroupStorage,
      final DistributePayloadManager distributePayloadManager,
      final Router nodeRouter,
      final Router clientRouter,
      final Config config) {

    final LoggerHandler loggerHandler = LoggerHandler.create();

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

    nodeRouter.post("/pushPrivacyGroup").produces(TEXT.httpHeaderValue).consumes(CBOR.httpHeaderValue).handler(
        new PushPrivacyGroupHandler(privacyGroupStorage, queryPrivacyGroupStorage));

    //Setup client APIs
    clientRouter
        .route()
        .handler(BodyHandler.create())
        .handler(loggerHandler)
        .handler(ResponseContentTypeHandler.create())
        .failureHandler(new HttpErrorHandler());

    clientRouter.get("/upcheck").produces(TEXT.httpHeaderValue).handler(new UpcheckHandler());
    clientRouter.get("/peercount").produces(TEXT.httpHeaderValue).handler(
        new PeerCountHandler(() -> networkNodes.nodeURLs().size()));

    clientRouter.post("/send").produces(JSON.httpHeaderValue).consumes(JSON.httpHeaderValue).handler(
        new SendHandler(distributePayloadManager));

    clientRouter
        .post("/sendraw")
        .produces(APPLICATION_OCTET_STREAM.httpHeaderValue)
        .consumes(APPLICATION_OCTET_STREAM.httpHeaderValue)
        .handler(new SendRawHandler(distributePayloadManager));

    clientRouter.post("/receive").produces(JSON.httpHeaderValue).consumes(JSON.httpHeaderValue).handler(
        new ReceiveHandler(enclave, storage, JSON));
    clientRouter.post("/receive").produces(ORION.httpHeaderValue).consumes(ORION.httpHeaderValue).handler(
        new ReceiveHandler(enclave, storage, ORION));
    clientRouter
        .post("/receiveraw")
        .produces(APPLICATION_OCTET_STREAM.httpHeaderValue)
        .consumes(APPLICATION_OCTET_STREAM.httpHeaderValue)
        .handler(new ReceiveHandler(enclave, storage, APPLICATION_OCTET_STREAM));

    clientRouter.post("/createPrivacyGroup").consumes(JSON.httpHeaderValue).produces(JSON.httpHeaderValue).handler(
        new CreatePrivacyGroupHandler(
            privacyGroupStorage,
            queryPrivacyGroupStorage,
            networkNodes,
            enclave,
            vertx,
            config));

    clientRouter.post("/deletePrivacyGroup").consumes(JSON.httpHeaderValue).produces(JSON.httpHeaderValue).handler(
        new DeletePrivacyGroupHandler(
            privacyGroupStorage,
            queryPrivacyGroupStorage,
            networkNodes,
            enclave,
            vertx,
            config));

    clientRouter.post("/findPrivacyGroup").consumes(JSON.httpHeaderValue).produces(JSON.httpHeaderValue).handler(
        new FindPrivacyGroupHandler(queryPrivacyGroupStorage, privacyGroupStorage));

    clientRouter.get("/knownnodes").produces(JSON.httpHeaderValue).handler(new KnownNodesHandler(networkNodes));
  }

  public Orion() {
    this(vertx());
  }

  public Orion(final Vertx vertx) {
    this.vertx = vertx;
  }

  private final AtomicBoolean isRunning = new AtomicBoolean(false);

  public void stop() {
    if (!isRunning.compareAndSet(true, false)) {
      return;
    }
    final CompletableFuture<Boolean> publicServerFuture = new CompletableFuture<>();
    final CompletableFuture<Boolean> privateServerFuture = new CompletableFuture<>();
    final CompletableFuture<Boolean> discoveryFuture = new CompletableFuture<>();
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
      final Future<Void> future = Future.future();
      future.setHandler(result -> {
        if (result.succeeded()) {
          discoveryFuture.complete(true);
        } else {
          discoveryFuture.completeExceptionally(result.cause());
        }
      });
      discovery.stop(future);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }

    try {
      CompletableFuture.allOf(publicServerFuture, privateServerFuture, discoveryFuture).get();
    } catch (final InterruptedException | ExecutionException e) {
      log.error("Error stopping vert.x HTTP servers and discovery", e);
    }

    final CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();

    vertx.close(result -> {
      if (result.succeeded()) {
        resultFuture.complete(true);
      } else {
        resultFuture.completeExceptionally(result.cause());
      }
    });

    try {
      resultFuture.get();
    } catch (final InterruptedException | ExecutionException io) {
      log.error("Error stopping vert.x", io);
    }

    if (storage != null) {
      try {
        storage.close();
      } catch (final IOException e) {
        log.error("Error closing storage", e);
      }
    }
  }

  public void run(final PrintStream out, final PrintStream err, final String... args) {
    // parsing arguments
    final OrionArguments arguments = new OrionArguments(out, err, args);

    if (arguments.argumentExit()) {
      return;
    }

    // load config file
    final Config config = loadConfig(arguments.configFileName().map(Paths::get).orElse(null));

    // generate key pair and exit
    if (arguments.keysToGenerate().isPresent()) {
      try {
        generateKeyPairs(out, err, config, arguments.keysToGenerate().get());
      } catch (final IOException ex) {
        throw new OrionStartException(ex.getMessage(), ex);
      }
      return;
    }

    run(out, err, config);
  }

  public void run(final PrintStream out, final PrintStream err, final Config config) {
    final Path libSodiumPath = config.libSodiumPath();
    if (libSodiumPath != null) {
      Sodium.loadLibrary(libSodiumPath);
    }

    final FileKeyStore keyStore;
    try {
      keyStore = new FileKeyStore(config);
    } catch (final IOException ex) {
      throw new OrionStartException(ex.getMessage(), ex);
    }
    final ConcurrentNetworkNodes networkNodes = new ConcurrentNetworkNodes(config, keyStore.nodeKeys());

    final Enclave enclave = new SodiumEnclave(keyStore);

    final Path workDir = config.workDir();
    log.info("using working directory {}", workDir);

    try {
      Files.createDirectories(workDir);
    } catch (final IOException ex) {
      throw new OrionStartException("Couldn't create working directory '" + workDir + "': " + ex.getMessage(), ex);
    }

    if (!"off".equals(config.tls())) {
      // verify server TLS cert and key
      final Path tlsServerCert = config.tlsServerCert();
      final Path tlsServerKey = config.tlsServerKey();

      try {
        TLS.createSelfSignedCertificateIfMissing(tlsServerKey, tlsServerCert, config);
      } catch (final IOException e) {
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
      final Path tlsClientCert = config.tlsClientCert();
      final Path tlsClientKey = config.tlsClientKey();

      try {
        TLS.createSelfSignedCertificateIfMissing(tlsClientKey, tlsClientCert, config);
      } catch (final IOException e) {
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
    final Router nodeRouter = Router.router(vertx).exceptionHandler(log::error);
    final Router clientRouter = Router.router(vertx).exceptionHandler(log::error);

    // controller dependencies
    final StorageKeyBuilder keyBuilder = new Sha512_256StorageKeyBuilder();
    final EncryptedPayloadStorage encryptedStorage = new EncryptedPayloadStorage(storage, keyBuilder);
    final QueryPrivacyGroupStorage queryPrivacyGroupStorage = new QueryPrivacyGroupStorage(storage, enclave);
    final PrivacyGroupStorage privacyGroupStorage = new PrivacyGroupStorage(storage, enclave);
    final DistributePayloadManager distributePayloadManager = new DistributePayloadManager(
        vertx,
        config,
        enclave,
        encryptedStorage,
        privacyGroupStorage,
        queryPrivacyGroupStorage,
        networkNodes);

    configureRoutes(
        vertx,
        networkNodes,
        enclave,
        encryptedStorage,
        privacyGroupStorage,
        queryPrivacyGroupStorage,
        distributePayloadManager,
        nodeRouter,
        clientRouter,
        config);

    // asynchronously start the vertx http server for public API
    final CompletableFuture<Boolean> nodeFuture = new CompletableFuture<>();
    final HttpServerOptions options = new HttpServerOptions()
        .setPort(config.nodePort())
        .setHost(config.nodeNetworkInterface())
        .setCompressionSupported(true);

    if (!"off".equals(config.tls())) {
      final Path tlsServerCert = workDir.resolve(config.tlsServerCert());
      final Path tlsServerKey = workDir.resolve(config.tlsServerKey());
      final PemKeyCertOptions pemKeyCertOptions =
          new PemKeyCertOptions().setKeyPath(tlsServerKey.toString()).setCertPath(tlsServerCert.toString());

      options.setSsl(true);
      options.setClientAuth(ClientAuth.REQUIRED);
      options.setPemKeyCertOptions(pemKeyCertOptions);

      if (!config.tlsServerChain().isEmpty()) {
        final PemTrustOptions pemTrustOptions = new PemTrustOptions();
        for (final Path chainCert : config.tlsServerChain()) {
          pemTrustOptions.addCertPath(chainCert.toAbsolutePath().toString());
        }
        options.setPemTrustOptions(pemTrustOptions);
      }

      final Path knownClientsFile = config.tlsKnownClients();
      final String serverTrustMode = config.tlsServerTrust().toLowerCase();
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

    try {
      nodeHTTPServer =
          vertx.createHttpServer(options).requestHandler(nodeRouter::accept).exceptionHandler(log::error).listen(
              completeFutureInHandler(nodeFuture));
      final CompletableFuture<Boolean> clientFuture = new CompletableFuture<>();
      final HttpServerOptions clientOptions =
          new HttpServerOptions().setPort(config.clientPort()).setHost(config.clientNetworkInterface());
      clientHTTPServer = vertx
          .createHttpServer(clientOptions)
          .requestHandler(clientRouter::accept)
          .exceptionHandler(log::error)
          .listen(completeFutureInHandler(clientFuture));

      // wait for node and client http server to start successfully
      CompletableFuture.allOf(nodeFuture, clientFuture).get();
      // if there is not a node url in the config, then grab the actual port and use it to set the node url.
      if (!config.nodeUrl().isPresent()) {
        networkNodes.setNodeUrl(
            new URL("http", config.nodeNetworkInterface(), nodeHTTPServer.actualPort(), ""),
            keyStore.nodeKeys());
      }

      final CompletableFuture<Boolean> networkDiscoveryFuture = new CompletableFuture<>();
      // start network discovery of other peers
      discovery = new NetworkDiscovery(networkNodes, config);
      vertx.deployVerticle(discovery, result -> {
        if (result.succeeded()) {
          networkDiscoveryFuture.complete(true);
        } else {
          networkDiscoveryFuture.completeExceptionally(result.cause());
        }
      });
      CompletableFuture.allOf(networkDiscoveryFuture).get();
    } catch (final ExecutionException | MalformedURLException e) {
      throw new OrionStartException("Orion failed to start: " + e.getCause().getMessage(), e.getCause());
    } catch (final InterruptedException e) {
      throw new OrionStartException("Orion was interrupted while starting services");
    }

    //write acutal ports to a ports file
    writePortsToFile(config, nodeHTTPServer.actualPort(), clientHTTPServer.actualPort());

    // set shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    isRunning.set(true);
  }

  private void writePortsToFile(final Config config, final int nodePort, final int clientPort) {
    final Path dataPath = config.workDir();
    final File portsFile = new File(dataPath.toFile(), "orion.ports");
    portsFile.deleteOnExit();

    final Properties properties = new Properties();
    properties.setProperty("http-node-port", String.valueOf(nodePort));
    properties.setProperty("http-client-port", String.valueOf(clientPort));

    log.info("Writing orion.ports file: {}, with contents: {}", portsFile.getAbsolutePath(), properties);
    try (final FileOutputStream fileOutputStream = new FileOutputStream(portsFile)) {
      properties.store(
          fileOutputStream,
          "This file contains the ports used by the running instance of Pantheon. This file will be deleted after the node is shutdown.");
    } catch (final Exception e) {
      log.warn("Error writing ports file", e);
    }
  }

  private Handler<AsyncResult<HttpServer>> completeFutureInHandler(final CompletableFuture<Boolean> future) {
    return result -> {
      if (result.succeeded()) {
        future.complete(true);
      } else {
        future.completeExceptionally(result.cause());
      }
    };
  }

  private KeyValueStore createStorage(final String storage, final Path storagePath) {
    String db = "routerdb";
    final String[] storageOptions = storage.split(":", 2);
    if (storageOptions.length > 1) {
      db = storageOptions[1];
    }

    if (storage.toLowerCase().startsWith("mapdb")) {
      try {
        return MapDBKeyValueStore.open(storagePath.resolve(db));
      } catch (final IOException e) {
        throw new OrionStartException("Couldn't create MapDB store: " + db, e);
      }
    } else if (storage.toLowerCase().startsWith("leveldb")) {
      try {
        return LevelDBKeyValueStore.open(storagePath.resolve(db));
      } catch (final IOException e) {
        throw new OrionStartException("Couldn't create LevelDB store: " + db, e);
      }
    } else if (storage.toLowerCase().startsWith("sql")) {
      final JpaEntityManagerProvider jpaEntityManagerProvider = new JpaEntityManagerProvider(db);
      return new OrionSQLKeyValueStore(jpaEntityManagerProvider);
    } else {
      throw new OrionStartException("unsupported storage mechanism: " + storage);
    }
  }

  @SuppressWarnings("unused")
  private void generateKeyPairs(
      final PrintStream out,
      final PrintStream err,
      final Config config,
      final String[] keysToGenerate) throws IOException {
    log.info("generating Key Pairs");

    final Path libSodiumPath = config.libSodiumPath();
    if (libSodiumPath != null) {
      Sodium.loadLibrary(libSodiumPath);
    }
    final FileKeyStore keyStore = new FileKeyStore(config);

    final Scanner scanner = new Scanner(System.in, UTF_8.name());

    for (final String keyName : keysToGenerate) {
      final Path basePath = Paths.get(keyName);

      //Prompt for Password from user
      out.format("Enter password for key pair [%s] : ", keyName);
      final String pwd = scanner.nextLine().trim();
      keyStore.generateKeyPair(basePath, pwd.length() > 0 ? pwd : null);
    }
  }

  private static Config loadConfig(@Nullable final Path configFile) {
    if (configFile == null) {
      log.warn("no config file provided, using default");
      return Config.defaultConfig();
    }
    log.info("using {} provided config file", configFile.toAbsolutePath());
    try {
      return Config.load(configFile);
    } catch (final IOException e) {
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

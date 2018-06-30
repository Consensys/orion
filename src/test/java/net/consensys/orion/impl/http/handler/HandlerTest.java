package net.consensys.orion.impl.http.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.concurrent.CompletableAsyncCompletion;
import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;
import net.consensys.cava.kv.KeyValueStore;
import net.consensys.cava.kv.MapDBKeyValueStore;
import net.consensys.orion.api.cmd.Orion;
import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.enclave.Enclave;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.api.storage.Storage;
import net.consensys.orion.api.storage.StorageKeyBuilder;
import net.consensys.orion.impl.helpers.StubEnclave;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.network.ConcurrentNetworkNodes;
import net.consensys.orion.impl.storage.EncryptedPayloadStorage;
import net.consensys.orion.impl.storage.Sha512_256StorageKeyBuilder;
import net.consensys.orion.impl.utils.Serializer;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import okhttp3.HttpUrl;
import okhttp3.HttpUrl.Builder;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
abstract class HandlerTest {

  // http client
  OkHttpClient httpClient = new OkHttpClient();
  String nodeBaseUrl;
  String clientBaseUrl;

  // these are re-built between tests
  ConcurrentNetworkNodes networkNodes;
  protected Config config;
  protected Enclave enclave;

  private Vertx vertx;
  private Integer nodeHTTPServerPort;
  private HttpServer nodeHttpServer;
  private Integer clientHTTPServerPort;
  private HttpServer clientHttpServer;

  private KeyValueStore storage;
  protected Storage<EncryptedPayload> payloadStorage;

  @BeforeEach
  void setUp(@TempDirectory Path tempDir) throws Exception {
    // Setup ports for Public and Private API Servers
    setupPorts();

    // Initialize the base HTTP url in two forms: String and OkHttp's HttpUrl object to allow for simpler composition
    // of complex URLs with path parameters, query strings, etc.
    HttpUrl nodeHTTP = new Builder().scheme("http").host("localhost").port(nodeHTTPServerPort).build();
    nodeBaseUrl = nodeHTTP.toString();

    // orion dependencies, reset them all between tests
    config = Config.load("tls='off'\nworkdir=\"" + tempDir + "\"");
    networkNodes = new ConcurrentNetworkNodes(nodeHTTP.url());
    enclave = buildEnclave(tempDir);

    Path path = tempDir.resolve("routerdb");
    storage = new MapDBKeyValueStore(path);
    // create our vertx object
    vertx = Vertx.vertx();
    StorageKeyBuilder keyBuilder = new Sha512_256StorageKeyBuilder();
    payloadStorage = new EncryptedPayloadStorage(storage, keyBuilder);
    Router publicRouter = Router.router(vertx);
    Router privateRouter = Router.router(vertx);
    Orion.configureRoutes(vertx, networkNodes, enclave, payloadStorage, publicRouter, privateRouter, config);

    setupNodeServer(publicRouter);
    setupClientServer(privateRouter);
  }

  private void setupNodeServer(Router router) throws Exception {
    HttpServerOptions publicServerOptions = new HttpServerOptions();
    publicServerOptions.setPort(nodeHTTPServerPort);

    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    nodeHttpServer = vertx.createHttpServer(publicServerOptions).requestHandler(router::accept).listen(result -> {
      if (result.succeeded()) {
        completion.complete();
      } else {
        completion.completeExceptionally(result.cause());
      }
    });
    completion.join();
  }

  private void setupClientServer(Router router) throws Exception {
    HttpUrl clientHTTP = new Builder().scheme("http").host("localhost").port(clientHTTPServerPort).build();
    clientBaseUrl = clientHTTP.toString();

    HttpServerOptions privateServerOptions = new HttpServerOptions();
    privateServerOptions.setPort(clientHTTPServerPort);

    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    clientHttpServer = vertx.createHttpServer(privateServerOptions).requestHandler(router::accept).listen(result -> {
      if (result.succeeded()) {
        completion.complete();
      } else {
        completion.completeExceptionally(result.cause());
      }
    });
    completion.join();
  }

  private void setupPorts() throws IOException {
    // get a free httpServerPort for Public API
    ServerSocket socket1 = new ServerSocket(0);
    nodeHTTPServerPort = socket1.getLocalPort();

    // get a free httpServerPort for Private API
    ServerSocket socket2 = new ServerSocket(0);
    clientHTTPServerPort = socket2.getLocalPort();

    socket1.close();
    socket2.close();
  }

  @AfterEach
  void tearDown() throws Exception {
    nodeHttpServer.close();
    clientHttpServer.close();
    storage.close();
    vertx.close();
  }

  protected Enclave buildEnclave(Path tempDir) {
    return new StubEnclave();
  }

  Request buildPrivateAPIRequest(String path, HttpContentType contentType, Object payload) {
    return buildPostRequest(clientBaseUrl, path, contentType, Serializer.serialize(contentType, payload));
  }

  Request buildPublicAPIRequest(String path, HttpContentType contentType, Object payload) {
    return buildPostRequest(nodeBaseUrl, path, contentType, Serializer.serialize(contentType, payload));
  }

  private Request buildPostRequest(String baseurl, String path, HttpContentType contentType, byte[] payload) {
    RequestBody body = RequestBody.create(MediaType.parse(contentType.httpHeaderValue), payload);

    if (path.startsWith("/")) {
      path = path.substring(1, path.length());
    }

    return new Request.Builder().post(body).url(baseurl + path).build();
  }

  void assertError(final OrionErrorCode expected, final Response actual) throws IOException {
    assertEquals(String.format("{\"error\":\"%s\"}", expected.code()), actual.body().string());
  }
}

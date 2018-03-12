package net.consensys.orion.impl.http.handlers;

import net.consensys.orion.api.cmd.OrionRoutes;
import net.consensys.orion.api.enclave.Enclave;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.storage.StorageEngine;
import net.consensys.orion.impl.config.MemoryConfig;
import net.consensys.orion.impl.enclave.sodium.LibSodiumSettings;
import net.consensys.orion.impl.enclave.sodium.SodiumEncryptedPayload;
import net.consensys.orion.impl.helpers.CesarEnclave;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.http.server.vertx.VertxServer;
import net.consensys.orion.impl.network.MemoryNetworkNodes;
import net.consensys.orion.impl.storage.file.MapDbStorage;
import net.consensys.orion.impl.utils.Serializer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import okhttp3.HttpUrl;
import okhttp3.HttpUrl.Builder;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.junit.After;
import org.junit.Before;

public abstract class HandlerTest {
  protected final Serializer serializer = new Serializer();

  // http client
  protected OkHttpClient httpClient = new OkHttpClient();
  protected String publicBaseUrl;
  protected String privateBaseUrl;

  // these are re-built between tests
  protected MemoryNetworkNodes networkNodes;
  protected MemoryConfig config;
  protected Enclave enclave;

  protected Vertx vertx;
  protected Integer publicHTTPServerPort;
  protected VertxServer publicVertxServer;
  protected Integer privateHTTPServerPort;
  protected VertxServer privateVertxServer;
  protected OrionRoutes routes;

  private StorageEngine<EncryptedPayload> storageEngine;

  @Before
  public void setUp() throws Exception {

    // Setup ports for Public and Private API Servers
    setupPorts();

    // Initialise the base HTTP url in two forms: String and OkHttp's HttpUrl object to allow for simpler composition
    // of complex URLs with path parameters, query strings, etc.
    HttpUrl publicHTTP =
        new Builder()
            .scheme("http")
            .host(InetAddress.getLocalHost().getHostAddress())
            .port(publicHTTPServerPort)
            .build();
    publicBaseUrl = publicHTTP.toString();

    // orion dependencies, reset them all between tests
    config = new MemoryConfig();
    config.setLibSodiumPath(LibSodiumSettings.defaultLibSodiumPath());
    networkNodes = new MemoryNetworkNodes(publicHTTP.url());
    enclave = buildEnclave();

    storageEngine = new MapDbStorage(SodiumEncryptedPayload.class, "routerdb", serializer);
    routes = new OrionRoutes(vertx, networkNodes, serializer, enclave, storageEngine);

    // create our vertx object
    vertx = Vertx.vertx();

    setupPublicAPIServer();
    setupPrivateAPIServer();
  }

  private void setupPublicAPIServer()
      throws InterruptedException, java.util.concurrent.ExecutionException {
    HttpServerOptions publicServerOptions = new HttpServerOptions();
    publicServerOptions.setPort(publicHTTPServerPort);

    publicVertxServer = new VertxServer(vertx, routes.getPublicRouter(), publicServerOptions);
    publicVertxServer.start().get();
  }

  private void setupPrivateAPIServer()
      throws UnknownHostException, InterruptedException, java.util.concurrent.ExecutionException {
    HttpUrl privateHTTP =
        new Builder()
            .scheme("http")
            .host(InetAddress.getLocalHost().getHostAddress())
            .port(privateHTTPServerPort)
            .build();
    privateBaseUrl = privateHTTP.toString();

    HttpServerOptions privateServerOptions = new HttpServerOptions();
    privateServerOptions.setPort(privateHTTPServerPort);

    privateVertxServer = new VertxServer(vertx, routes.getPrivateRouter(), privateServerOptions);

    privateVertxServer.start().get();
  }

  private void setupPorts() throws IOException {
    // get a free httpServerPort for Public API
    ServerSocket socket1 = new ServerSocket(0);
    publicHTTPServerPort = socket1.getLocalPort();

    // get a free httpServerPort for Private API
    ServerSocket socket2 = new ServerSocket(0);
    privateHTTPServerPort = socket2.getLocalPort();

    socket1.close();
    socket2.close();
  }

  @After
  public void tearDown() throws Exception {
    publicVertxServer.stop().get();
    privateVertxServer.stop().get();
    vertx.close();
    storageEngine.close();
  }

  protected Enclave buildEnclave() {
    return new CesarEnclave();
  }

  protected Request buildPrivateAPIRequest(
      String path, HttpContentType contentType, Object payload) {
    return buildPostRequest(
        privateBaseUrl, path, contentType, serializer.serialize(contentType, payload));
  }

  protected Request buildPublicAPIRequest(
      String path, HttpContentType contentType, Object payload) {
    return buildPostRequest(
        publicBaseUrl, path, contentType, serializer.serialize(contentType, payload));
  }

  private Request buildPostRequest(
      String baseurl, String path, HttpContentType contentType, byte[] payload) {
    RequestBody body = RequestBody.create(MediaType.parse(contentType.httpHeaderValue), payload);

    if (path.startsWith("/")) {
      path = path.substring(1, path.length());
    }

    return new Request.Builder().post(body).url(baseurl + path).build();
  }
}

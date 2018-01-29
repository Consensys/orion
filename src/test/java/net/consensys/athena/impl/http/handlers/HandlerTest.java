package net.consensys.athena.impl.http.handlers;

import net.consensys.athena.api.cmd.AthenaRoutes;
import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.storage.StorageEngine;
import net.consensys.athena.impl.config.MemoryConfig;
import net.consensys.athena.impl.enclave.sodium.LibSodiumSettings;
import net.consensys.athena.impl.helpers.CesarEnclave;
import net.consensys.athena.impl.http.server.HttpContentType;
import net.consensys.athena.impl.http.server.vertx.VertxServer;
import net.consensys.athena.impl.network.MemoryNetworkNodes;
import net.consensys.athena.impl.storage.file.MapDbStorage;
import net.consensys.athena.impl.utils.Serializer;

import java.net.InetAddress;
import java.net.ServerSocket;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import okhttp3.HttpUrl;
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
  protected String baseUrl;

  // these are re-built between tests
  protected MemoryNetworkNodes networkNodes;
  protected MemoryConfig config;
  protected Enclave enclave;

  protected Vertx vertx;
  protected Integer httpServerPort;
  protected VertxServer vertxServer;
  protected AthenaRoutes routes;

  private StorageEngine<EncryptedPayload> storageEngine;

  @Before
  public void setUp() throws Exception {
    // athena dependencies, reset them all between tests
    config = new MemoryConfig();
    config.setLibSodiumPath(LibSodiumSettings.defaultLibSodiumPath());
    networkNodes = new MemoryNetworkNodes();
    enclave = buildEnclave();

    storageEngine = new MapDbStorage("routerdb");
    routes = new AthenaRoutes(vertx, networkNodes, serializer, enclave, storageEngine);

    // create our vertx object
    vertx = Vertx.vertx();

    // get a free httpServerPort
    ServerSocket socket = new ServerSocket(0);
    httpServerPort = socket.getLocalPort();
    socket.close();

    // settings = HTTP server with provided httpServerPort
    HttpServerOptions httpServerOptions = new HttpServerOptions();
    httpServerOptions.setPort(httpServerPort);

    // Initialise the base HTTP url in two forms: String and OkHttp's HttpUrl object to allow for simpler composition
    // of complex URLs with path parameters, query strings, etc.
    baseUrl =
        new HttpUrl.Builder()
            .scheme("http")
            .host(InetAddress.getLocalHost().getHostAddress())
            .port(httpServerPort)
            .build()
            .toString();

    // deploy our server
    vertxServer = new VertxServer(vertx, routes.getRouter(), httpServerOptions);
    vertxServer.start().get();
  }

  @After
  public void tearDown() throws Exception {
    vertxServer.stop().get();
    vertx.close();
    storageEngine.close();
  }

  protected Enclave buildEnclave() {
    return new CesarEnclave();
  }

  protected Request buildPostRequest(String path, HttpContentType contentType, Object payload) {
    RequestBody body =
        RequestBody.create(
            MediaType.parse(contentType.httpHeaderValue),
            serializer.serialize(contentType, payload));

    if (path.startsWith("/")) {
      path = path.substring(1, path.length());
    }

    return new Request.Builder().post(body).url(baseUrl + path).build();
  }
}

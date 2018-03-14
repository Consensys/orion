package net.consensys.orion.impl.http.handlers;

import static org.junit.Assert.assertEquals;

import net.consensys.orion.api.cmd.OrionRoutes;
import net.consensys.orion.api.enclave.Enclave;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.api.storage.StorageEngine;
import net.consensys.orion.impl.config.MemoryConfig;
import net.consensys.orion.impl.enclave.sodium.LibSodiumSettings;
import net.consensys.orion.impl.enclave.sodium.SodiumEncryptedPayload;
import net.consensys.orion.impl.helpers.StubEnclave;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.http.server.vertx.VertxServer;
import net.consensys.orion.impl.network.MemoryNetworkNodes;
import net.consensys.orion.impl.storage.file.MapDbStorage;
import net.consensys.orion.impl.utils.Serializer;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import okhttp3.HttpUrl;
import okhttp3.HttpUrl.Builder;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
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
  protected OrionRoutes routes;

  private StorageEngine<EncryptedPayload> storageEngine;

  @Before
  public void setUp() throws Exception {

    // get a free httpServerPort
    ServerSocket socket = new ServerSocket(0);
    httpServerPort = socket.getLocalPort();
    socket.close();

    // Initialise the base HTTP url in two forms: String and OkHttp's HttpUrl object to allow for simpler composition
    // of complex URLs with path parameters, query strings, etc.
    HttpUrl http =
        new Builder()
            .scheme("http")
            .host(InetAddress.getLocalHost().getHostAddress())
            .port(httpServerPort)
            .build();
    baseUrl = http.toString();

    // orion dependencies, reset them all between tests
    config = new MemoryConfig();
    config.setLibSodiumPath(LibSodiumSettings.defaultLibSodiumPath());
    networkNodes = new MemoryNetworkNodes(http.url());
    enclave = buildEnclave();

    String path = "routerdb";
    new File(path).mkdirs();
    storageEngine = new MapDbStorage(SodiumEncryptedPayload.class, path, serializer);
    routes = new OrionRoutes(vertx, networkNodes, serializer, enclave, storageEngine);

    // create our vertx object
    vertx = Vertx.vertx();

    // settings = HTTP server with provided httpServerPort
    HttpServerOptions httpServerOptions = new HttpServerOptions();
    httpServerOptions.setPort(httpServerPort);

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
    return new StubEnclave();
  }

  protected Request buildPostRequest(String path, HttpContentType contentType, Object payload) {
    return buildPostRequest(path, contentType, serializer.serialize(contentType, payload));
  }

  protected Request buildPostRequest(String path, HttpContentType contentType, byte[] payload) {
    RequestBody body = RequestBody.create(MediaType.parse(contentType.httpHeaderValue), payload);

    if (path.startsWith("/")) {
      path = path.substring(1, path.length());
    }

    return new Request.Builder().post(body).url(baseUrl + path).build();
  }

  protected void assertError(final OrionErrorCode expected, final Response actual)
      throws IOException {
    assertEquals("{\"error\":" + expected.code() + "}", actual.body().string());
  }
}

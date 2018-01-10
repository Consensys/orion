package net.consensys.athena.impl.http.handlers;

import static java.util.Optional.empty;

import net.consensys.athena.api.cmd.AthenaRoutes;
import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.impl.config.MemoryConfig;
import net.consensys.athena.impl.enclave.sodium.LibSodiumSettings;
import net.consensys.athena.impl.enclave.sodium.SodiumCombinedKey;
import net.consensys.athena.impl.enclave.sodium.SodiumEncryptedPayload;
import net.consensys.athena.impl.enclave.sodium.SodiumPublicKey;
import net.consensys.athena.impl.helpers.CesarEnclave;
import net.consensys.athena.impl.http.server.HttpContentType;
import net.consensys.athena.impl.http.server.HttpServerSettings;
import net.consensys.athena.impl.http.server.vertx.VertxServer;
import net.consensys.athena.impl.network.MemoryNetworkNodes;
import net.consensys.athena.impl.utils.Serializer;

import java.io.IOException;
import java.net.ServerSocket;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.vertx.core.Vertx;
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

  @Before
  public void setUp() throws IOException {
    // athena dependencies, reset them all between tests
    config = new MemoryConfig();
    config.setLibSodiumPath(LibSodiumSettings.defaultLibSodiumPath());
    networkNodes = new MemoryNetworkNodes();
    enclave = buildEnclave();

    routes = new AthenaRoutes(vertx, networkNodes, serializer, enclave);

    // create our vertx object
    vertx = Vertx.vertx();

    // get a free httpServerPort
    ServerSocket socket = new ServerSocket(0);
    httpServerPort = socket.getLocalPort();
    socket.close();

    // settings = HTTP server with provided httpServerPort
    HttpServerSettings httpSettings =
        new HttpServerSettings(Optional.empty(), Optional.of(httpServerPort), empty(), null);

    // Initialise the base HTTP url in two forms: String and OkHttp's HttpUrl object to allow for simpler composition
    // of complex URLs with path parameters, query strings, etc.
    baseUrl =
        new HttpUrl.Builder()
            .scheme("http")
            .host("localhost")
            .port(httpServerPort)
            .build()
            .toString();

    // deploy our server
    vertxServer = new VertxServer(vertx, routes.getRouter(), httpSettings);
    vertxServer.start();
  }

  @After
  public void tearDown() {
    vertxServer.stop();
    vertx.close();
  }

  protected Enclave buildEnclave() {
    return new CesarEnclave();
  }

  protected SodiumEncryptedPayload mockPayload() {
    SodiumCombinedKey sodiumCombinedKey = new SodiumCombinedKey("Combined key fakery".getBytes());
    Map<PublicKey, Integer> combinedKeysOwners = new HashMap<>();

    SodiumEncryptedPayload encryptedPayload =
        new SodiumEncryptedPayload(
            new SodiumPublicKey("fakekey".getBytes()),
            "fake nonce".getBytes(),
            "fake combinedNonce".getBytes(),
            new SodiumCombinedKey[] {sodiumCombinedKey},
            "fake ciphertext".getBytes(),
            combinedKeysOwners);

    return encryptedPayload;
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

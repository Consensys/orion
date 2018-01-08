package net.consensys.athena.impl.http.controllers;

import static java.util.Optional.empty;

import net.consensys.athena.api.cmd.AthenaRoutes;
import net.consensys.athena.impl.config.MemoryConfig;
import net.consensys.athena.impl.enclave.sodium.LibSodiumSettings;
import net.consensys.athena.impl.http.data.Serializer;
import net.consensys.athena.impl.http.server.HttpServerSettings;
import net.consensys.athena.impl.http.server.vertx.VertxServer;
import net.consensys.athena.impl.network.MemoryNetworkNodes;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Optional;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public abstract class ControllerTest {
  private final Serializer serializer = new Serializer();

  // these are re-built between tests
  protected MemoryNetworkNodes networkNodes;
  protected MemoryConfig config;

  protected Vertx vertx;
  protected Integer httpServerPort;
  private VertxServer vertxServer;
  private AthenaRoutes routes;

  @Before
  public void setUp(TestContext context) throws IOException {
    // athena dependencies, reset them all between tests
    config = new MemoryConfig();
    config.setLibSodiumPath(LibSodiumSettings.defaultLibSodiumPath());
    networkNodes = new MemoryNetworkNodes();

    routes = new AthenaRoutes(vertx, networkNodes, config, serializer);

    // create our vertx object
    vertx = Vertx.vertx();

    // get a free httpServerPort
    ServerSocket socket = new ServerSocket(0);
    httpServerPort = socket.getLocalPort();
    socket.close();

    // settings = HTTP server with provided httpServerPort
    HttpServerSettings httpSettings =
        new HttpServerSettings(Optional.empty(), Optional.of(httpServerPort), empty(), null);

    // deploy our server
    vertxServer = new VertxServer(vertx, routes.getRouter(), httpSettings);
    vertx.deployVerticle(vertxServer, context.asyncAssertSuccess());
  }

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }
}

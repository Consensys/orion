//package net.consensys.athena.impl.http.controllers;
//
//import io.vertx.core.DeploymentOptions;
//import io.vertx.core.Vertx;
//import io.vertx.core.json.JsonObject;
//import io.vertx.ext.unit.TestContext;
//import io.vertx.ext.unit.junit.VertxUnitRunner;
//import java.io.IOException;
//import java.net.ServerSocket;
//import net.consensys.athena.api.cmd.AthenaRoutes;
//import net.consensys.athena.impl.http.data.Serializer;
//import net.consensys.athena.impl.http.server.vertx.VertxServer;
//import org.junit.Before;
//import org.junit.runner.RunWith;
//
//@RunWith(VertxUnitRunner.class)
//public class ControllerTest {
//  private final Serializer serializer = new Serializer();
//
//  private Vertx vertx;
//  private Integer port;
//  private VertxServer appServer;
//  private AthenaRoutes routes = new AthenaRoutes();
//
//  @Before
//  public void setUp(TestContext context) throws IOException {
//    vertx = Vertx.vertx();
//    ServerSocket socket = new ServerSocket(0);
//    port = socket.getLocalPort();
//    socket.close();
//
//    appServer = new VertxServer(vertx, routes.getRouter(), httpSettings);
//
//
//
//    DeploymentOptions options = new DeploymentOptions()
//        .setConfig(new JsonObject().put("http.port", port)
//        );
//    vertx.deployVerticle(MyFirstVerticle.class.getName(), options, context.asyncAssertSuccess());
//  }
//}

package net.consensys.athena.impl.http.server.vertx;

import net.consensys.athena.impl.http.server.HttpServerSettings;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;

public class VertxServer extends AbstractVerticle {
  private final HttpServer httpServer;
  private final Router router;

  public VertxServer(Vertx vertx, Router router, HttpServerSettings httpSettings) {
    // vertx http server
    // TODO manage optiosn & settings (https, domain socketn ...)

    HttpServerOptions serverOptions = new HttpServerOptions();
    serverOptions.setPort(httpSettings.getHttpPort().get());

    this.httpServer = vertx.createHttpServer(serverOptions);
    this.router = router;
  }

  @Override
  public void start() throws Exception {
    httpServer.requestHandler(router::accept).listen();
  }

  @Override
  public void stop() throws Exception {
    httpServer.close();
  }
}

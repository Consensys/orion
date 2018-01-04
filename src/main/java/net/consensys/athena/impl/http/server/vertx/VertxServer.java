package net.consensys.athena.impl.http.server.vertx;

import static java.util.Optional.empty;

import net.consensys.athena.api.config.Config;
import net.consensys.athena.impl.http.server.HttpServerSettings;

import java.util.Optional;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;

public class VertxServer {
  private final HttpServer httpServer;
  private final Router router;

  public VertxServer(Vertx vertx, Router router, Config config) {
    // vertx http server
    // TODO manage optiosn & settings (https, domain socketn ...)
    HttpServerSettings httpSettings =
        new HttpServerSettings(config.socket(), Optional.of((int) config.port()), empty(), null);

    HttpServerOptions serverOptions = new HttpServerOptions();
    serverOptions.setPort(httpSettings.getHttpPort().get());

    this.httpServer = vertx.createHttpServer(serverOptions);
    this.router = router;
  }

  public void start() {
    httpServer.requestHandler(router::accept).listen();
  }
}

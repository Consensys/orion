package net.consensys.athena.impl.http.server.vertx;

import net.consensys.athena.impl.http.server.HttpServerSettings;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;

public class VertxServer {
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

  public Future<Boolean> start() {
    CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();
    httpServer
        .requestHandler(router::accept)
        .listen(
            result -> {
              if (result.succeeded()) {
                resultFuture.complete(true);
              } else {
                resultFuture.completeExceptionally(result.cause());
              }
            });

    return resultFuture;
  }

  public Future<Boolean> stop() {
    CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();

    httpServer.close(
        result -> {
          if (result.succeeded()) {
            resultFuture.complete(true);
          } else {
            resultFuture.completeExceptionally(result.cause());
          }
        });

    return resultFuture;
  }
}

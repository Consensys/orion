package net.consensys.orion.acceptance.proxy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SocketTimeoutVerticle extends AbstractVerticle {

  private static final Logger log = LogManager.getLogger();

  /** The port users connect to the proxy server on. */
  private final int listeningPort;

  private final Vertx vertx;

  private final HttpServer exceptionalServer;

  public SocketTimeoutVerticle(String hostName, int listeningPort) {
    this.listeningPort = listeningPort;
    this.vertx = Vertx.vertx();

    final HttpServerOptions exceptionalOptions =
        new HttpServerOptions().setPort(listeningPort).setHost(hostName);
    this.exceptionalServer = vertx.createHttpServer(exceptionalOptions);
  }

  @Override
  public void start() throws Exception {

    exceptionalServer.requestHandler(
        originalRequest -> {

          //TODO why is this request handler not getting called as a result of the send request???
          System.out.println("XXXXXXxxxxXXXXX");
          log.error("AAAAAAARggggghhhh");

          //TODO cause socket exception
          try {
            Thread.sleep(2000);
          } catch (InterruptedException e) {
            log.warn(e);
          }
        });

    CompletableFuture<Void> resultFuture = new CompletableFuture<>();

    exceptionalServer.listen(
        result -> {
          if (result.succeeded()) {
            log.info("Socket Exception server started on {}", listeningPort);
            resultFuture.complete(null);
          } else {
            result.cause().printStackTrace();
            resultFuture.completeExceptionally(result.cause());
          }
        });

    // Make the asynchronous operation synchronous
    try {
      resultFuture.get();
    } catch (final InterruptedException | ExecutionException io) {
      log.error(io.getMessage());
    }
  }

  public void stop() {
    CompletableFuture<Void> resultFuture = new CompletableFuture<>();

    vertx.close(
        result -> {
          if (result.succeeded()) {
            resultFuture.complete(null);
          } else {
            resultFuture.completeExceptionally(result.cause());
          }
        });

    try {
      resultFuture.get();
    } catch (InterruptedException | ExecutionException io) {
      log.error(io.getMessage());
    }
  }
}

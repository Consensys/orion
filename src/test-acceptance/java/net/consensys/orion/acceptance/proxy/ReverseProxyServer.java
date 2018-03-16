package net.consensys.orion.acceptance.proxy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** A simple reverse proxy server. */
public class ReverseProxyServer {

  private static final Logger log = LogManager.getLogger();

  private final Vertx vertx;
  private final ProxyVerticle proxy;
  private final SocketTimeoutVerticle socketTimeout;

  private String deploymentID;

  public ReverseProxyServer(String hostName, int listeningPort, int targetPort) {
    this.vertx = Vertx.vertx();
    this.proxy = new ProxyVerticle(hostName, listeningPort, targetPort);
    this.socketTimeout = new SocketTimeoutVerticle(hostName, listeningPort);
  }

  public void start() {
    deploy(proxy);
  }

  public void stop() {
    undeployVerticle();
  }

  /** For every subsequent request, do not proxy the request, instead cause a socket problem. */
  public void socketProblem() {
    undeployVerticle();
    deploy(socketTimeout);
  }

  private void undeployVerticle() {
    CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();

    vertx.undeploy(
        deploymentID,
        res -> {
          if (res.succeeded()) {
            log.info("Undeployed id {}", deploymentID);
            resultFuture.complete(true);
          } else {
            log.warn("Failed undeploy of id {}", deploymentID);
            resultFuture.completeExceptionally(res.cause());
          }
        });

    // Make the asynchronous operation synchronous
    try {
      resultFuture.get();
    } catch (final InterruptedException | ExecutionException io) {
      log.error(io.getMessage());
    }
  }

  private void deploy(Verticle toDeply) {
    CompletableFuture<String> resultFuture = new CompletableFuture<>();

    vertx.deployVerticle(
        toDeply,
        res -> {
          if (res.succeeded()) {
            log.info("Deployed {}: ", res.result());
            resultFuture.complete(res.result());
          } else {
            log.error("Failed deployment of {}", toDeply);
            resultFuture.completeExceptionally(res.cause());
          }
        });

    // Make the asynchronous operation synchronous
    try {
      deploymentID = resultFuture.get();
    } catch (final InterruptedException | ExecutionException io) {
      log.error(io.getMessage());
    }
  }
}

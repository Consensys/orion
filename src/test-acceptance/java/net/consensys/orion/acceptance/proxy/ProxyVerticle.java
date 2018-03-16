package net.consensys.orion.acceptance.proxy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProxyVerticle extends AbstractVerticle {

  private static final Logger log = LogManager.getLogger();

  private final String hostName;

  /** The port users connect to the proxy server on. */
  private final int listeningPort;

  /** The port that messages are proxied onto. */
  private final int targetPort;

  private final Vertx vertx;

  private final HttpServer proxyServer;
  private final HttpClient client;

  public ProxyVerticle(String hostName, int listeningPort, int targetPort) {
    this.hostName = hostName;
    this.listeningPort = listeningPort;
    this.targetPort = targetPort;
    this.vertx = Vertx.vertx();

    final HttpClientOptions clientConfig =
        new HttpClientOptions().setDefaultPort(targetPort).setDefaultHost(hostName);
    this.client = vertx.createHttpClient(clientConfig);

    final HttpServerOptions proxyOptions =
        new HttpServerOptions().setPort(listeningPort).setHost(hostName);
    this.proxyServer = vertx.createHttpServer(proxyOptions);
  }

  @Override
  public void start() throws Exception {
    proxyServer.requestHandler(
        originalRequest -> {
          final HttpClientRequest proxiedRequest =
              client.request(
                  originalRequest.method(),
                  originalRequest.uri(),
                  proxiedResponse -> {
                    log.info("Proxying response: {}", proxiedResponse.statusCode());
                    originalRequest.response().setStatusCode(proxiedResponse.statusCode());
                    originalRequest.response().headers().setAll(proxiedResponse.headers());
                    originalRequest.response().setChunked(true);

                    proxiedResponse.bodyHandler(
                        data -> {
                          log.info("Proxying response body: {}", data);
                          originalRequest.response().end(data);
                        });
                  });

          proxiedRequest.headers().setAll(originalRequest.headers());
          proxiedRequest.setChunked(true);

          originalRequest.bodyHandler(
              data -> {
                log.info("Proxying originalRequest body: {}", data);
                proxiedRequest.end(data);
              });
        });

    proxyServer.listen(
        ar -> {
          if (ar.succeeded()) {
            log.info("Proxy server started on {}", listeningPort);
          } else {
            ar.cause().printStackTrace();
          }
        });
  }

  public void stop() {
    CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();

    vertx.close(
        result -> {
          if (result.succeeded()) {
            resultFuture.complete(true);
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

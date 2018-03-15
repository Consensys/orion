package net.consensys.orion.acceptance.send;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** A simple reverse proxy server. */
public class ReverseProxyServer {

  private static final Logger log = LogManager.getLogger();

  private final String hostName;

  /** The port users connect to the proxy server on. */
  private final int listeningPort;

  /** The port the proxy forwards requests on to. */
  private final int targetPort;

  private final Vertx vertx;

  public ReverseProxyServer(String hostName, int listeningPort, int targetPort) {
    this.hostName = hostName;
    this.listeningPort = listeningPort;
    this.targetPort = targetPort;
    this.vertx = Vertx.vertx();
  }

  public void start() {
    final HttpClientOptions clientConfig =
        new HttpClientOptions().setDefaultHost(hostName).setDefaultPort(targetPort);
    final HttpClient client = vertx.createHttpClient(clientConfig);

    final HttpServerOptions proxyOptions =
        new HttpServerOptions().setPort(listeningPort).setHost(hostName);
    final HttpServer proxyServer = vertx.createHttpServer(proxyOptions);

    proxyServer.requestHandler(
        request -> {
          final HttpClientRequest cReq =
              client.request(
                  request.method(),
                  request.uri(),
                  cRes -> {
                    log.info("Proxying response: %s", cRes.statusCode());
                    request.response().setStatusCode(cRes.statusCode());
                    request.response().headers().setAll(cRes.headers());
                    request.response().setChunked(true);

                    cRes.bodyHandler(
                        data -> {
                          log.info("Proxying response body: %s", data);
                          request.response().end(data);
                        });
                  });

          cReq.headers().setAll(request.headers());
          cReq.setChunked(true);

          request.bodyHandler(
              data -> {
                log.info("Proxying request body: %s", data);
                cReq.end(data);
              });
        });

    proxyServer.listen(
        ar -> {
          if (ar.succeeded()) {
            log.info("Proxy server started on %s", listeningPort);
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

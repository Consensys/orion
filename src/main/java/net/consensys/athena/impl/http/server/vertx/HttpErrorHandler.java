package net.consensys.athena.impl.http.server.vertx;

import net.consensys.athena.impl.http.server.HttpContentType;
import net.consensys.athena.impl.http.server.HttpError;
import net.consensys.athena.impl.utils.Serializer;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HttpErrorHandler implements Handler<RoutingContext> {
  private static final Logger log = LogManager.getLogger();

  public HttpErrorHandler(Serializer serializer) {
    this.serializer = serializer;
  }

  private final Serializer serializer;

  @Override
  public void handle(RoutingContext failureContext) {
    int statusCode = failureContext.statusCode();
    statusCode = statusCode < 0 ? 500 : statusCode;

    HttpServerResponse response = failureContext.response().setStatusCode(statusCode);

    if (failureContext.failure() != null) {
      HttpError httpError = new HttpError(failureContext.failure().getMessage());
      Buffer buffer = Buffer.buffer(serializer.serialize(HttpContentType.JSON, httpError));

      log.error(failureContext.currentRoute().getPath() + " failed " + httpError);
      log.error(failureContext.failure().getStackTrace());

      response
          .putHeader(HttpHeaders.CONTENT_TYPE, HttpContentType.JSON.httpHeaderValue)
          .end(buffer);
    } else {
      response.end();
    }
  }
}

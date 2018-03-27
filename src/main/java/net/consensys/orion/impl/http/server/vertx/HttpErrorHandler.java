package net.consensys.orion.impl.http.server.vertx;

import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.api.exception.OrionException;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.http.server.HttpError;
import net.consensys.orion.impl.utils.Serializer;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HttpErrorHandler implements Handler<RoutingContext> {
  private static final Logger log = LogManager.getLogger();

  private final Serializer serializer;

  public HttpErrorHandler(Serializer serializer) {
    this.serializer = serializer;
  }

  @Override
  public void handle(RoutingContext failureContext) {
    final int statusCode = statusCode(failureContext);
    final HttpServerResponse response = failureContext.response().setStatusCode(statusCode);

    if (hasError(failureContext)) {
      final Buffer buffer = errorJson(failureContext.failure(), failureContext.currentRoute());

      response
          .putHeader(HttpHeaders.CONTENT_TYPE, HttpContentType.JSON.httpHeaderValue)
          .end(buffer);
    } else {
      response.end();
    }
  }

  private Buffer errorJson(final Throwable failure, final Route failureRoute) {
    final OrionErrorCode orionError = orionError(failure);
    final HttpError httpError = new HttpError(orionError);
    final Buffer buffer = Buffer.buffer(serializer.serialize(HttpContentType.JSON, httpError));

    log.error(failureRoute.getPath() + " failed " + httpError, failure);

    return buffer;
  }

  /**
   * Status code may not have been set (left as a negative number), in which case assume server side
   * issue.
   */
  private int statusCode(RoutingContext failureContext) {
    return failureContext.statusCode() < 0
        ? HttpResponseStatus.INTERNAL_SERVER_ERROR.code()
        : failureContext.statusCode();
  }

  private boolean hasError(RoutingContext failureContext) {
    return failureContext.failure() != null;
  }

  private OrionErrorCode orionError(final Throwable failure) {

    if (failure instanceof OrionException) {
      log.info(failure);
      return ((OrionException) failure).code();
    }

    log.warn("Non OrionException, default unmapped code used", failure);
    return OrionErrorCode.UNMAPPED;
  }
}

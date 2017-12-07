package net.consensys.athena.impl.http.server;

import java.util.Optional;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;

public interface Result<T> {

  @NotNull
  static <U> Result<U> ok(ContentType defaultContentType, FullHttpResponse response, U payload) {
    return new ResultImpl<>(
        defaultContentType, response, Optional.of(payload), HttpResponseStatus.OK);
  }

  @NotNull
  static Result badRequest(ContentType defaultContentType, FullHttpResponse response) {
    return new ResultImpl<>(
        defaultContentType, response, Optional.empty(), HttpResponseStatus.BAD_REQUEST);
  }

  @NotNull
  static Result notImplemented(ContentType defaultContentType, FullHttpResponse response) {
    return new ResultImpl<>(
        defaultContentType, response, Optional.empty(), HttpResponseStatus.NOT_IMPLEMENTED);
  }

  @NotNull
  static Result internalServerError(ContentType defaultContentType, FullHttpResponse response) {
    return new ResultImpl<>(
        defaultContentType, response, Optional.empty(), HttpResponseStatus.INTERNAL_SERVER_ERROR);
  }

  FullHttpResponse getResponse();

  Optional<T> getPayload();

  ContentType defaultContentType();

  default HttpResponseStatus getStatus() {
    return HttpResponseStatus.OK;
  }
}

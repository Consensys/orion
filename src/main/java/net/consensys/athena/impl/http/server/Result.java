package net.consensys.athena.impl.http.server;

import static java.util.Optional.*;

import java.util.Optional;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.jetbrains.annotations.NotNull;

public interface Result<T> {

  @NotNull
  static <U> Result<U> ok(ContentType defaultContentType, U payload) {
    return new ResultImpl<>(defaultContentType, of(payload), HttpResponseStatus.OK);
  }

  @NotNull
  static Result badRequest(ContentType defaultContentType) {
    return new ResultImpl<>(defaultContentType, empty(), HttpResponseStatus.BAD_REQUEST);
  }

  @NotNull
  static Result notImplemented(ContentType defaultContentType) {
    return new ResultImpl<>(defaultContentType, empty(), HttpResponseStatus.NOT_IMPLEMENTED);
  }

  @NotNull
  static Result internalServerError(ContentType defaultContentType) {
    return new ResultImpl<>(defaultContentType, empty(), HttpResponseStatus.INTERNAL_SERVER_ERROR);
  }

  Optional<T> getPayload();

  ContentType defaultContentType();

  default HttpResponseStatus getStatus() {
    return HttpResponseStatus.OK;
  }

  default Optional<HttpHeaders> getExtraHeaders() {
    return empty();
  }
}

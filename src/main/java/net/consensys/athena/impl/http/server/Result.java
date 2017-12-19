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
  static <U> Result<U> badRequest(ContentType defaultContentType, U payload) {
    return new ResultImpl<>(defaultContentType, of(payload), HttpResponseStatus.BAD_REQUEST);
  }

  @NotNull
  static <U> Result<U> notFound(ContentType defaultContentType, U payload) {
    return new ResultImpl<>(defaultContentType, of(payload), HttpResponseStatus.NOT_FOUND);
  }

  @NotNull
  static Result notImplemented(ContentType defaultContentType) {
    return new ResultImpl<>(defaultContentType, empty(), HttpResponseStatus.NOT_IMPLEMENTED);
  }

  @NotNull
  static <U> Result<U> internalServerError(ContentType defaultContentType, U payload) {
    return new ResultImpl<>(
        defaultContentType, of(payload), HttpResponseStatus.INTERNAL_SERVER_ERROR);
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

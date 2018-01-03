package net.consensys.athena.impl.http.data;

import java.util.Optional;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

public class ResultImpl<T> implements Result<T> {

  private final Optional<T> payload;
  private final ContentType defaultContentType;
  private final HttpResponseStatus status;
  private HttpHeaders extraHeaders;

  public ResultImpl(
      ContentType defaultContentType, Optional<T> payload, HttpResponseStatus status) {
    this.payload = payload;
    this.defaultContentType = defaultContentType;
    this.status = status;
    this.extraHeaders = null;
  }

  public ResultImpl(
      ContentType defaultContentType,
      Optional<T> payload,
      HttpResponseStatus status,
      HttpHeaders extraHeaders) {
    this(defaultContentType, payload, status);
    this.extraHeaders = extraHeaders;
  }

  @Override
  public Optional<T> getPayload() {
    return payload;
  }

  @Override
  public ContentType defaultContentType() {
    return defaultContentType;
  }

  @Override
  public HttpResponseStatus getStatus() {
    return status;
  }

  @Override
  public Optional<HttpHeaders> getExtraHeaders() {
    return Optional.ofNullable(extraHeaders);
  }
}

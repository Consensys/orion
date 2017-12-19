package net.consensys.athena.impl.http.data;

import java.util.Optional;

import io.netty.handler.codec.http.HttpResponseStatus;

public class ResultImpl<T> implements Result<T> {

  private Optional<T> payload;
  private ContentType defaultContentType;
  private HttpResponseStatus status;

  public ResultImpl(
      ContentType defaultContentType, Optional<T> payload, HttpResponseStatus status) {
    this.payload = payload;
    this.defaultContentType = defaultContentType;
    this.status = status;
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
}

package net.consensys.athena.impl.http.server;

import java.util.Optional;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

public class ResultImpl<T> implements Result<T> {

  private Optional<T> payload;
  private FullHttpResponse response;
  private ContentType defaultContentType;
  private HttpResponseStatus status;

  public ResultImpl(
      ContentType defaultContentType,
      FullHttpResponse response,
      Optional<T> payload,
      HttpResponseStatus status) {
    this.payload = payload;
    this.response = response;
    this.defaultContentType = defaultContentType;
    this.status = status;
  }

  @Override
  public FullHttpResponse getResponse() {
    return response;
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

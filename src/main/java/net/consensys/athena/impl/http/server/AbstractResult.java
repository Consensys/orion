package net.consensys.athena.impl.http.server;

import io.netty.handler.codec.http.FullHttpResponse;

public abstract class AbstractResult implements Result {

  private final ContentType defaultContentType;
  private FullHttpResponse response;

  public AbstractResult(FullHttpResponse response) {
    this(response, ContentType.JSON);
  }

  public AbstractResult(FullHttpResponse response, ContentType defaultContentType) {
    this.response = response;
    this.defaultContentType = defaultContentType;
  }

  @Override
  public FullHttpResponse getResponse() {
    return response;
  }

  @Override
  public ContentType defaultContentType() {
    return defaultContentType;
  }
}

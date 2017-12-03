package net.consensys.athena.impl.http.server;

import io.netty.handler.codec.http.FullHttpResponse;

public abstract class AbstractResponder implements Responder {

  private final ContentType defaultContentType;
  private FullHttpResponse response;

  public AbstractResponder(FullHttpResponse response) {
    this(response, ContentType.JSON);
  }

  public AbstractResponder(FullHttpResponse response, ContentType defaultContentType) {
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

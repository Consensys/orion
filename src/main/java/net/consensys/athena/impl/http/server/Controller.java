package net.consensys.athena.impl.http.server;

import io.netty.handler.codec.http.FullHttpRequest;

public interface Controller {
  Result handle(FullHttpRequest request) throws Exception;

  // returns the expected request class, used for deserialization purposes
  default Class expectedRequest() {
    return EmptyRequest.class;
  }
}

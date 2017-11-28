package net.consensys.athena.impl.http.server;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

public interface Controller {
  FullHttpResponse handle(FullHttpRequest request, FullHttpResponse response);
}

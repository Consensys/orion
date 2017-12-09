package net.consensys.athena.impl.http.server;

import io.netty.handler.codec.http.FullHttpRequest;

public interface Controller {
  Result handle(FullHttpRequest request);
}

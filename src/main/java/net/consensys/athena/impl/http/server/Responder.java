package net.consensys.athena.impl.http.server;

import io.netty.handler.codec.http.FullHttpResponse;

public interface Responder {

  FullHttpResponse getResponse();

  byte[] getRaw();

  byte[] getHaskellEncoded();

  String getJson();

  ContentType defaultContentType();
}

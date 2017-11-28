package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.impl.http.server.Controller;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

/** Used by a client to retrieve a payload (without base64 encoding) */
public class ReceiveRawController implements Controller {

  public static final Controller INSTANCE = new ReceiveRawController();

  @Override
  public FullHttpResponse handle(FullHttpRequest request, FullHttpResponse response) {
    return response;
  }
}

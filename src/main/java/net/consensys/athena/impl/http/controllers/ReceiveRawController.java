package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.impl.http.server.Controller;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

/** Used by a client to retrieve a payload (without base64 encoding) */
public class ReceiveRawController implements Controller {

  public static final Controller INSTANCE = new ReceiveRawController();

  @Override
  public FullHttpResponse handle(HttpRequest request, FullHttpResponse response) {
    return response;
  }
}

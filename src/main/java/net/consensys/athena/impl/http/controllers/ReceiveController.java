package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.impl.http.server.Controller;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

public class ReceiveController implements Controller {

  public static final Controller INSTANCE = new ReceiveController();

  @Override
  public FullHttpResponse handle(HttpRequest request, FullHttpResponse response) {

    return response;
  }
}

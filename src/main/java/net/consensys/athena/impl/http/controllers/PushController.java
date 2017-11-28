package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.impl.http.server.Controller;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

/** used to push a payload to a node. */
public class PushController implements Controller {

  public static final Controller INSTANCE = new PushController();



  private PushController() {}

  @Override
  public FullHttpResponse handle(FullHttpRequest request, FullHttpResponse response) {
    // we receive a encrypted payload (binary content) and store it into storage system



    return response;
  }
}

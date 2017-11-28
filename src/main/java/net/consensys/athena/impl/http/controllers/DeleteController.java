package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.impl.http.server.Controller;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

/** Delete a payload from storage. */
public class DeleteController implements Controller {

  public static final Controller INSTANCE = new DeleteController();

  @Override
  public FullHttpResponse handle(FullHttpRequest request, FullHttpResponse response) {
    return response;
  }
}

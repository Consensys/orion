package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.impl.http.server.Controller;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

/** used for a client(quorum) to send a raw payload to store in athena */
public class SendRawController implements Controller {

  public static final Controller INSTANCE = new SendRawController();

  @Override
  public FullHttpResponse handle(FullHttpRequest request, FullHttpResponse response) {
    return response;
  }
}

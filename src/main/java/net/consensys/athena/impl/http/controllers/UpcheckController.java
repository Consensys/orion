package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.impl.http.responders.UpcheckResponder;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Responder;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

/**
 * Simple upcheck/hello check to see if the server is up and running. Returns a 200 response with
 * the body "I'm up!"
 */
public class UpcheckController implements Controller {

  @Override
  public Responder handle(HttpRequest request, FullHttpResponse response) {
    return new UpcheckResponder(response);
  }
}

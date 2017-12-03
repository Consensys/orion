package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.http.responders.PushResponder;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Responder;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

/** used to push a payload to a node. */
public class PushController implements Controller {
  private Storage storage;

  public PushController(Storage storage) {
    this.storage = storage;
  }

  @Override
  public Responder handle(HttpRequest request, FullHttpResponse response) {
    return new PushResponder(response);
  }
}

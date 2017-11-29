package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.http.server.Controller;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

/** used to push a payload to a node. */
public class PushController implements Controller {
  private Storage storage;

  public PushController(Storage storage) {
    this.storage = storage;
  }



  private PushController() {}

  @Override
  public FullHttpResponse handle(FullHttpRequest request, FullHttpResponse response) {
    // we receive a encrypted payload (binary content) and store it into storage system



    return response;
  }
}

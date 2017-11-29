package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.http.server.Controller;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

/** Delete a payload from storage. */
public class DeleteController implements Controller {
  private final Storage storage;

  public DeleteController(Storage storage) {
    this.storage = storage;
  }

  @Override
  public FullHttpResponse handle(HttpRequest request, FullHttpResponse response) {
    return response;
  }
}

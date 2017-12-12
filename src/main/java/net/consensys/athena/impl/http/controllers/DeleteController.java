package net.consensys.athena.impl.http.controllers;

import static net.consensys.athena.impl.http.server.Result.notImplemented;

import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Result;

import io.netty.handler.codec.http.FullHttpRequest;

/** Delete a payload from storage. */
public class DeleteController implements Controller {
  private final Storage storage;

  public DeleteController(Storage storage) {
    this.storage = storage;
  }

  @Override
  public Result handle(FullHttpRequest request) {
    return notImplemented();
  }
}

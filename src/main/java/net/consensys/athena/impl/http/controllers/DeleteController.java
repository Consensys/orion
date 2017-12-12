package net.consensys.athena.impl.http.controllers;

import static net.consensys.athena.impl.http.data.Result.notImplemented;

import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.http.data.Request;
import net.consensys.athena.impl.http.data.Result;
import net.consensys.athena.impl.http.server.Controller;

/** Delete a payload from storage. */
public class DeleteController implements Controller {
  private final Storage storage;

  public DeleteController(Storage storage) {
    this.storage = storage;
  }

  @Override
  public Result handle(Request request) {
    return notImplemented();
  }
}

package net.consensys.athena.impl.http.controllers;

import static net.consensys.athena.impl.http.server.Result.*;

import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.impl.http.server.ContentType;
import net.consensys.athena.impl.http.server.Controller;
import net.consensys.athena.impl.http.server.Result;

import io.netty.handler.codec.http.FullHttpRequest;

/** used to push a payload to a node. */
public class PushController implements Controller {
  private Storage storage;

  public PushController(Storage storage) {
    this.storage = storage;
  }

  @Override
  public Result handle(FullHttpRequest request) {
    return notImplemented(ContentType.HASKELL_ENCODED);
  }
}

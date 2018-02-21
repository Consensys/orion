package net.consensys.orion.impl.http.handler.delete;

import net.consensys.orion.api.storage.Storage;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/** Delete a payload from storage. */
public class DeleteHandler implements Handler<RoutingContext> {
  private final Storage storage;

  public DeleteHandler(Storage storage) {
    this.storage = storage;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    throw new UnsupportedOperationException("This handler has not been implemented yet");
  }
}

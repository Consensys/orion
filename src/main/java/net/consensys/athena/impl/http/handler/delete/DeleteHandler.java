package net.consensys.athena.impl.http.handler.delete;

import net.consensys.athena.api.storage.Storage;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/** Delete a payload from storage. */
public class DeleteHandler implements Handler<RoutingContext> {
  private final Storage storage;

  public DeleteHandler(Storage storage) {
    this.storage = storage;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    throw new NotImplementedException();
  }
}

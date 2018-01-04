package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.api.storage.Storage;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/** Delete a payload from storage. */
public class DeleteController implements Handler<RoutingContext> {
  private final Storage storage;

  public DeleteController(Storage storage) {
    this.storage = storage;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    routingContext.fail(500);
  }
}

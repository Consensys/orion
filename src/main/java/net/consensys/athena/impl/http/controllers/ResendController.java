package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.storage.Storage;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * ask to resend a single transaction or all transactions. Useful in situations where a
 * constellation node has lost it's database and wants to recover lost transactions.
 */
public class ResendController implements Handler<RoutingContext> {
  private final Enclave enclave;
  private final Storage storage;

  public ResendController(Enclave enclave, Storage storage) {
    this.enclave = enclave;
    this.storage = storage;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    routingContext.fail(500);
  }
}

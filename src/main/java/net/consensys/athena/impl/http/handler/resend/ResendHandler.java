package net.consensys.athena.impl.http.handler.resend;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.storage.Storage;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * ask to resend a single transaction or all transactions. Useful in situations where a
 * constellation node has lost it's database and wants to recover lost transactions.
 */
public class ResendHandler implements Handler<RoutingContext> {
  private final Enclave enclave;
  private final Storage storage;

  public ResendHandler(Enclave enclave, Storage storage) {
    this.enclave = enclave;
    this.storage = storage;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    throw new UnsupportedOperationException("This handler has not been implemented yet");
  }
}

package net.consensys.orion.cmd;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import java.util.function.Supplier;

// Responsible for reporting the number of connected peers:
public class PeerCountHandler implements Handler<RoutingContext> {

  private final Supplier<Integer> peerCountSupplier;

  public PeerCountHandler(Supplier<Integer> peerCountSupplier) {
    this.peerCountSupplier = peerCountSupplier;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    routingContext.response().end(this.peerCountSupplier.get().toString());
  }
}

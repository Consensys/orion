package net.consensys.athena.impl.http.handler.upcheck;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Simple upcheck/hello check to see if the server is up and running. Returns a 200 response with
 * the body "I'm up!"
 */
public class UpcheckHandler implements Handler<RoutingContext> {

  @Override
  public void handle(RoutingContext routingContext) {
    routingContext.response().end("I'm up!");
  }
}

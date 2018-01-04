package net.consensys.athena.impl.http.controllers;

import net.consensys.athena.api.network.NetworkNodes;
import net.consensys.athena.impl.http.data.ContentType;
import net.consensys.athena.impl.http.data.Serializer;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

/**
 * Used as a part of the network discovery process. Look up the binary list of constellation nodes
 * that a node has knowledge of.
 */
public class PartyInfoController implements Handler<RoutingContext> {

  private final NetworkNodes networkNodes;
  private final Serializer serializer;

  public PartyInfoController(NetworkNodes networkNodes, Serializer serializer) {
    this.networkNodes = networkNodes;
    this.serializer = serializer;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    Buffer toReturn = Buffer.buffer(serializer.serialize(ContentType.CBOR, networkNodes));
    routingContext.response().end(toReturn);
  }
}

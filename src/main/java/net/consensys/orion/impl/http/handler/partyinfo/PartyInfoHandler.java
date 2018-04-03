package net.consensys.orion.impl.http.handler.partyinfo;

import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.network.ConcurrentNetworkNodes;
import net.consensys.orion.impl.utils.Serializer;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

/**
 * Used as a part of the network discovery process. Look up the binary list of constellation nodes that a node has
 * knowledge of.
 */
public class PartyInfoHandler implements Handler<RoutingContext> {

  private final ConcurrentNetworkNodes networkNodes;

  public PartyInfoHandler(ConcurrentNetworkNodes networkNodes) {
    this.networkNodes = networkNodes;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    ConcurrentNetworkNodes callerPeers =
        Serializer.deserialize(HttpContentType.CBOR, ConcurrentNetworkNodes.class, routingContext.getBody().getBytes());
    Buffer toReturn = Buffer.buffer(Serializer.serialize(HttpContentType.CBOR, networkNodes));
    routingContext.response().end(toReturn);

    // merge callerPeers into our peers
    networkNodes.merge(callerPeers);
  }
}

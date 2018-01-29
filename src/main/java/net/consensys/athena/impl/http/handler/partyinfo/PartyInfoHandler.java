package net.consensys.athena.impl.http.handler.partyinfo;

import net.consensys.athena.api.network.NetworkNodes;
import net.consensys.athena.impl.http.server.HttpContentType;
import net.consensys.athena.impl.network.MemoryNetworkNodes;
import net.consensys.athena.impl.utils.Serializer;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

/**
 * Used as a part of the network discovery process. Look up the binary list of constellation nodes
 * that a node has knowledge of.
 */
public class PartyInfoHandler implements Handler<RoutingContext> {

  private final NetworkNodes networkNodes;
  private final Serializer serializer;

  public PartyInfoHandler(NetworkNodes networkNodes, Serializer serializer) {
    this.networkNodes = networkNodes;
    this.serializer = serializer;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    NetworkNodes callerPeers =
        serializer.deserialize(
            HttpContentType.CBOR, MemoryNetworkNodes.class, routingContext.getBody().getBytes());
    Buffer toReturn = Buffer.buffer(serializer.serialize(HttpContentType.CBOR, networkNodes));
    routingContext.response().end(toReturn);

    // merge callerPeers into our peers
    networkNodes.merge(callerPeers);
  }
}

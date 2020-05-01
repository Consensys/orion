/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.orion.http.handler.partyinfo;

import net.consensys.orion.http.server.HttpContentType;
import net.consensys.orion.network.PersistentNetworkNodes;
import net.consensys.orion.network.ReadOnlyNetworkNodes;
import net.consensys.orion.utils.Serializer;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

/**
 * Used as a part of the network discovery process. Look up the binary list of constellation nodes that a node has
 * knowledge of.
 */
public class PartyInfoHandler implements Handler<RoutingContext> {
  private final PersistentNetworkNodes networkNodes;

  public PartyInfoHandler(final PersistentNetworkNodes networkNodes) {
    this.networkNodes = networkNodes;
  }

  @Override
  public void handle(final RoutingContext routingContext) {
    final ReadOnlyNetworkNodes callerPeers =
        Serializer.deserialize(HttpContentType.CBOR, ReadOnlyNetworkNodes.class, routingContext.getBody().getBytes());
    final Buffer toReturn = Buffer.buffer(Serializer.serialize(HttpContentType.CBOR, networkNodes));
    routingContext.response().end(toReturn);

    // merge callerPeers into our peers
    System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&& PARTY INFO HANDLER");
    networkNodes.merge(callerPeers);
  }
}

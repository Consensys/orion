/*
 * Copyright 2019 ConsenSys AG.
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
package net.consensys.orion.http.handler.knownnodes;

import net.consensys.orion.http.server.HttpContentType;
import net.consensys.orion.network.PersistentNetworkNodes;
import net.consensys.orion.utils.Serializer;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

public class KnownNodesHandler implements Handler<RoutingContext> {

  private final PersistentNetworkNodes networkNodes;

  public KnownNodesHandler(final PersistentNetworkNodes networkNodes) {
    this.networkNodes = networkNodes;
  }

  @Override
  public void handle(final RoutingContext routingContext) {
    final List<KnownNode> knownNodes = new ArrayList<>();

    networkNodes.nodePKs().forEach((entry) -> {
      if (!entry.getValue().equals(networkNodes.uri())) {
        knownNodes.add(new KnownNode(entry.getKey(), entry.getValue()));
      }
    });

    final Buffer bufferResponse = Buffer.buffer(Serializer.serialize(HttpContentType.JSON, knownNodes));

    routingContext.response().end(bufferResponse);
  }
}

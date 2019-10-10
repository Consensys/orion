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
package net.consensys.orion.http.handler.send;

import static net.consensys.orion.http.server.HttpContentType.JSON;

import net.consensys.orion.exception.OrionErrorCode;
import net.consensys.orion.exception.OrionException;
import net.consensys.orion.payload.DistributePayloadManager;
import net.consensys.orion.utils.Serializer;

import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

public class SendHandler implements Handler<RoutingContext> {

  private final DistributePayloadManager distributePayloadManager;

  public SendHandler(final DistributePayloadManager distributePayloadManager) {
    this.distributePayloadManager = distributePayloadManager;
  }

  @Override
  public void handle(final RoutingContext routingContext) {
    final SendRequest sendRequest = parseRequest(routingContext);

    distributePayloadManager.processSendRequest(sendRequest, res -> {
      if (res.succeeded()) {
        routingContext.response().end(Json.encodeToBuffer(res.result()));
      } else {
        routingContext.fail(res.cause());
      }
    });
  }

  private SendRequest parseRequest(final RoutingContext routingContext) {
    final SendRequest sendRequest =
        Serializer.deserialize(JSON, SendRequest.class, routingContext.getBody().getBytes());

    if (!sendRequest.isValid()) {
      throw new OrionException(OrionErrorCode.INVALID_PAYLOAD);
    }

    return sendRequest;
  }

}

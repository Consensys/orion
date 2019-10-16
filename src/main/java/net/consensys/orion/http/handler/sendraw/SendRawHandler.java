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
package net.consensys.orion.http.handler.sendraw;

import net.consensys.orion.exception.OrionErrorCode;
import net.consensys.orion.exception.OrionException;
import net.consensys.orion.http.handler.send.SendRequest;
import net.consensys.orion.payload.DistributePayloadManager;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class SendRawHandler implements Handler<RoutingContext> {

  private final DistributePayloadManager distributePayloadManager;

  public SendRawHandler(final DistributePayloadManager distributePayloadManager) {
    this.distributePayloadManager = distributePayloadManager;
  }

  @Override
  public void handle(final RoutingContext routingContext) {
    final SendRequest sendRequest = parseRequest(routingContext);

    distributePayloadManager.processSendRequest(sendRequest, res -> {
      if (res.succeeded()) {
        routingContext.response().end(res.result().getKey());
      } else {
        routingContext.fail(res.cause());
      }
    });
  }

  private SendRequest parseRequest(final RoutingContext routingContext) {
    final String from = routingContext.request().getHeader("c11n-from");
    final String toList = routingContext.request().getHeader("c11n-to");
    final String[] to = toList != null ? toList.split(",") : new String[0];

    final SendRequest sendRequest = new SendRequest(routingContext.getBody().getBytes(), from, to);

    if (!sendRequest.isValid()) {
      throw new OrionException(OrionErrorCode.INVALID_PAYLOAD);
    }

    return sendRequest;
  }
}

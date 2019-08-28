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
package net.consensys.orion.http.handler.privacy;

import static net.consensys.orion.http.server.HttpContentType.CBOR;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.orion.exception.OrionErrorCode;
import net.consensys.orion.exception.OrionException;
import net.consensys.orion.network.ConcurrentNetworkNodes;
import net.consensys.orion.utils.Serializer;

import java.io.Serializable;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

abstract class PrivacyGroupBaseHandler {

  private final Logger log = LogManager.getLogger();
  private final ConcurrentNetworkNodes networkNodes;
  private final HttpClient httpClient;

  PrivacyGroupBaseHandler(ConcurrentNetworkNodes networkNodes, HttpClient httpClient) {
    this.networkNodes = networkNodes;
    this.httpClient = httpClient;
  }

  @SuppressWarnings("unchecked")
  CompletableFuture<Boolean>[] sendRequestsToOthers(
      Stream<Box.PublicKey> addresses,
      Serializable request,
      String endpoint) {
    return addresses.map(pKey -> {
      URL recipientURL = networkNodes.urlForRecipient(pKey);

      log.info("Propagating request to {} with URL {} {}", pKey, recipientURL.toString(), endpoint);

      final CompletableFuture<Boolean> responseFuture = new CompletableFuture<>();
      final byte[] payload = Serializer.serialize(CBOR, request);

      httpClient
          .post(recipientURL.getPort(), recipientURL.getHost(), endpoint)
          .putHeader("Content-Type", "application/cbor")
          .handler(response -> handleResponse(endpoint, pKey, recipientURL, responseFuture, response))
          .exceptionHandler(
              ex -> responseFuture.completeExceptionally(new OrionException(OrionErrorCode.NODE_PUSHING_TO_PEER, ex)))
          .end(Buffer.buffer(payload));
      return responseFuture;
    }).toArray(CompletableFuture[]::new);
  }

  private void handleResponse(
      String endpoint,
      Box.PublicKey pKey,
      URL recipientURL,
      CompletableFuture<Boolean> responseFuture,
      HttpClientResponse response) {
    response.bodyHandler(responseBody -> {
      log.info("{} with URL {} responded with {}", pKey, recipientURL.toString(), response.statusCode());
      if (response.statusCode() != 200) {
        responseFuture.completeExceptionally(new OrionException(OrionErrorCode.NODE_PROPAGATING_TO_ALL_PEERS));
      } else {
        log.info("Success for {}", endpoint);
        responseFuture.complete(true);
      }
    });
  }

  void handleFailure(final RoutingContext routingContext, final Throwable ex) {
    log.warn("propagating the payload failed");

    final Throwable cause = ex.getCause();
    if (cause instanceof OrionException) {
      routingContext.fail(cause);
    } else {
      routingContext.fail(new OrionException(OrionErrorCode.NODE_PROPAGATING_TO_ALL_PEERS, ex));
    }
  }
}

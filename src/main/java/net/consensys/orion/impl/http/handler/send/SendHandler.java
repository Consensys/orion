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

package net.consensys.orion.impl.http.handler.send;

import static net.consensys.orion.impl.http.server.HttpContentType.JSON;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.orion.api.config.Config;
import net.consensys.orion.api.enclave.Enclave;
import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.api.exception.OrionException;
import net.consensys.orion.api.storage.Storage;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.network.ConcurrentNetworkNodes;
import net.consensys.orion.impl.network.NodeHttpClientBuilder;
import net.consensys.orion.impl.utils.Serializer;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Send a base64 encoded payload to encrypt. */
public class SendHandler implements Handler<RoutingContext> {
  private static final Logger log = LogManager.getLogger();

  private final Enclave enclave;
  private final Storage<EncryptedPayload> storage;
  private final List<Box.PublicKey> nodeKeys;
  private final ConcurrentNetworkNodes networkNodes;
  private final HttpContentType contentType;

  private final HttpClient httpClient;

  public SendHandler(
      Vertx vertx,
      Enclave enclave,
      Storage<EncryptedPayload> storage,
      ConcurrentNetworkNodes networkNodes,
      HttpContentType contentType,
      Config config) {
    this.enclave = enclave;
    this.storage = storage;
    this.nodeKeys = Arrays.asList(enclave.nodeKeys());
    this.networkNodes = networkNodes;
    this.contentType = contentType;
    this.httpClient = NodeHttpClientBuilder.build(vertx, config, 1500);
  }

  @Override
  public void handle(RoutingContext routingContext) {
    final SendRequest sendRequest;
    if (contentType == JSON) {
      sendRequest = Serializer.deserialize(JSON, SendRequest.class, routingContext.getBody().getBytes());
    } else {
      sendRequest = binaryRequest(routingContext);
    }
    log.debug(sendRequest);

    if (!sendRequest.isValid()) {
      throw new OrionException(OrionErrorCode.INVALID_PAYLOAD);
    }

    log.debug("reading public keys from SendRequest object");
    // read provided public keys
    Box.PublicKey fromKey = sendRequest.from().map(enclave::readKey).orElseGet(() -> {
      if (nodeKeys.isEmpty()) {
        throw new OrionException(OrionErrorCode.NO_SENDER_KEY);
      }
      return nodeKeys.get(0);
    });

    final List<Box.PublicKey> toKeys =
        Arrays.stream(sendRequest.to()).map(enclave::readKey).collect(Collectors.toList());

    // toKeys = toKeys + [nodeAlwaysSendTo] --> default pub key to always send to
    toKeys.addAll(Arrays.asList(enclave.alwaysSendTo()));
    Box.PublicKey[] arrToKeys = toKeys.toArray(new Box.PublicKey[0]);

    // convert payload from b64 to bytes
    final byte[] rawPayload = sendRequest.rawPayload();

    // encrypting payload
    log.debug("encrypting payload from SendRequest object");
    final EncryptedPayload encryptedPayload = enclave.encrypt(rawPayload, fromKey, arrToKeys);

    List<Box.PublicKey> keys = toKeys.stream().filter(pKey -> !nodeKeys.contains(pKey)).collect(Collectors.toList());

    if (keys.stream().anyMatch(pKey -> networkNodes.urlForRecipient(pKey) == null)) {
      routingContext.fail(new OrionException(OrionErrorCode.NODE_MISSING_PEER_URL, "couldn't find peer URL"));
      return;
    }

    // storing payload
    log.debug("Generate payload digest");
    final String digest = storage.generateDigest(encryptedPayload);

    // propagate payload
    log.debug("propagating payload");

    @SuppressWarnings("rawtypes")
    CompletableFuture[] cfs = keys.stream().map(pKey -> {
      URL recipientURL = networkNodes.urlForRecipient(pKey);

      CompletableFuture<Boolean> responseFuture = new CompletableFuture<>();

      // serialize payload, stripping non-relevant encryptedKeys, and configureRoutes payload
      final byte[] payload = Serializer.serialize(HttpContentType.CBOR, encryptedPayload.stripFor(pKey));

      // execute request
      httpClient
          .post(recipientURL.getPort(), recipientURL.getHost(), "/push")
          .putHeader("Content-Type", "application/cbor")
          .handler(response -> response.bodyHandler(responseBody -> {
            if (response.statusCode() != 200 || !digest.equals(responseBody.toString())) {
              responseFuture.completeExceptionally(new OrionException(OrionErrorCode.NODE_PROPAGATING_TO_ALL_PEERS));
            } else {
              responseFuture.complete(true);
            }
          }))
          .exceptionHandler(
              ex -> responseFuture.completeExceptionally(new OrionException(OrionErrorCode.NODE_PUSHING_TO_PEER, ex)))
          .end(Buffer.buffer(payload));

      return responseFuture;
    }).toArray(CompletableFuture[]::new);

    CompletableFuture.allOf(cfs).whenComplete((all, ex) -> {
      if (ex != null) {
        handleFailure(routingContext, ex);
        return;
      }
      storage.put(encryptedPayload).thenAccept((result) -> {

        final Buffer responseData;
        if (contentType == JSON) {
          responseData = Buffer.buffer(Serializer.serialize(JSON, Collections.singletonMap("key", digest)));
        } else {
          responseData = Buffer.buffer(digest);
        }
        routingContext.response().end(responseData);
      }).exceptionally(e -> handleFailure(routingContext, e));
    });
  }

  private void handleFailure(RoutingContext routingContext, Throwable ex) {
    log.warn("propagating the payload failed");

    Throwable cause = ex.getCause();
    if (cause instanceof OrionException) {
      routingContext.fail(cause);
    } else {
      routingContext.fail(new OrionException(OrionErrorCode.NODE_PROPAGATING_TO_ALL_PEERS, ex));
    }
  }

  private SendRequest binaryRequest(RoutingContext routingContext) {
    String from = routingContext.request().getHeader("c11n-from");
    String[] to = routingContext.request().getHeader("c11n-to").split(",");
    return new SendRequest(routingContext.getBody().getBytes(), from, to);
  }
}

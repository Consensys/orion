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

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.crypto.sodium.Box.PublicKey;
import net.consensys.orion.config.Config;
import net.consensys.orion.enclave.Enclave;
import net.consensys.orion.enclave.EncryptedPayload;
import net.consensys.orion.enclave.PrivacyGroupPayload;
import net.consensys.orion.enclave.QueryPrivacyGroupPayload;
import net.consensys.orion.exception.OrionErrorCode;
import net.consensys.orion.exception.OrionException;
import net.consensys.orion.http.server.HttpContentType;
import net.consensys.orion.network.ConcurrentNetworkNodes;
import net.consensys.orion.network.NodeHttpClientBuilder;
import net.consensys.orion.storage.Storage;
import net.consensys.orion.utils.Serializer;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
  private final Storage<PrivacyGroupPayload> privacyGroupStorage;
  private final Storage<QueryPrivacyGroupPayload> queryPrivacyGroupStorage;
  private final List<Box.PublicKey> nodeKeys;
  private final ConcurrentNetworkNodes networkNodes;
  private final SendRequestParser parser;

  private final HttpClient httpClient;

  public SendHandler(
      final Vertx vertx,
      final Enclave enclave,
      final Storage<EncryptedPayload> storage,
      final Storage<PrivacyGroupPayload> privacyGroupStorage,
      final Storage<QueryPrivacyGroupPayload> queryPrivacyGroupStorage,
      final ConcurrentNetworkNodes networkNodes,
      final SendRequestParser parser,
      final Config config) {
    this.enclave = enclave;
    this.storage = storage;
    this.privacyGroupStorage = privacyGroupStorage;
    this.queryPrivacyGroupStorage = queryPrivacyGroupStorage;
    this.nodeKeys = Arrays.asList(enclave.nodeKeys());
    this.networkNodes = networkNodes;
    this.parser = parser;
    this.httpClient = NodeHttpClientBuilder.build(vertx, config, 1500);
  }

  @Override
  public void handle(final RoutingContext routingContext) {
    final SendRequest sendRequest = parser.parse(routingContext);
    final PublicKey fromKey = readPublicKey(sendRequest);

    if (sendRequest.privacyGroupId().isEmpty()) {
      handleLegacySendRequest(routingContext, sendRequest, fromKey);
    } else if (sendRequest.privacyGroupId().isPresent()) {
      handlePrivacyGroupSendRequest(routingContext, sendRequest, fromKey);
    }
  }

  private PublicKey readPublicKey(SendRequest sendRequest) {
    log.debug("reading public keys from SendRequest object");
    // read provided public keys
    return sendRequest.from().map(enclave::readKey).orElseGet(() -> {
      if (nodeKeys.isEmpty()) {
        throw new OrionException(OrionErrorCode.NO_SENDER_KEY);
      }
      return nodeKeys.get(0);
    });
  }

  private void handleLegacySendRequest(RoutingContext routingContext, SendRequest sendRequest, PublicKey fromKey) {
    final List<PublicKey> toKeys = Arrays.stream(sendRequest.to()).map(enclave::readKey).collect(Collectors.toList());
    final ArrayList<String> keys = new ArrayList<>(Arrays.stream(sendRequest.to()).collect(Collectors.toList()));
    if (sendRequest.from().isPresent()) {
      keys.add(sendRequest.from().get());
    }
    final PrivacyGroupPayload privacyGroupPayload = new PrivacyGroupPayload(
        keys.toArray(new String[0]),
        "legacy",
        "Privacy groups to support the creation of groups by privateFor and privateFrom",
        PrivacyGroupPayload.State.ACTIVE,
        PrivacyGroupPayload.Type.LEGACY,
        null);

    final QueryPrivacyGroupPayload queryPrivacyGroupPayload =
        new QueryPrivacyGroupPayload(keys.toArray(new String[0]), null);
    final String key = queryPrivacyGroupStorage.generateDigest(queryPrivacyGroupPayload);

    queryPrivacyGroupStorage.get(key).thenApply(privacyGroup -> {
      if (privacyGroup.isPresent()) {
        sendPayloadToParticipants(routingContext, sendRequest, fromKey, toKeys, privacyGroupPayload);
        return key;
      } else {
        return privacyGroupStorage.put(privacyGroupPayload).thenApply((result) -> {
          queryPrivacyGroupPayload.setPrivacyGroupToAppend(privacyGroupStorage.generateDigest(privacyGroupPayload));
          return queryPrivacyGroupStorage.update(key, queryPrivacyGroupPayload).thenApply((res) -> {
            sendPayloadToParticipants(routingContext, sendRequest, fromKey, toKeys, privacyGroupPayload);
            return result;
          }).exceptionally(e -> {
            handleFailure(routingContext, e, ErrorType.FIND_GROUP);
            return null;
          });
        }).exceptionally(e -> {
          handleFailure(routingContext, e, ErrorType.FIND_GROUP);
          return null;
        });
      }
    }).exceptionally(e -> {
      handleFailure(routingContext, e, ErrorType.FIND_GROUP);
      return null;
    });
  }

  private void handlePrivacyGroupSendRequest(
      RoutingContext routingContext,
      SendRequest sendRequest,
      PublicKey fromKey) {
    privacyGroupStorage.get(sendRequest.privacyGroupId().get()).thenApply((result) -> {
      if (result.get().state().equals(PrivacyGroupPayload.State.ACTIVE)) {
        List<PublicKey> toKeys =
            Arrays.stream(result.get().addresses()).map(enclave::readKey).collect(Collectors.toList());
        toKeys.remove(fromKey);
        sendPayloadToParticipants(routingContext, sendRequest, fromKey, toKeys, result.get());
        return result;
      } else {
        handleFailure(
            routingContext,
            new OrionException(OrionErrorCode.ENCLAVE_PRIVACY_GROUP_MISSING, "privacy group not found"),
            ErrorType.FIND_GROUP);
        return result;
      }
    }).exceptionally(e -> {
      handleFailure(routingContext, e, ErrorType.FIND_GROUP);
      return Optional.empty();
    });
  }

  private void sendPayloadToParticipants(
      final RoutingContext routingContext,
      final SendRequest sendRequest,
      final Box.PublicKey fromKey,
      final List<Box.PublicKey> toKeys,
      final PrivacyGroupPayload privacyGroupPayload) {
    // toKeys = toKeys + [nodeAlwaysSendTo] --> default pub key to always send to
    toKeys.addAll(Arrays.asList(enclave.alwaysSendTo()));
    final Box.PublicKey[] arrToKeys = toKeys.toArray(new Box.PublicKey[0]);

    // convert payload from b64 to bytes
    final byte[] rawPayload = sendRequest.rawPayload();

    // encrypting payload
    log.debug("encrypting payload from SendRequest object");
    final EncryptedPayload encryptedPayload =
        enclave.encrypt(rawPayload, fromKey, arrToKeys, privacyGroupPayload.randomSeed());

    final List<Box.PublicKey> keys =
        toKeys.stream().filter(pKey -> !nodeKeys.contains(pKey)).collect(Collectors.toList());

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
    final CompletableFuture[] cfs = keys.stream().map(pKey -> {
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
        handleFailure(routingContext, ex, ErrorType.PROPAGATE_ALL_PEERS);
        return;
      }
      storage.put(encryptedPayload).thenAccept((result) -> {

        final Buffer responseData;
        if (parser.getContentType() == JSON) {
          responseData = Buffer.buffer(Serializer.serialize(JSON, Collections.singletonMap("key", digest)));
        } else {
          responseData = Buffer.buffer(digest);
        }
        routingContext.response().end(responseData);
      }).exceptionally(e -> handleFailure(routingContext, e, ErrorType.PROPAGATE_ALL_PEERS));
    });
  }

  enum ErrorType {
    STORE_GROUP, FIND_GROUP, PROPAGATE_ALL_PEERS,
  }

  private void handleFailure(final RoutingContext routingContext, final Throwable ex, final ErrorType errorType) {
    final Throwable cause = ex.getCause();
    if (cause instanceof OrionException) {
      routingContext.fail(cause);
    } else {
      switch (errorType) {
        case STORE_GROUP:
          log.warn("storing the privacy group failed");
          routingContext.fail(new OrionException(OrionErrorCode.ENCLAVE_UNABLE_STORE_PRIVACY_GROUP, ex));
          break;
        case FIND_GROUP:
          log.warn("finding the privacy group failed");
          routingContext.fail(new OrionException(OrionErrorCode.ENCLAVE_PRIVACY_GROUP_MISSING, ex));
          break;
        case PROPAGATE_ALL_PEERS:
          log.warn("propagating the payload failed");
          routingContext.fail(new OrionException(OrionErrorCode.NODE_PROPAGATING_TO_ALL_PEERS, ex));
          break;
      }
    }
  }

}

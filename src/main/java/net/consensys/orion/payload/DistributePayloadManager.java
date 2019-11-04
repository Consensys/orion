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
package net.consensys.orion.payload;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.crypto.sodium.Box.PublicKey;
import net.consensys.orion.config.Config;
import net.consensys.orion.enclave.Enclave;
import net.consensys.orion.enclave.EncryptedPayload;
import net.consensys.orion.enclave.PrivacyGroupPayload;
import net.consensys.orion.enclave.QueryPrivacyGroupPayload;
import net.consensys.orion.exception.OrionErrorCode;
import net.consensys.orion.exception.OrionException;
import net.consensys.orion.http.handler.send.SendRequest;
import net.consensys.orion.http.handler.send.SendResponse;
import net.consensys.orion.http.server.HttpContentType;
import net.consensys.orion.network.ConcurrentNetworkNodes;
import net.consensys.orion.network.NodeHttpClientBuilder;
import net.consensys.orion.storage.Storage;
import net.consensys.orion.utils.Serializer;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class DistributePayloadManager {

  private static final Logger log = LogManager.getLogger();

  private final Enclave enclave;
  private final Storage<EncryptedPayload> storage;
  private final Storage<PrivacyGroupPayload> privacyGroupStorage;
  private final Storage<QueryPrivacyGroupPayload> queryPrivacyGroupStorage;
  private final ConcurrentNetworkNodes networkNodes;
  private final List<PublicKey> nodeKeys;
  private final HttpClient httpClient;

  public DistributePayloadManager(
      final Vertx vertx,
      final Config config,
      final Enclave enclave,
      final Storage<EncryptedPayload> storage,
      final Storage<PrivacyGroupPayload> privacyGroupStorage,
      final Storage<QueryPrivacyGroupPayload> queryPrivacyGroupStorage,
      final ConcurrentNetworkNodes networkNodes) {
    this(
        enclave,
        storage,
        privacyGroupStorage,
        queryPrivacyGroupStorage,
        networkNodes,
        NodeHttpClientBuilder.build(vertx, config, 1500));
  }

  @VisibleForTesting
  DistributePayloadManager(
      final Enclave enclave,
      final Storage<EncryptedPayload> storage,
      final Storage<PrivacyGroupPayload> privacyGroupStorage,
      final Storage<QueryPrivacyGroupPayload> queryPrivacyGroupStorage,
      final ConcurrentNetworkNodes networkNodes,
      final HttpClient httpClient) {
    this.enclave = enclave;
    this.storage = storage;
    this.privacyGroupStorage = privacyGroupStorage;
    this.queryPrivacyGroupStorage = queryPrivacyGroupStorage;
    this.networkNodes = networkNodes;
    this.httpClient = httpClient;

    this.nodeKeys = Arrays.asList(enclave.nodeKeys());
  }

  public void processSendRequest(final SendRequest sendRequest, final Handler<AsyncResult<SendResponse>> handler) {

    final PublicKey fromKey;
    try {
      fromKey = readPublicKey(sendRequest);
    } catch (final Exception e) {
      handler.handle(Future.failedFuture(e));
      return;
    }

    final Future<PrivacyGroupPayload> privacyGroupPayloadFuture;
    if (sendRequest.privacyGroupId().isEmpty()) {
      privacyGroupPayloadFuture = handleLegacySendRequest(sendRequest);
    } else {
      privacyGroupPayloadFuture = handlePrivacyGroupSendRequest(sendRequest);
    }

    privacyGroupPayloadFuture
        .compose(pgPayload -> sendPayloadToParticipants(sendRequest, fromKey, pgPayload))
        .setHandler(ar -> {
          if (ar.succeeded()) {
            handler.handle(Future.succeededFuture(new SendResponse(ar.result())));
          } else {
            handler.handle(Future.failedFuture(ar.cause()));
          }
        });
  }

  private Future<PrivacyGroupPayload> handleLegacySendRequest(final SendRequest sendRequest) {
    final Future<PrivacyGroupPayload> future = Future.future();

    try {
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
          future.complete(privacyGroupPayload);
          return key;
        } else {
          return privacyGroupStorage.put(privacyGroupPayload).thenApply(result -> {
            queryPrivacyGroupPayload.setPrivacyGroupToAppend(privacyGroupStorage.generateDigest(privacyGroupPayload));
            return queryPrivacyGroupStorage.update(key, queryPrivacyGroupPayload).thenApply((res) -> {
              future.complete(privacyGroupPayload);
              return result;
            }).exceptionally(e -> {
              future.fail(new OrionException(OrionErrorCode.ENCLAVE_UNABLE_STORE_PRIVACY_GROUP));
              return null;
            });
          }).exceptionally(e -> {
            future.fail(new OrionException(OrionErrorCode.ENCLAVE_UNABLE_STORE_PRIVACY_GROUP));
            return null;
          });
        }
      }).exceptionally(e -> {
        future.fail(new OrionException(OrionErrorCode.ENCLAVE_PRIVACY_QUERY_ERROR));
        return null;
      });
    } catch (final OrionException e) {
      future.fail(e);
    }

    return future;
  }

  private Future<PrivacyGroupPayload> handlePrivacyGroupSendRequest(final SendRequest sendRequest) {
    final Future<PrivacyGroupPayload> future = Future.future();

    try {
      final String privacyGroupId = sendRequest.privacyGroupId().get();
      privacyGroupStorage.get(privacyGroupId).thenApply((result) -> {
        if (result.get().state().equals(PrivacyGroupPayload.State.ACTIVE)) {
          future.complete(result.get());
          return result;
        } else {
          future.fail(new OrionException(OrionErrorCode.ENCLAVE_PRIVACY_GROUP_MISSING, "privacy group not found"));
          return result;
        }
      }).exceptionally(e -> {
        future.fail(new OrionException(OrionErrorCode.ENCLAVE_PRIVACY_GROUP_MISSING, "privacy group not found"));
        return Optional.empty();
      });
    } catch (final OrionException e) {
      future.fail(e);
    }

    return future;
  }

  private Future<String> sendPayloadToParticipants(
      final SendRequest sendRequest,
      final PublicKey fromKey,
      final PrivacyGroupPayload privacyGroupPayload) {
    final Future<String> future = Future.future();

    try {
      final Optional<String> from = sendRequest.from();
      final List<PublicKey> toKeys = Arrays
          .stream(privacyGroupPayload.addresses())
          .filter(key -> from.isEmpty() || !key.equals(from.get()))
          .map(enclave::readKey)
          .collect(Collectors.toList());

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
        throw new OrionException(OrionErrorCode.NODE_MISSING_PEER_URL, "couldn't find peer URL");
      }

      @SuppressWarnings("URLEqualsHashCode")
      final Map<URL, ArrayList<PublicKey>> urlToKeysMap = getUrlToKeyListMap(keys);

      log.debug("Generate payload digest");
      final String digest = storage.generateDigest(encryptedPayload);

      log.debug("propagating payload");
      @SuppressWarnings("rawtypes")
      final CompletableFuture[] cfs = urlToKeysMap.keySet().stream().map(url -> {

        CompletableFuture<Boolean> responseFuture = new CompletableFuture<>();

        // serialize payload, stripping non-relevant encryptedKeys, and configureRoutes payload
        final byte[] payload =
            Serializer.serialize(HttpContentType.CBOR, encryptedPayload.stripFor(urlToKeysMap.get(url)));

        // execute request
        httpClient
            .post(url.getPort(), url.getHost(), "/push")
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
          future.fail(new OrionException(OrionErrorCode.NODE_PROPAGATING_TO_ALL_PEERS, ex));
        }
        storage.put(encryptedPayload).thenAccept(future::complete).exceptionally(e -> {
          future.fail(new OrionException(OrionErrorCode.NODE_PROPAGATING_TO_ALL_PEERS, ex));
        });
      });

    } catch (final OrionException e) {
      future.fail(e);
    }

    return future;
  }

  @NotNull
  private Map<URL, ArrayList<PublicKey>> getUrlToKeyListMap(final List<PublicKey> keys) {
    @SuppressWarnings("URLEqualsHashCode")
    final Map<URL, ArrayList<PublicKey>> urlToKeysMap = new HashMap<>();
    for (final PublicKey key : keys) {
      final URL url = networkNodes.urlForRecipient(key);
      if (!urlToKeysMap.containsKey(url)) {
        urlToKeysMap.put(url, new ArrayList<>());
      }
      urlToKeysMap.get(url).add(key);
    }
    return urlToKeysMap;
  }

  private PublicKey readPublicKey(final SendRequest sendRequest) {
    log.debug("reading public keys from SendRequest object");
    // read provided public keys
    return sendRequest.from().map(enclave::readKey).orElseGet(() -> {
      if (nodeKeys.isEmpty()) {
        throw new OrionException(OrionErrorCode.NO_SENDER_KEY);
      }
      return nodeKeys.get(0);
    });
  }
}

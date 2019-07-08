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

import static net.consensys.orion.http.server.HttpContentType.JSON;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.orion.config.Config;
import net.consensys.orion.enclave.Enclave;
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
import java.security.SecureRandom;
import java.util.Arrays;
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

/**
 * Create a privacyGroup given the list of addresses.
 */
public class CreatePrivacyGroupHandler implements Handler<RoutingContext> {


  private static final Logger log = LogManager.getLogger();

  private final Storage<PrivacyGroupPayload> privacyGroupStorage;
  private final Storage<QueryPrivacyGroupPayload> queryPrivacyGroupStorage;
  private final ConcurrentNetworkNodes networkNodes;
  private final Enclave enclave;
  private final HttpClient httpClient;

  public CreatePrivacyGroupHandler(
      Storage<PrivacyGroupPayload> privacyGroupStorage,
      Storage<QueryPrivacyGroupPayload> queryPrivacyGroupStorage,
      ConcurrentNetworkNodes networkNodes,
      Enclave enclave,
      Vertx vertx,
      Config config) {
    this.privacyGroupStorage = privacyGroupStorage;
    this.queryPrivacyGroupStorage = queryPrivacyGroupStorage;
    this.networkNodes = networkNodes;
    this.enclave = enclave;
    this.httpClient = NodeHttpClientBuilder.build(vertx, config, 1500);
  }

  @Override
  public void handle(RoutingContext routingContext) {

    byte[] request = routingContext.getBody().getBytes();
    PrivacyGroupRequest privacyGroup = Serializer.deserialize(JSON, PrivacyGroupRequest.class, request);

    if (!Arrays.asList(privacyGroup.addresses()).contains(privacyGroup.from())) {
      routingContext.fail(
          new OrionException(OrionErrorCode.CREATE_GROUP_INCLUDE_SELF, "the list of addresses should include self "));
      return;
    }

    byte[] bytes = new byte[20];
    if (privacyGroup.getSeed().isPresent()) {
      bytes = privacyGroup.getSeed().get();
    } else {
      SecureRandom random = new SecureRandom();
      random.nextBytes(bytes);
    }

    PrivacyGroupPayload privacyGroupPayload = new PrivacyGroupPayload(
        privacyGroup.addresses(),
        privacyGroup.name(),
        privacyGroup.description(),
        PrivacyGroupPayload.State.ACTIVE,
        PrivacyGroupPayload.Type.PANTHEON,
        bytes);
    final String privacyGroupId = privacyGroupStorage.generateDigest(privacyGroupPayload);

    List<Box.PublicKey> addressListToForward = Arrays
        .stream(privacyGroup.addresses())
        .filter(key -> !key.equals(privacyGroup.from())) // don't forward to self
        .distinct()
        .map(enclave::readKey)
        .collect(Collectors.toList());

    if (addressListToForward.stream().anyMatch(pKey -> networkNodes.urlForRecipient(pKey) == null)) {
      routingContext.fail(new OrionException(OrionErrorCode.NODE_MISSING_PEER_URL, "couldn't find peer URL "));
      return;
    }
    // propagate payload
    log.debug("propagating payload");

    @SuppressWarnings("rawtypes")
    CompletableFuture[] cfs = addressListToForward.stream().map(pKey -> {
      URL recipientURL = networkNodes.urlForRecipient(pKey);

      CompletableFuture<Boolean> responseFuture = new CompletableFuture<>();

      // serialize payload, stripping non-relevant encryptedKeys, and configureRoutes payload
      final byte[] payload = Serializer.serialize(HttpContentType.CBOR, privacyGroupPayload);

      // execute request
      httpClient
          .post(recipientURL.getPort(), recipientURL.getHost(), "/pushPrivacyGroup")
          .putHeader("Content-Type", "application/cbor")
          .handler(response -> response.bodyHandler(responseBody -> {
            if (response.statusCode() != 200 || !privacyGroupId.equals(responseBody.toString())) {
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
      privacyGroupStorage.put(privacyGroupPayload).thenAccept((result) -> {

        QueryPrivacyGroupPayload queryPrivacyGroupPayload =
            new QueryPrivacyGroupPayload(privacyGroupPayload.addresses(), null);

        queryPrivacyGroupPayload.setPrivacyGroupToAppend(privacyGroupId);
        String key = queryPrivacyGroupStorage.generateDigest(queryPrivacyGroupPayload);
        queryPrivacyGroupStorage.update(key, queryPrivacyGroupPayload).thenAccept((res) -> {

          PrivacyGroup group = new PrivacyGroup(
              privacyGroupId,
              PrivacyGroupPayload.Type.PANTHEON,
              privacyGroup.name(),
              privacyGroup.description());

          Buffer toReturn = Buffer.buffer(Serializer.serialize(JSON, group));
          routingContext.response().end(toReturn);

        }).exceptionally(
            e -> routingContext.fail(new OrionException(OrionErrorCode.ENCLAVE_UNABLE_STORE_PRIVACY_GROUP, e)));

      }).exceptionally(
          e -> routingContext.fail(new OrionException(OrionErrorCode.ENCLAVE_UNABLE_STORE_PRIVACY_GROUP, e)));
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
}

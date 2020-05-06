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

import net.consensys.orion.config.Config;
import net.consensys.orion.enclave.PrivacyGroupPayload;
import net.consensys.orion.enclave.QueryPrivacyGroupPayload;
import net.consensys.orion.exception.OrionErrorCode;
import net.consensys.orion.exception.OrionException;
import net.consensys.orion.http.server.HttpContentType;
import net.consensys.orion.network.NodeHttpClientBuilder;
import net.consensys.orion.network.PersistentNetworkNodes;
import net.consensys.orion.storage.Storage;
import net.consensys.orion.utils.Serializer;

import java.net.URI;
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
import org.apache.tuweni.bytes.Bytes;

/**
 * Create a privacyGroup given the list of addresses.
 */
public class CreatePrivacyGroupHandler implements Handler<RoutingContext> {

  private static final Logger log = LogManager.getLogger();

  private final Storage<PrivacyGroupPayload> privacyGroupStorage;
  private final Storage<QueryPrivacyGroupPayload> queryPrivacyGroupStorage;
  private final PersistentNetworkNodes networkNodes;
  private final HttpClient httpClient;

  public CreatePrivacyGroupHandler(
      final Storage<PrivacyGroupPayload> privacyGroupStorage,
      final Storage<QueryPrivacyGroupPayload> queryPrivacyGroupStorage,
      final PersistentNetworkNodes networkNodes,
      final Vertx vertx,
      final Config config) {
    this.privacyGroupStorage = privacyGroupStorage;
    this.queryPrivacyGroupStorage = queryPrivacyGroupStorage;
    this.networkNodes = networkNodes;
    this.httpClient = NodeHttpClientBuilder.build(vertx, config, 1500);
  }

  @Override
  public void handle(final RoutingContext routingContext) {

    final byte[] request = routingContext.getBody().getBytes();
    final PrivacyGroupRequest privacyGroupRequest = Serializer.deserialize(JSON, PrivacyGroupRequest.class, request);

    if (!Arrays.asList(privacyGroupRequest.addresses()).contains(privacyGroupRequest.from())) {
      routingContext.fail(
          new OrionException(OrionErrorCode.CREATE_GROUP_INCLUDE_SELF, "the list of addresses should include self "));
      return;
    }

    final byte[] bytes;
    if (privacyGroupRequest.getSeed().isPresent()) {
      bytes = privacyGroupRequest.getSeed().get();
    } else {
      final SecureRandom random = new SecureRandom();
      bytes = new byte[20];
      random.nextBytes(bytes);
    }

    log.info("Creating privacy group for {}", Arrays.toString(privacyGroupRequest.addresses()));

    final PrivacyGroupPayload privacyGroupPayload = new PrivacyGroupPayload(
        privacyGroupRequest.addresses(),
        privacyGroupRequest.name() == null || privacyGroupRequest.name().isBlank() ? "" : privacyGroupRequest.name(),
        privacyGroupRequest.description() == null || privacyGroupRequest.description().isBlank() ? ""
            : privacyGroupRequest.description(),
        PrivacyGroupPayload.State.ACTIVE,
        PrivacyGroupPayload.Type.PANTHEON,
        bytes);
    final String privacyGroupId = privacyGroupStorage.generateDigest(privacyGroupPayload);

    final URI myUri = networkNodes.uriForRecipient(Bytes.fromBase64String(privacyGroupRequest.from()));
    final List<URI> addressListToForward;
    try {
      addressListToForward = Arrays
          .stream(privacyGroupRequest.addresses())
          .map(key -> networkNodes.uriForRecipient(Bytes.fromBase64String(key)))
          .filter(uri -> !uri.equals(myUri))
          .distinct()
          .collect(Collectors.toList());
    } catch (final NullPointerException npe) {
      routingContext.fail(new OrionException(OrionErrorCode.NODE_MISSING_PEER_URL, "couldn't find peer URL "));
      return;
    }

    // propagate payload
    log.debug("propagating payload");

    @SuppressWarnings("rawtypes")
    final CompletableFuture[] cfs = addressListToForward.stream().map(recipientURL -> {

      log.info("Propagating create request with URL {}", recipientURL.toString());

      final CompletableFuture<Boolean> responseFuture = new CompletableFuture<>();

      // serialize payload, stripping non-relevant encryptedKeys, and configureRoutes payload
      final byte[] payload = Serializer.serialize(HttpContentType.CBOR, privacyGroupPayload);

      // execute request
      httpClient
          .post(recipientURL.getPort(), recipientURL.getHost(), "/pushPrivacyGroup")
          .putHeader("Content-Type", "application/cbor")
          .handler(response -> response.bodyHandler(responseBody -> {
            log.info("URL {} responded with {}", recipientURL.toString(), response.statusCode());
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
      log.info("Storing privacy group {}", privacyGroupId);

      privacyGroupStorage.put(privacyGroupPayload).thenAccept((result) -> {

        final QueryPrivacyGroupPayload queryPrivacyGroupPayload =
            new QueryPrivacyGroupPayload(privacyGroupPayload.addresses(), null);

        queryPrivacyGroupPayload.setPrivacyGroupToAppend(privacyGroupId);
        final String key = queryPrivacyGroupStorage.generateDigest(queryPrivacyGroupPayload);

        log.info("Stored privacy group. resulting digest: {}", key);

        queryPrivacyGroupStorage.update(key, queryPrivacyGroupPayload).thenAccept((res) -> {

          final PrivacyGroup group = new PrivacyGroup(
              privacyGroupId,
              PrivacyGroupPayload.Type.PANTHEON,
              privacyGroupRequest.name(),
              privacyGroupRequest.description(),
              privacyGroupRequest.addresses());
          log.info("Storing privacy group {} complete", privacyGroupId);

          final Buffer toReturn = Buffer.buffer(Serializer.serialize(JSON, group));
          routingContext.response().end(toReturn);

        }).exceptionally(
            e -> routingContext.fail(new OrionException(OrionErrorCode.ENCLAVE_UNABLE_STORE_PRIVACY_GROUP, e)));

      }).exceptionally(
          e -> routingContext.fail(new OrionException(OrionErrorCode.ENCLAVE_UNABLE_STORE_PRIVACY_GROUP, e)));
    });
  }

  private void handleFailure(final RoutingContext routingContext, final Throwable ex) {
    log.warn("propagating the payload failed");

    final Throwable cause = ex.getCause();
    if (cause instanceof OrionException) {
      routingContext.fail(cause);
    } else {
      routingContext.fail(new OrionException(OrionErrorCode.NODE_PROPAGATING_TO_ALL_PEERS, ex));
    }
  }
}

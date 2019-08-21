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
import net.consensys.orion.http.handler.set.SetPrivacyGroupRequest;
import net.consensys.orion.network.ConcurrentNetworkNodes;
import net.consensys.orion.network.NodeHttpClientBuilder;
import net.consensys.orion.storage.Storage;
import net.consensys.orion.utils.PrivacyGroupUtils;
import net.consensys.orion.utils.Serializer;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

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
public class AddToPrivacyGroupHandler implements Handler<RoutingContext> {

  private static final Logger log = LogManager.getLogger();

  private final Storage<PrivacyGroupPayload> privacyGroupStorage;
  private final Storage<QueryPrivacyGroupPayload> queryPrivacyGroupStorage;
  private final ConcurrentNetworkNodes networkNodes;
  private final Enclave enclave;
  private final HttpClient httpClient;

  public AddToPrivacyGroupHandler(
      final Storage<PrivacyGroupPayload> privacyGroupStorage,
      final Storage<QueryPrivacyGroupPayload> queryPrivacyGroupStorage,
      final ConcurrentNetworkNodes networkNodes,
      final Enclave enclave,
      final Vertx vertx,
      final Config config) {
    this.privacyGroupStorage = privacyGroupStorage;
    this.queryPrivacyGroupStorage = queryPrivacyGroupStorage;
    this.networkNodes = networkNodes;
    this.enclave = enclave;
    this.httpClient = NodeHttpClientBuilder.build(vertx, config, 1500);
  }


  @Override
  public void handle(final RoutingContext routingContext) {

    final byte[] request = routingContext.getBody().getBytes();
    final ModifyPrivacyGroupRequest modifyPrivacyGroupRequest =
        Serializer.deserialize(JSON, ModifyPrivacyGroupRequest.class, request);

    final AtomicReference<PrivacyGroupPayload> combinedPrivacyGroup = new AtomicReference<>();
    privacyGroupStorage.get(modifyPrivacyGroupRequest.privacyGroupId()).thenAccept((result) -> {
      if (result.isEmpty() || result.get().state() == PrivacyGroupPayload.State.DELETED) {
        routingContext
            .fail(new OrionException(OrionErrorCode.ENCLAVE_UNABLE_ADD_TO_PRIVACY_GROUP, "couldn't add to group"));
      } else {
        final PrivacyGroupPayload oldPrivacyGroupPayload = result.get();

        combinedPrivacyGroup.set(
            new PrivacyGroupPayload(
                Stream
                    .concat(
                        Arrays.stream(oldPrivacyGroupPayload.addresses()),
                        Arrays.stream(modifyPrivacyGroupRequest.addresses()))
                    .distinct()
                    .toArray(String[]::new),
                oldPrivacyGroupPayload.name(),
                oldPrivacyGroupPayload.description(),
                oldPrivacyGroupPayload.state(),
                oldPrivacyGroupPayload.type(),
                oldPrivacyGroupPayload.randomSeed()));
        Stream<Box.PublicKey> combinedAddresses = Arrays
            .stream(combinedPrivacyGroup.get().addresses())
            .filter(key -> !key.equals(modifyPrivacyGroupRequest.from()))
            .distinct()
            .map(enclave::readKey);


        final var addRequests = PrivacyGroupUtils.sendRequestsToOthers(
            combinedAddresses,
            new SetPrivacyGroupRequest(combinedPrivacyGroup.get(), modifyPrivacyGroupRequest.privacyGroupId()),
            "/setPrivacyGroup",
            networkNodes,
            httpClient);



        CompletableFuture.allOf(addRequests.toArray(CompletableFuture[]::new)).whenComplete((iAll, iEx) -> {

          if (iEx != null) {
            PrivacyGroupUtils.handleFailure(routingContext, iEx);
            return;
          }

          CompletableFuture<Boolean> sendStateToNewUserFuture = new CompletableFuture<>();

          /*
           * Todo here: get current private network state from pantheon and send to the new members
           * */


          sendStateToNewUserFuture.whenComplete((res, iiEx) -> {
            if (iiEx != null) {
              PrivacyGroupUtils.handleFailure(routingContext, iiEx);
              return;
            }
            final PrivacyGroupPayload innerCombinedPrivacyGroupPayload = combinedPrivacyGroup.get();
            privacyGroupStorage
                .update(privacyGroupStorage.generateDigest(oldPrivacyGroupPayload), innerCombinedPrivacyGroupPayload)
                .thenAccept((privacyGroupResult) -> {

                  final QueryPrivacyGroupPayload queryPrivacyGroupPayload =
                      new QueryPrivacyGroupPayload(innerCombinedPrivacyGroupPayload.addresses(), null);

                  queryPrivacyGroupPayload.setPrivacyGroupToAppend(modifyPrivacyGroupRequest.privacyGroupId());
                  final String key = queryPrivacyGroupStorage.generateDigest(queryPrivacyGroupPayload);

                  log.info("Stored privacy group. resulting digest: {}", key);
                  queryPrivacyGroupStorage
                      .update(key, queryPrivacyGroupPayload)
                      .thenAccept((queryPrivacyGroupStorageResult) -> {
                        final PrivacyGroup group = new PrivacyGroup(
                            modifyPrivacyGroupRequest.privacyGroupId(),
                            PrivacyGroupPayload.Type.PANTHEON,
                            innerCombinedPrivacyGroupPayload.name(),
                            innerCombinedPrivacyGroupPayload.description(),
                            innerCombinedPrivacyGroupPayload.addresses());
                        log.info("Storing privacy group {} complete", modifyPrivacyGroupRequest.privacyGroupId());

                        final Buffer toReturn = Buffer.buffer(Serializer.serialize(JSON, group));
                        routingContext.response().end(toReturn);
                      })
                      .exceptionally(
                          e -> routingContext
                              .fail(new OrionException(OrionErrorCode.ENCLAVE_UNABLE_STORE_PRIVACY_GROUP, e)));
                })
                .exceptionally(
                    e -> routingContext.fail(new OrionException(OrionErrorCode.ENCLAVE_UNABLE_STORE_PRIVACY_GROUP, e)));
          });

          //hack to get it to run for now?
          sendStateToNewUserFuture.complete(true);
        });
      }
    });
  }

}

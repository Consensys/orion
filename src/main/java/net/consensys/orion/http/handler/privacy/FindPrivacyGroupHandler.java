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

import static net.consensys.cava.io.Base64.encodeBytes;
import static net.consensys.orion.http.server.HttpContentType.JSON;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.orion.enclave.Enclave;
import net.consensys.orion.enclave.PrivacyGroupPayload;
import net.consensys.orion.enclave.QueryPrivacyGroupPayload;
import net.consensys.orion.exception.OrionErrorCode;
import net.consensys.orion.exception.OrionException;
import net.consensys.orion.storage.Storage;
import net.consensys.orion.utils.Serializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

/**
 * Delete the privacy group given the privacyGroupId.
 */
public class FindPrivacyGroupHandler implements Handler<RoutingContext> {

  private final Storage<QueryPrivacyGroupPayload> queryPrivacyGroupStorage;
  private final Storage<PrivacyGroupPayload> privacyGroupStorage;
  private final Enclave enclave;

  public FindPrivacyGroupHandler(
      Storage<QueryPrivacyGroupPayload> queryPrivacyGroupStorage,
      Storage<PrivacyGroupPayload> privacyGroupStorage,
      Enclave enclave) {
    this.queryPrivacyGroupStorage = queryPrivacyGroupStorage;
    this.privacyGroupStorage = privacyGroupStorage;
    this.enclave = enclave;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void handle(RoutingContext routingContext) {

    byte[] request = routingContext.getBody().getBytes();
    FindPrivacyGroupRequest findPrivacyGroupRequest =
        Serializer.deserialize(JSON, FindPrivacyGroupRequest.class, request);

    String[] addresses = findPrivacyGroupRequest.addresses();
    Box.PublicKey[] toKeys = Arrays.stream(addresses).map(enclave::readKey).toArray(Box.PublicKey[]::new);

    QueryPrivacyGroupPayload queryPrivacyGroupPayload =
        new QueryPrivacyGroupPayload(findPrivacyGroupRequest.addresses(), null);
    String key = queryPrivacyGroupStorage.generateDigest(queryPrivacyGroupPayload);
    encodeBytes(enclave.generatePrivacyGroupId(toKeys, new byte[0], PrivacyGroupPayload.Type.LEGACY));


    queryPrivacyGroupStorage.get(key).thenAccept((result) -> {
      if (result.isPresent()) {
        List<String> privacyGroupIds = result.get().privacyGroupId();

        CompletableFuture[] cfs = privacyGroupIds.stream().map(pKey -> {

          CompletableFuture<PrivacyGroup> responseFuture = new CompletableFuture<>();
          privacyGroupStorage.get(pKey).thenAccept((res) -> {
            if (res.isPresent()) {
              PrivacyGroupPayload privacyGroupPayload = res.get();
              if (privacyGroupPayload.state().equals(PrivacyGroupPayload.State.ACTIVE)) {
                PrivacyGroup response = new PrivacyGroup(
                    pKey,
                    privacyGroupPayload.type(),
                    privacyGroupPayload.name(),
                    privacyGroupPayload.description(),
                    privacyGroupPayload.addresses());

                responseFuture.complete(response);
              } else {
                responseFuture.complete(new PrivacyGroup());
              }
            } else {
              responseFuture.completeExceptionally(new OrionException(OrionErrorCode.ENCLAVE_PRIVACY_GROUP_MISSING));
            }
          });
          return responseFuture;
        }).toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(cfs).whenComplete((all, ex) -> {
          if (ex != null) {
            routingContext.fail(new OrionException(OrionErrorCode.ENCLAVE_PRIVACY_GROUP_MISSING));
            return;
          }

          List<PrivacyGroup> listPrivacyGroups = new ArrayList<>();
          for (CompletableFuture c : cfs) {
            try {
              PrivacyGroup privacyGroup = (PrivacyGroup) c.get();
              if (privacyGroup.getPrivacyGroupId() != null) {
                listPrivacyGroups.add(privacyGroup);
              }
            } catch (InterruptedException | ExecutionException e) {
              routingContext.fail(new OrionException(OrionErrorCode.ENCLAVE_PRIVACY_GROUP_MISSING));
            }
          }

          final Buffer responseData = Buffer.buffer(Serializer.serialize(JSON, listPrivacyGroups));
          routingContext.response().end(responseData);

        });

      } else {
        final Buffer responseData = Buffer.buffer(Serializer.serialize(JSON, new ArrayList<>()));
        routingContext.response().end(responseData);
      }
    });
  }

}

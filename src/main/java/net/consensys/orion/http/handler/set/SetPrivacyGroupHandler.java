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
package net.consensys.orion.http.handler.set;

import static net.consensys.orion.http.server.HttpContentType.CBOR;

import net.consensys.orion.enclave.PrivacyGroupPayload;
import net.consensys.orion.enclave.QueryPrivacyGroupPayload;
import net.consensys.orion.exception.OrionErrorCode;
import net.consensys.orion.exception.OrionException;
import net.consensys.orion.storage.Storage;
import net.consensys.orion.utils.Serializer;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Set a privacy group in the enclave, distinct from push as key is specified.
 */
public class SetPrivacyGroupHandler implements Handler<RoutingContext> {

  private static final Logger log = LogManager.getLogger();

  private final Storage<PrivacyGroupPayload> privacyGroupStorage;
  private final Storage<QueryPrivacyGroupPayload> queryPrivacyGroupStorage;

  public SetPrivacyGroupHandler(
      final Storage<PrivacyGroupPayload> privacyGroupStorage,
      final Storage<QueryPrivacyGroupPayload> queryPrivacyGroupStorage) {
    this.privacyGroupStorage = privacyGroupStorage;
    this.queryPrivacyGroupStorage = queryPrivacyGroupStorage;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void handle(final RoutingContext routingContext) {
    final byte[] request = routingContext.getBody().getBytes();
    final SetPrivacyGroupRequest setRequest = Serializer.deserialize(CBOR, SetPrivacyGroupRequest.class, request);
    updatePrivacyGroupStorage(routingContext, setRequest);
  }

  private void updatePrivacyGroupStorage(RoutingContext routingContext, SetPrivacyGroupRequest addRequest) {
    privacyGroupStorage
        .update(addRequest.getPrivacyGroupId(), addRequest.getPayload())
        .thenAccept((privacyGroupResult) -> {

          final QueryPrivacyGroupPayload queryPrivacyGroupPayload =
              new QueryPrivacyGroupPayload(addRequest.getPayload().addresses(), null);

          queryPrivacyGroupPayload.setPrivacyGroupToModify(addRequest.getPrivacyGroupId());
          final String key = queryPrivacyGroupStorage.generateDigest(queryPrivacyGroupPayload);

          log.info("Set privacy group. resulting digest: {}", key);
          updateQueryStorage(routingContext, queryPrivacyGroupPayload, key);
        })
        .exceptionally(
            e -> routingContext.fail(new OrionException(OrionErrorCode.ENCLAVE_UNABLE_STORE_PRIVACY_GROUP, e)));
  }

  private void updateQueryStorage(
      RoutingContext routingContext,
      QueryPrivacyGroupPayload queryPrivacyGroupPayload,
      String key) {
    queryPrivacyGroupStorage.update(key, queryPrivacyGroupPayload).thenAccept((result) -> {
      if (result.isEmpty()) {
        routingContext
            .fail(new OrionException(OrionErrorCode.ENCLAVE_PRIVACY_GROUP_MISSING, "privacy group not found"));
        return;
      }

      var queryResult = result.get();
      log.info(
          "Storing privacy group {} complete with addresses {}",
          queryResult.privacyGroupId(),
          result.get().addresses());

      final Buffer toReturn = Buffer.buffer(Serializer.serialize(CBOR, queryResult));
      routingContext.response().end(toReturn);
    }).exceptionally(
        e -> routingContext.fail(new OrionException(OrionErrorCode.ENCLAVE_UNABLE_STORE_PRIVACY_GROUP, e)));
  }
}

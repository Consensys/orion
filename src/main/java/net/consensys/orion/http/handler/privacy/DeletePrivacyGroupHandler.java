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

import net.consensys.orion.enclave.PrivacyGroupPayload;
import net.consensys.orion.exception.OrionErrorCode;
import net.consensys.orion.exception.OrionException;
import net.consensys.orion.storage.Storage;
import net.consensys.orion.utils.Serializer;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

/**
 * Delete the privacy group given the privacyGroupId.
 */
public class DeletePrivacyGroupHandler implements Handler<RoutingContext> {

  private final Storage<PrivacyGroupPayload> privacyGroupStorage;

  public DeletePrivacyGroupHandler(Storage<PrivacyGroupPayload> privacyGroupStorage) {
    this.privacyGroupStorage = privacyGroupStorage;
  }

  @Override
  public void handle(RoutingContext routingContext) {

    byte[] request = routingContext.getBody().getBytes();
    DeletePrivacyGroupRequest privacyGroup = Serializer.deserialize(JSON, DeletePrivacyGroupRequest.class, request);

    privacyGroupStorage.get(privacyGroup.privacyGroupId()).thenAccept((res) -> {
      if (res.isPresent() && res.get().state() == PrivacyGroupPayload.State.DELETED) {
        routingContext
            .fail(new OrionException(OrionErrorCode.ENCLAVE_UNABLE_DELETE_PRIVACY_GROUP, "group already deleted"));
      } else {
        privacyGroupStorage.delete(privacyGroup.privacyGroupId()).thenAccept((result) -> {
          if (result.isPresent() && result.get().state() == PrivacyGroupPayload.State.DELETED) {
            Buffer toReturn = Buffer.buffer(Serializer.serialize(JSON, result.get().state()));
            routingContext.response().end(toReturn);
          } else {
            routingContext
                .fail(new OrionException(OrionErrorCode.ENCLAVE_UNABLE_DELETE_PRIVACY_GROUP, "couldn't delete group"));
          }
        }).exceptionally(
            e -> routingContext.fail(new OrionException(OrionErrorCode.ENCLAVE_UNABLE_DELETE_PRIVACY_GROUP, e)));
      }
    });
  }
}

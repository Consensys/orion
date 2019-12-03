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
import net.consensys.orion.enclave.PrivacyGroupPayload.State;
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
 * Find the privacy group given the privacyGroupId.
 */
public class GetPrivacyGroupHandler implements Handler<RoutingContext> {

  private static final Logger log = LogManager.getLogger();

  private final Storage<PrivacyGroupPayload> privacyGroupStorage;

  public GetPrivacyGroupHandler(final Storage<PrivacyGroupPayload> privacyGroupStorage) {
    this.privacyGroupStorage = privacyGroupStorage;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void handle(final RoutingContext routingContext) {
    final byte[] request = routingContext.getBody().getBytes();
    final GetPrivacyGroupRequest getPrivacyGroupRequest =
        Serializer.deserialize(JSON, GetPrivacyGroupRequest.class, request);

    final String privacyGroupId = getPrivacyGroupRequest.privacyGroupId();

    if (privacyGroupId == null) {
      routingContext.fail(400);
    } else {

      privacyGroupStorage.get(privacyGroupId).thenAccept((result) -> {
        if (result.isPresent()) {
          final PrivacyGroupPayload privacyGroupPayload = result.get();
          log.info("Found privacy group {}", privacyGroupId);

          if (privacyGroupPayload.state().equals(State.ACTIVE)) {
            final PrivacyGroup response = new PrivacyGroup(
                privacyGroupId,
                privacyGroupPayload.type(),
                privacyGroupPayload.name(),
                privacyGroupPayload.description(),
                privacyGroupPayload.addresses());
            routingContext.response().end(Buffer.buffer(Serializer.serialize(JSON, response)));
          } else {
            routingContext.fail(404, new OrionException(OrionErrorCode.ENCLAVE_PRIVACY_GROUP_MISSING));
          }
        } else {
          routingContext.fail(404, new OrionException(OrionErrorCode.ENCLAVE_PRIVACY_GROUP_MISSING));
        }
      });
    }
  }

}

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
 * Retrieves a privacy group given the privacyGroupId.
 */
public class RetrievePrivacyGroupHandler implements Handler<RoutingContext> {

  private static final Logger log = LogManager.getLogger();

  private final Storage<PrivacyGroupPayload> privacyGroupStorage;

  public RetrievePrivacyGroupHandler(final Storage<PrivacyGroupPayload> privacyGroupStorage) {
    this.privacyGroupStorage = privacyGroupStorage;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void handle(final RoutingContext routingContext) {
    final byte[] request = routingContext.getBody().getBytes();
    final RetrievePrivacyGroupRequest retrievePrivacyGroupRequest =
        Serializer.deserialize(JSON, RetrievePrivacyGroupRequest.class, request);

    final String privacyGroupId = retrievePrivacyGroupRequest.privacyGroupId();

    if (privacyGroupId == null) {
      routingContext.fail(400);
    } else {
      handleRequest(routingContext, privacyGroupId);
    }
  }

  private void handleRequest(final RoutingContext routingContext, final String privacyGroupId) {
    privacyGroupStorage.get(privacyGroupId).thenAccept((result) -> {
      if (result.isPresent()) {
        final PrivacyGroupPayload privacyGroupPayload = result.get();
        log.info("Found privacy group {}", privacyGroupId);
        if (privacyGroupPayload.state().equals(State.ACTIVE)) {
          responseWithPrivacyGroup(routingContext, privacyGroupId, privacyGroupPayload);
        } else {
          responseWithNotFoundError(routingContext);
        }
      } else {
        responseWithNotFoundError(routingContext);
      }
    });
  }

  private void responseWithPrivacyGroup(
      final RoutingContext routingContext,
      final String privacyGroupId,
      final PrivacyGroupPayload privacyGroupPayload) {
    final PrivacyGroup response = new PrivacyGroup(
        privacyGroupId,
        privacyGroupPayload.type(),
        privacyGroupPayload.name(),
        privacyGroupPayload.description(),
        privacyGroupPayload.addresses());
    routingContext.response().end(Buffer.buffer(Serializer.serialize(JSON, response)));
  }

  private void responseWithNotFoundError(final RoutingContext routingContext) {
    routingContext.fail(404, new OrionException(OrionErrorCode.ENCLAVE_PRIVACY_GROUP_MISSING));
  }

}

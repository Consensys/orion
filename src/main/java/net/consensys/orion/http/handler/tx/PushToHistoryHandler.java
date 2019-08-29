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
package net.consensys.orion.http.handler.tx;

import static net.consensys.orion.http.server.HttpContentType.JSON;

import net.consensys.orion.enclave.CommitmentPair;
import net.consensys.orion.enclave.EncryptedPayload;
import net.consensys.orion.enclave.PrivacyGroupPayload;
import net.consensys.orion.exception.OrionErrorCode;
import net.consensys.orion.exception.OrionException;
import net.consensys.orion.storage.Storage;
import net.consensys.orion.utils.Serializer;

import java.util.ArrayList;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler to add a commitment for an enclaveKey to Orion storage
 */
public class PushToHistoryHandler implements Handler<RoutingContext> {

  private final Storage<EncryptedPayload> payloadStorage;
  private final Storage<ArrayList<CommitmentPair>> privateTransactionStorage;
  private final Storage<PrivacyGroupPayload> privacyGroupStorage;


  public PushToHistoryHandler(
      final Storage<EncryptedPayload> payloadStorage,
      final Storage<ArrayList<CommitmentPair>> privateTransactionStorage,
      final Storage<PrivacyGroupPayload> privacyGroupStorage) {
    this.payloadStorage = payloadStorage;
    this.privateTransactionStorage = privateTransactionStorage;
    this.privacyGroupStorage = privacyGroupStorage;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void handle(final RoutingContext routingContext) {
    final byte[] request = routingContext.getBody().getBytes();
    final PushToHistoryRequest addRequest = Serializer.deserialize(JSON, PushToHistoryRequest.class, request);
    privacyGroupStorage.get(addRequest.privacyGroupId()).thenAccept(privacyGroup -> {
      payloadStorage.get(addRequest.enclaveKey()).thenAccept(encryptedPayload -> {
        if (privacyGroup.isEmpty() || encryptedPayload.isEmpty()) {
          routingContext.fail(
              new OrionException(
                  OrionErrorCode.ENCLAVE_UNABLE_ADD_COMMITMENT,
                  "couldn't add transaction to privacy group"));
          return;
        }
        updateTransactionStorageAndReturn(routingContext, addRequest);
      }).exceptionally(e -> routingContext.fail(new OrionException(OrionErrorCode.ENCLAVE_UNABLE_ADD_COMMITMENT, e)));
    }).exceptionally(e -> routingContext.fail(new OrionException(OrionErrorCode.ENCLAVE_UNABLE_ADD_COMMITMENT, e)));
  }

  private void updateTransactionStorageAndReturn(
      final RoutingContext routingContext,
      final PushToHistoryRequest addRequest) {
    privateTransactionStorage.get(addRequest.privacyGroupId()).thenAccept(currentResult -> {
      ArrayList<CommitmentPair> newValue = currentResult.orElseGet(ArrayList::new);
      CommitmentPair pairToAdd = new CommitmentPair(addRequest.enclaveKey(), addRequest.privacyMarkerTxHash());
      if (!newValue.contains(pairToAdd)) {
        newValue.add(pairToAdd);
      }
      privateTransactionStorage.update(addRequest.privacyGroupId(), newValue).thenAccept(newlyAdded -> {
        if (newlyAdded.isEmpty()) {
          routingContext.fail(
              new OrionException(
                  OrionErrorCode.ENCLAVE_UNABLE_ADD_COMMITMENT,
                  "couldn't add transaction to privacy group"));
          return;
        }
        final Buffer toReturn = Buffer.buffer(Serializer.serialize(JSON, true));
        routingContext.response().end(toReturn);
      }).exceptionally(e -> routingContext.fail(new OrionException(OrionErrorCode.ENCLAVE_UNABLE_ADD_COMMITMENT, e)));
    }).exceptionally(e -> routingContext.fail(new OrionException(OrionErrorCode.ENCLAVE_UNABLE_ADD_COMMITMENT, e)));
  }
}

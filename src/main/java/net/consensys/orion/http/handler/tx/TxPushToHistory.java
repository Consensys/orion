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

import net.consensys.orion.enclave.TransactionPair;
import net.consensys.orion.exception.OrionErrorCode;
import net.consensys.orion.exception.OrionException;
import net.consensys.orion.storage.Storage;
import net.consensys.orion.utils.Serializer;

import java.util.ArrayList;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

/**
 * Find the privacy group given the privacyGroupId.
 */
public class TxPushToHistory implements Handler<RoutingContext> {

  private final Storage<ArrayList<TransactionPair>> privateTransactionStorage;

  public TxPushToHistory(final Storage<ArrayList<TransactionPair>> privateTransactionStorage) {
    this.privateTransactionStorage = privateTransactionStorage;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void handle(final RoutingContext routingContext) {
    final byte[] request = routingContext.getBody().getBytes();
    final TxPushToHistoryRequest addRequest = Serializer.deserialize(JSON, TxPushToHistoryRequest.class, request);


    privateTransactionStorage.get(addRequest.privacyGroupId()).thenAccept(currentResult -> {
      var newValue = new ArrayList<TransactionPair>();
      if (currentResult.isPresent()) {
        newValue = currentResult.get();
      }
      newValue.add(new TransactionPair(addRequest.enclaveKey(), addRequest.privacyMarkerTxHash()));
      privateTransactionStorage.update(addRequest.privacyGroupId(), newValue).thenAccept(newlyAdded -> {
        if (newlyAdded.isEmpty()) {
          routingContext.fail(
              new OrionException(
                  OrionErrorCode.ENCLAVE_UNABLE_ADD_PRIVATE_TX,
                  "couldn't add transaction to privacy group"));
          return;
        }
        final Buffer toReturn = Buffer.buffer(Serializer.serialize(JSON, true));
        routingContext.response().end(toReturn);
      });
    });
  }
}

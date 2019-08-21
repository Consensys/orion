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

import static net.consensys.orion.http.server.HttpContentType.CBOR;

import net.consensys.orion.enclave.TransactionPair;
import net.consensys.orion.exception.OrionErrorCode;
import net.consensys.orion.exception.OrionException;
import net.consensys.orion.storage.Storage;
import net.consensys.orion.utils.Serializer;

import java.util.ArrayList;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Find the privacy group given the privacyGroupId.
 */
public class TxPrivacyGroupHandler implements Handler<RoutingContext> {

  private static final Logger log = LogManager.getLogger();

  private final Storage<ArrayList<TransactionPair>> privateTransactionStorage;


  public TxPrivacyGroupHandler(final Storage<ArrayList<TransactionPair>> privateTransactionStorage) {
    this.privateTransactionStorage = privateTransactionStorage;

  }


  @Override
  @SuppressWarnings("rawtypes")
  public void handle(final RoutingContext routingContext) {
    final byte[] request = routingContext.getBody().getBytes();
    final TxPrivacyGroupRequest addRequest = Serializer.deserialize(CBOR, TxPrivacyGroupRequest.class, request);


    privateTransactionStorage.get(addRequest.privacyGroupId()).thenAccept(currentResult -> {
      if (currentResult.isEmpty()) {
        routingContext.fail(
            new OrionException(
                OrionErrorCode.ENCLAVE_UNABLE_ADD_PRIVATE_TX,
                "couldn't add transaction to privacy group"));
        return;
      }

      var newValue = currentResult.get();
      newValue.add(addRequest.payload());

      privateTransactionStorage.update(addRequest.privacyGroupId(), newValue).thenAccept(newlyAdded -> {
        if (newlyAdded.isEmpty()) {
          routingContext.fail(
              new OrionException(
                  OrionErrorCode.ENCLAVE_UNABLE_ADD_PRIVATE_TX,
                  "couldn't add transaction to privacy group"));
          final Buffer toReturn = Buffer.buffer(Serializer.serialize(CBOR, newlyAdded));
          routingContext.response().end(toReturn);
        }

      });


    });



  }

}

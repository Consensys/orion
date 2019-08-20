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

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import net.consensys.orion.enclave.QueryPrivacyGroupPayload;
import net.consensys.orion.enclave.TransactionPair;
import net.consensys.orion.exception.OrionErrorCode;
import net.consensys.orion.exception.OrionException;
import net.consensys.orion.storage.Storage;
import net.consensys.orion.utils.Serializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static net.consensys.orion.http.server.HttpContentType.CBOR;

/**
 * Find the privacy group given the privacyGroupId.
 */
public class TxPrivacyGroupHandler implements Handler<RoutingContext> {

  private static final Logger log = LogManager.getLogger();

  private final Storage<TransactionPair> privateTransactionStorage;


  public TxPrivacyGroupHandler(
          final Storage<TransactionPair> privateTransactionStorage) {
    this.privateTransactionStorage = privateTransactionStorage;

  }


  @Override
  @SuppressWarnings("rawtypes")
  public void handle(final RoutingContext routingContext) {
    final byte[] request = routingContext.getBody().getBytes();
    final TxPrivacyGroupRequest addRequest = Serializer.deserialize(CBOR, TxPrivacyGroupRequest.class, request);


    privateTransactionStorage.update(addRequest.privacyGroupId(), addRequest.payload()).thenAccept(result -> {
        if (result.isEmpty()) {
            routingContext.fail(
                    new OrionException(OrionErrorCode.ENCLAVE_UNABLE_ADD_PRIVATE_TX, "couldn't add transaction to privacy group"));
            return;
        }
        final Buffer toReturn = Buffer.buffer(Serializer.serialize(CBOR, result));
        routingContext.response().end(toReturn);
    });


  }

}

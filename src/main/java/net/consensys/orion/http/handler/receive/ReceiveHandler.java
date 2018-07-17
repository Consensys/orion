/*
 * Copyright 2018 ConsenSys AG.
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
package net.consensys.orion.http.handler.receive;

import static net.consensys.cava.io.Base64.decodeBytes;
import static net.consensys.cava.io.Base64.encodeBytes;
import static net.consensys.orion.http.server.HttpContentType.JSON;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.orion.enclave.Enclave;
import net.consensys.orion.enclave.EnclaveException;
import net.consensys.orion.enclave.EncryptedPayload;
import net.consensys.orion.http.server.HttpContentType;
import net.consensys.orion.storage.Storage;
import net.consensys.orion.utils.Serializer;

import java.util.Collections;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Retrieve a base 64 encoded payload. */
public class ReceiveHandler implements Handler<RoutingContext> {
  private static final Logger log = LogManager.getLogger();
  private final Enclave enclave;
  private final Storage<EncryptedPayload> storage;
  private final HttpContentType contentType;

  public ReceiveHandler(Enclave enclave, Storage<EncryptedPayload> storage, HttpContentType contentType) {
    this.enclave = enclave;
    this.storage = storage;
    this.contentType = contentType;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    log.trace("receive handler called");
    ReceiveRequest receiveRequest;
    String key;
    Box.PublicKey to = null;
    if (contentType == JSON) {
      receiveRequest = Serializer.deserialize(JSON, ReceiveRequest.class, routingContext.getBody().getBytes());
      log.debug("got receive request {}", receiveRequest);
      key = receiveRequest.key;
      if (receiveRequest.to != null) {
        to = Box.PublicKey.fromBytes(decodeBytes(receiveRequest.to));
      }
    } else {
      key = routingContext.request().getHeader("c11n-key");
    }
    if (to == null) {
      to = enclave.nodeKeys()[0];
    }
    Box.PublicKey recipient = to;

    storage.get(key).thenAccept(encryptedPayload -> {
      if (!encryptedPayload.isPresent()) {
        log.info("unable to find payload with key {}", key);
        routingContext.fail(404);
        return;
      }

      byte[] decryptedPayload;
      try {
        decryptedPayload = enclave.decrypt(encryptedPayload.get(), recipient);
      } catch (EnclaveException e) {
        log.info("unable to decrypt payload with key {}", key);
        routingContext.fail(404);
        return;
      }

      // configureRoutes a ReceiveResponse
      Buffer toReturn;
      if (contentType == JSON) {
        toReturn = Buffer
            .buffer(Serializer.serialize(JSON, Collections.singletonMap("payload", encodeBytes(decryptedPayload))));
      } else {
        toReturn = Buffer.buffer(decryptedPayload);
      }

      routingContext.response().end(toReturn);
    });
  }
}

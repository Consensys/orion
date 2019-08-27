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
package net.consensys.orion.http.handler.push;

import net.consensys.orion.enclave.EncryptedPayload;
import net.consensys.orion.http.server.HttpContentType;
import net.consensys.orion.storage.Storage;
import net.consensys.orion.utils.Serializer;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** used to push a payload to a node. */
public class PushHandler implements Handler<RoutingContext> {
  private static final Logger log = LogManager.getLogger();
  private final Storage<EncryptedPayload> storage;

  public PushHandler(Storage<EncryptedPayload> storage) {
    this.storage = storage;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    EncryptedPayload pushRequest =
        Serializer.deserialize(HttpContentType.CBOR, EncryptedPayload.class, routingContext.getBody().getBytes());

    storage.put(pushRequest).thenAccept((digest) -> {
      log.debug("stored payload. resulting digest: {}", digest);
      routingContext.response().end(digest);
    }).exceptionally(routingContext::fail);
  }
}

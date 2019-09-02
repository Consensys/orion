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
package net.consensys.orion.http.handler.push;

import net.consensys.orion.enclave.PrivacyGroupPayload;
import net.consensys.orion.enclave.QueryPrivacyGroupPayload;
import net.consensys.orion.http.server.HttpContentType;
import net.consensys.orion.storage.Storage;
import net.consensys.orion.utils.Serializer;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** used to push a privacy group to a node. */
public class PushPrivacyGroupHandler implements Handler<RoutingContext> {
  private static final Logger log = LogManager.getLogger();
  private final Storage<PrivacyGroupPayload> storage;
  private final Storage<QueryPrivacyGroupPayload> queryPrivacyGroupStorage;

  public PushPrivacyGroupHandler(
      final Storage<PrivacyGroupPayload> storage,
      final Storage<QueryPrivacyGroupPayload> queryPrivacyGroupStorage) {
    this.storage = storage;
    this.queryPrivacyGroupStorage = queryPrivacyGroupStorage;
  }

  @Override
  public void handle(final RoutingContext routingContext) {
    final PrivacyGroupPayload pushRequest =
        Serializer.deserialize(HttpContentType.CBOR, PrivacyGroupPayload.class, routingContext.getBody().getBytes());

    storage.put(pushRequest).thenAccept((digest) -> {
      final QueryPrivacyGroupPayload queryPrivacyGroupPayload =
          new QueryPrivacyGroupPayload(pushRequest.addresses(), null);
      if (pushRequest.state().equals(PrivacyGroupPayload.State.DELETED)) {
        queryPrivacyGroupPayload.setToDelete(true);
      }
      queryPrivacyGroupPayload.setPrivacyGroupToModify(storage.generateDigest(pushRequest));
      final String key = queryPrivacyGroupStorage.generateDigest(queryPrivacyGroupPayload);
      queryPrivacyGroupStorage.update(key, queryPrivacyGroupPayload).thenApply((res) -> {
        log.info("Stored privacy group. resulting digest: {}", digest);
        routingContext.response().end(digest);
        return null;
      }).exceptionally(e -> {
        routingContext.fail(e);
        return null;
      });

    }).exceptionally(routingContext::fail);

  }
}

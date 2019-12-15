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
import net.consensys.orion.enclave.QueryPrivacyGroupPayload;
import net.consensys.orion.exception.OrionErrorCode;
import net.consensys.orion.exception.OrionException;
import net.consensys.orion.storage.Storage;
import net.consensys.orion.utils.Serializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Find the privacy group given the privacyGroupId.
 */
public class FindPrivacyGroupHandler implements Handler<RoutingContext> {

  private static final Logger log = LogManager.getLogger();

  private final Storage<QueryPrivacyGroupPayload> queryPrivacyGroupStorage;
  private final Storage<PrivacyGroupPayload> privacyGroupStorage;

  public FindPrivacyGroupHandler(
      final Storage<QueryPrivacyGroupPayload> queryPrivacyGroupStorage,
      final Storage<PrivacyGroupPayload> privacyGroupStorage) {
    this.queryPrivacyGroupStorage = queryPrivacyGroupStorage;
    this.privacyGroupStorage = privacyGroupStorage;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void handle(final RoutingContext routingContext) {

    final byte[] request = routingContext.getBody().getBytes();
    final FindPrivacyGroupRequest findPrivacyGroupRequest =
        Serializer.deserialize(JSON, FindPrivacyGroupRequest.class, request);

    final String[] addresses = findPrivacyGroupRequest.addresses();
    log.trace("Searching for groups with {}", Arrays.toString(addresses));

    final QueryPrivacyGroupPayload queryPrivacyGroupPayload =
        new QueryPrivacyGroupPayload(findPrivacyGroupRequest.addresses(), null);
    final String key = queryPrivacyGroupStorage.generateDigest(queryPrivacyGroupPayload);
    log.trace("Generated digest of find request {}", key);

    queryPrivacyGroupStorage.get(key).thenAccept((result) -> {
      if (result.isPresent()) {
        final List<String> privacyGroupIds = result.get().privacyGroupId();
        log.trace("Privacy groups ids found {}", Arrays.toString(privacyGroupIds.toArray()));

        final CompletableFuture[] cfs = privacyGroupIds.stream().map(pKey -> {
          log.trace("Retrieving privacy group object for {}", pKey);

          final CompletableFuture<PrivacyGroup> responseFuture = new CompletableFuture<>();
          privacyGroupStorage.get(pKey).thenAccept((res) -> {
            if (res.isPresent()) {
              final PrivacyGroupPayload privacyGroupPayload = res.get();
              if (privacyGroupPayload.state().equals(PrivacyGroupPayload.State.ACTIVE)) {
                final PrivacyGroup response = new PrivacyGroup(
                    pKey,
                    privacyGroupPayload.type(),
                    privacyGroupPayload.name(),
                    privacyGroupPayload.description(),
                    privacyGroupPayload.addresses());

                responseFuture.complete(response);
              } else {
                responseFuture.complete(new PrivacyGroup());
              }
            } else {
              responseFuture.completeExceptionally(new OrionException(OrionErrorCode.ENCLAVE_PRIVACY_GROUP_MISSING));
            }
          });
          return responseFuture;
        }).toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(cfs).whenComplete((all, ex) -> {
          if (ex != null) {
            log.error(ex);
            routingContext.fail(new OrionException(OrionErrorCode.ENCLAVE_PRIVACY_GROUP_MISSING));
            return;
          }

          final List<PrivacyGroup> listPrivacyGroups = new ArrayList<>();
          for (final CompletableFuture c : cfs) {
            try {
              final PrivacyGroup privacyGroup = (PrivacyGroup) c.get();
              if (privacyGroup.getPrivacyGroupId() != null) {
                listPrivacyGroups.add(privacyGroup);
              } else {
                log.debug("Found a privacy group but it does not have a privacy group id");
              }
            } catch (final InterruptedException | ExecutionException e) {
              log.error(e);
              routingContext.fail(new OrionException(OrionErrorCode.ENCLAVE_PRIVACY_GROUP_MISSING));
            }
          }
          log.debug("Found privacy group objects {}", listPrivacyGroups);

          final Buffer responseData = Buffer.buffer(Serializer.serialize(JSON, listPrivacyGroups));
          routingContext.response().end(responseData);

        });

      } else {
        final Buffer responseData = Buffer.buffer(Serializer.serialize(JSON, new ArrayList<>()));
        routingContext.response().end(responseData);
      }
    });
  }

}

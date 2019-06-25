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

import static net.consensys.cava.io.Base64.encodeBytes;
import static net.consensys.orion.http.server.HttpContentType.JSON;

import net.consensys.cava.crypto.sodium.Box;
import net.consensys.orion.enclave.Enclave;
import net.consensys.orion.enclave.PrivacyGroupPayload;
import net.consensys.orion.enclave.QueryPrivacyGroupPayload;
import net.consensys.orion.exception.OrionErrorCode;
import net.consensys.orion.exception.OrionException;
import net.consensys.orion.storage.Storage;
import net.consensys.orion.utils.Serializer;

import java.util.Arrays;
import java.util.Collections;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

/**
 * Delete the privacy group given the privacyGroupId.
 */
public class FindPrivacyGroupHandler implements Handler<RoutingContext> {

  private final Storage<QueryPrivacyGroupPayload> queryPrivacyGroupStorage;
  private final Enclave enclave;

  public FindPrivacyGroupHandler(Storage<QueryPrivacyGroupPayload> queryPrivacyGroupStorage, Enclave enclave) {
    this.queryPrivacyGroupStorage = queryPrivacyGroupStorage;
    this.enclave = enclave;
  }

  @Override
  public void handle(RoutingContext routingContext) {

    byte[] request = routingContext.getBody().getBytes();
    FindPrivacyGroupRequest findPrivacyGroupRequest =
        Serializer.deserialize(JSON, FindPrivacyGroupRequest.class, request);

    String[] addresses = findPrivacyGroupRequest.addresses();
    Box.PublicKey[] toKeys = Arrays.stream(addresses).map(enclave::readKey).toArray(Box.PublicKey[]::new);

    QueryPrivacyGroupPayload queryPrivacyGroupPayload =
        new QueryPrivacyGroupPayload(findPrivacyGroupRequest.addresses(), null);
    String key = queryPrivacyGroupStorage.generateDigest(queryPrivacyGroupPayload);
    encodeBytes(enclave.generatePrivacyGroupId(toKeys, new byte[0], PrivacyGroupPayload.Type.LEGACY));


    queryPrivacyGroupStorage.get(key).thenAccept((result) -> {
      if (result.isPresent()) {
        final Buffer responseData = Buffer.buffer(
            Serializer.serialize(JSON, Collections.singletonMap("privacyGroupIds", result.get().privacyGroupId())));
        routingContext.response().end(responseData);
      } else {
        routingContext.fail(new OrionException(OrionErrorCode.ENCLAVE_PRIVACY_GROUP_MISSING));
      }
    });
  }

}

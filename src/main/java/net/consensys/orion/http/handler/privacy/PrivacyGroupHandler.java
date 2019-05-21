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

import net.consensys.orion.exception.OrionErrorCode;
import net.consensys.orion.exception.OrionException;
import net.consensys.orion.storage.Storage;
import net.consensys.orion.utils.Serializer;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

/**
 * Get the privacyGroup id given the list of addresses.
 */
public class PrivacyGroupHandler implements Handler<RoutingContext> {

  private final Storage<String[]> privacyGroupStorage;

  public PrivacyGroupHandler(Storage<String[]> privacyGroupStorage) {
    this.privacyGroupStorage = privacyGroupStorage;
  }

  @Override
  public void handle(RoutingContext routingContext) {

    byte[] request = routingContext.getBody().getBytes();
    PrivacyGroupRequest addressList = Serializer.deserialize(JSON, PrivacyGroupRequest.class, request);

    final String privacyGroupId = privacyGroupStorage.generateDigest(addressList.addresses());

    privacyGroupStorage.put(addressList.addresses()).thenAccept((result) -> {

      List<PrivacyGroups> groups = new ArrayList<>();

      groups.add(new PrivacyGroups(privacyGroupId, true));
      Buffer toReturn = Buffer.buffer(Serializer.serialize(JSON, groups));
      routingContext.response().end(toReturn);
    }).exceptionally(
        e -> routingContext.fail(new OrionException(OrionErrorCode.ENCLAVE_UNABLE_STORE_PRIVACY_GROUP, e)));
  }
}

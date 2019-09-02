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
package net.consensys.orion.storage;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.consensys.cava.io.Base64.encodeBytes;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.kv.KeyValueStore;
import net.consensys.orion.enclave.Enclave;
import net.consensys.orion.enclave.PrivacyGroupPayload;
import net.consensys.orion.enclave.QueryPrivacyGroupPayload;
import net.consensys.orion.http.server.HttpContentType;
import net.consensys.orion.utils.Serializer;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class QueryPrivacyGroupStorage implements Storage<QueryPrivacyGroupPayload> {

  private final KeyValueStore store;
  private final Enclave enclave;
  public byte[] bytes = new byte[20];

  public QueryPrivacyGroupStorage(KeyValueStore store, Enclave enclave) {
    this.store = store;
    this.enclave = enclave;
    SecureRandom random = new SecureRandom();
    random.nextBytes(bytes);
  }

  @Override
  public AsyncResult<String> put(QueryPrivacyGroupPayload data) {
    String key = generateDigest(data);
    Bytes keyBytes = Bytes.wrap(key.getBytes(UTF_8));
    Bytes dataBytes = Bytes.wrap(Serializer.serialize(HttpContentType.CBOR, data));
    return store.putAsync(keyBytes, dataBytes).thenSupply(() -> key);
  }

  @Override
  public String generateDigest(QueryPrivacyGroupPayload data) {
    Box.PublicKey[] publicKeys = Arrays.stream(data.addresses()).map(enclave::readKey).toArray(Box.PublicKey[]::new);
    return encodeBytes(enclave.generatePrivacyGroupId(publicKeys, bytes, PrivacyGroupPayload.Type.PANTHEON));
  }

  @Override
  public AsyncResult<Optional<QueryPrivacyGroupPayload>> get(String key) {
    Bytes keyBytes = Bytes.wrap(key.getBytes(UTF_8));
    return store.getAsync(keyBytes).thenApply(
        maybeBytes -> Optional.ofNullable(maybeBytes).map(
            bytes -> Serializer
                .deserialize(HttpContentType.CBOR, QueryPrivacyGroupPayload.class, bytes.toArrayUnsafe())));
  }

  @Override
  public AsyncResult<Optional<QueryPrivacyGroupPayload>> update(String key, QueryPrivacyGroupPayload data) {
    return get(key).thenApply((result) -> {
      List<String> listPrivacyGroupIds;
      QueryPrivacyGroupPayload queryPrivacyGroupPayload;
      if (result.isPresent()) {
        queryPrivacyGroupPayload = handleAlreadyPresentUpdate(data, result.get());
      } else {
        listPrivacyGroupIds = Collections.singletonList(data.privacyGroupToModify());
        queryPrivacyGroupPayload = new QueryPrivacyGroupPayload(data.addresses(), listPrivacyGroupIds);
      }

      put(queryPrivacyGroupPayload);
      return Optional.of(queryPrivacyGroupPayload);
    });
  }

  private QueryPrivacyGroupPayload handleAlreadyPresentUpdate(
      final QueryPrivacyGroupPayload data,
      final QueryPrivacyGroupPayload result) {
    List<String> listPrivacyGroupIds;
    QueryPrivacyGroupPayload queryPrivacyGroupPayload;
    if (data.isToDelete()) {
      result.privacyGroupId().remove(data.privacyGroupToModify());
    } else if (!result.privacyGroupId().contains(data.privacyGroupToModify())) {
      result.privacyGroupId().add(data.privacyGroupToModify());
    }
    listPrivacyGroupIds = result.privacyGroupId();
    queryPrivacyGroupPayload = new QueryPrivacyGroupPayload(result.addresses(), listPrivacyGroupIds);
    return queryPrivacyGroupPayload;
  }
}

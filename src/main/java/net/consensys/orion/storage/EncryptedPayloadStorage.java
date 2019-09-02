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
package net.consensys.orion.storage;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.consensys.cava.io.Base64.encodeBytes;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.kv.KeyValueStore;
import net.consensys.orion.enclave.EncryptedPayload;
import net.consensys.orion.exception.OrionErrorCode;
import net.consensys.orion.exception.OrionException;
import net.consensys.orion.http.server.HttpContentType;
import net.consensys.orion.utils.Serializer;

import java.util.Optional;

public class EncryptedPayloadStorage implements Storage<EncryptedPayload> {

  private final KeyValueStore store;
  private final StorageKeyBuilder keyBuilder;

  public EncryptedPayloadStorage(final KeyValueStore store, final StorageKeyBuilder keyBuilder) {
    this.store = store;
    this.keyBuilder = keyBuilder;
  }

  @Override
  public AsyncResult<String> put(final EncryptedPayload data) {
    final String key = generateDigest(data);
    final Bytes keyBytes = Bytes.wrap(key.getBytes(UTF_8));
    final Bytes dataBytes = Bytes.wrap(Serializer.serialize(HttpContentType.CBOR, data));
    return store.putAsync(keyBytes, dataBytes).thenSupply(() -> key);
  }

  @Override
  public String generateDigest(final EncryptedPayload data) {
    return encodeBytes(keyBuilder.build(data.cipherText()));
  }


  @Override
  public AsyncResult<Optional<EncryptedPayload>> get(final String key) {
    final Bytes keyBytes = Bytes.wrap(key.getBytes(UTF_8));
    return store.getAsync(keyBytes).thenApply(
        maybeBytes -> Optional.ofNullable(maybeBytes).map(
            bytes -> Serializer.deserialize(HttpContentType.CBOR, EncryptedPayload.class, bytes.toArrayUnsafe())));
  }

  @Override
  public AsyncResult<Optional<EncryptedPayload>> update(final String key, final EncryptedPayload data) {
    throw new OrionException(OrionErrorCode.METHOD_UNIMPLEMENTED);
  }
}

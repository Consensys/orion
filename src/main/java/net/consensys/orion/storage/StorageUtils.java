/*
 * Copyright 2020 ConsenSys AG.
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

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.crypto.sodium.Box;
import org.apache.tuweni.kv.KeyValueStore;
import org.apache.tuweni.kv.ProxyKeyValueStore;

/** Utility functions used to manipulate key-value store to expose higher order functions. **/
public class StorageUtils {

  private static final Logger log = LogManager.getLogger(StorageUtils.class);

  private StorageUtils() {}

  public static KeyValueStore<Box.PublicKey, URI> convertToPubKeyStore(KeyValueStore<Bytes, Bytes> store) {
    return ProxyKeyValueStore.open(
        store,
        Box.PublicKey::fromBytes,
        Box.PublicKey::bytes,
        StorageUtils::bytesToURI,
        StorageUtils::uriToBytes);
  }

  private static Bytes uriToBytes(Box.PublicKey key, URI uri) {
    return Bytes.wrap(uri.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static URI bytesToURI(Bytes v) {
    try {
      return URI.create(new String(v.toArray(), StandardCharsets.UTF_8));
    } catch (IllegalArgumentException e) {
      log.warn("Error reading URI", e);
    }
    return null;
  }
}

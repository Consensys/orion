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

import java.util.Optional;

import org.apache.tuweni.concurrent.AsyncResult;

public interface Storage<T> {

  /**
   * Stores data in the store.
   *
   * @param data The data to store.
   * @return the base64 encoded key, as an UTF-8 String
   */
  AsyncResult<String> put(T data);

  /**
   * Generates digest for data without storing it.
   *
   * @param data the data to generate a digest for
   * @return the digest of the data
   */
  String generateDigest(T data);

  /**
   * Gets data from the store.
   *
   * @param key should be base64 encoded UTF-8 string
   * @return The retrieved data.
   */
  AsyncResult<Optional<T>> get(String key);

  /**
   * Updates the data in the store.
   *
   * @param key should be base64 encoded UTF-8 string
   * @param data the data to update key with
   * @return The updated data.
   */
  AsyncResult<Optional<T>> update(String key, T data);
}

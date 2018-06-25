package net.consensys.orion.api.storage;

import net.consensys.cava.concurrent.AsyncResult;

import java.util.Optional;

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
}

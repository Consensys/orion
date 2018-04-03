package net.consensys.orion.api.storage;

import java.util.Optional;

public interface Storage<T> {

  /**
   * @param data The data to store.
   * @return the base64 encoded key, as an UTF-8 String
   */
  String put(T data);

  /**
   * @param key should be base64 encoded UTF-8 string
   * @return The retrieved data.
   */
  Optional<T> get(String key);

  /**
   * Remove stored data.
   *
   * @param key The base64 encoded key.
   */
  void remove(String key);
}

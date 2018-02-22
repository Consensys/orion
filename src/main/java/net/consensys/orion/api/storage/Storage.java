package net.consensys.orion.api.storage;

import java.util.Optional;

public interface Storage<T> {

  /**
   * @param data
   * @return the base64 encoded key, as an UTF-8 String
   */
  String put(T data);

  /**
   * @param key should be base64 encoded UTF-8 string
   * @return
   */
  Optional<T> get(String key);

  void remove(String key);
}

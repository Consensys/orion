package net.consensys.orion.api.storage;

import java.util.Optional;

public interface StorageEngine<T> {

  void put(String key, T data);

  Optional<T> get(String key);

  void remove(String key);

  boolean isOpen();

  void close();
}

package net.consensys.athena.api.storage;

import java.util.Optional;

public interface KeyValueStore {

  void store(StorageKey key, StorageData data);

  Optional<StorageData> retrieve(StorageKey key);

  default boolean isOpen() {
    return true;
  }

  default void close() {}
}

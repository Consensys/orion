package net.consensys.athena.api.storage;

import java.util.Optional;

public interface KeyValueStore {

  void put(StorageId key, StorageData data);

  Optional<StorageData> get(StorageId key);

  default boolean isOpen() {
    return true;
  }

  default void close() {}
}

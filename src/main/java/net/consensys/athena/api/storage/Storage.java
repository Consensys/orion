package net.consensys.athena.api.storage;

import java.util.Optional;

public interface Storage {

  StorageKey store(StorageData data);

  Optional<StorageData> retrieve(StorageKey key);

  default boolean isOpen() {
    return true;
  }

  default void close() {}
}

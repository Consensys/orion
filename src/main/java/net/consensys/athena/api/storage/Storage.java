package net.consensys.athena.api.storage;

import java.util.Optional;

public interface Storage {

  StorageId put(StorageData data);

  Optional<StorageData> get(StorageId key);

  default boolean isOpen() {
    return true;
  }

  default void close() {}
}

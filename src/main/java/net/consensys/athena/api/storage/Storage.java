package net.consensys.athena.api.storage;

public interface Storage {

  StorageKey store(StorageData data);

  StorageData retrieve(StorageKey key);

  default boolean isOpen() {
    return true;
  }

  default void close() {}
}
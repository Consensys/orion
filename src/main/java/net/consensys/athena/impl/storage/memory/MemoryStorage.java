package net.consensys.athena.impl.storage.memory;

import net.consensys.athena.api.storage.KeyValueStore;
import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MemoryStorage implements KeyValueStore {
  private final Map<StorageId, StorageData> store = new HashMap<>();

  @Override
  public void put(StorageId key, StorageData data) {
    store.put(key, data);
  }

  @Override
  public Optional<StorageData> get(StorageId key) {
    return Optional.ofNullable(store.get(key));
  }
}

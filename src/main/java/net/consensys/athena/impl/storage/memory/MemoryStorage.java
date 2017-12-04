package net.consensys.athena.impl.storage.memory;

import net.consensys.athena.api.storage.KeyValueStore;
import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MemoryStorage implements KeyValueStore {
  private final Map<StorageKey, StorageData> store = new HashMap<>();

  @Override
  public void store(StorageKey key, StorageData data) {
    store.put(key, data);
  }

  @Override
  public Optional<StorageData> retrieve(StorageKey key) {
    return Optional.ofNullable(store.get(key));
  }
}

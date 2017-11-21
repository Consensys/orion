package net.consensys.athena.impl.storage.memory;

import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageKey;
import net.consensys.athena.api.storage.StorageKeyBuilder;

import java.util.HashMap;
import java.util.Map;

public class MemoryStorage implements Storage {
  private final StorageKeyBuilder storageKeyBuilder;
  private final Map<StorageKey, StorageData> store = new HashMap<>();

  public MemoryStorage(StorageKeyBuilder storageKeyBuilder) {
    this.storageKeyBuilder = storageKeyBuilder;
  }

  @Override
  public StorageKey store(StorageData data) {
    StorageKey key = storageKeyBuilder.build(data);
    store.put(key, data);
    return key;
  }

  @Override
  public StorageData retrieve(StorageKey key) {
    return store.get(key);
  }
}

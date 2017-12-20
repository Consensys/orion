package net.consensys.athena.impl.storage.memory;

import net.consensys.athena.api.storage.StorageEngine;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MemoryStorage<T> implements StorageEngine<T> {
  private final Map<String, T> store = new HashMap<>();

  @Override
  public void put(String key, T data) {
    store.put(key, data);
  }

  @Override
  public Optional<T> get(String key) {
    return Optional.ofNullable(store.get(key));
  }

  @Override
  public void remove(String key) {
    store.remove(key);
  }

  @Override
  public boolean isOpen() {
    return true;
  }

  @Override
  public void close() {}
}

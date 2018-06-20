package net.consensys.orion.impl.storage.memory;

import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.orion.api.storage.StorageEngine;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MemoryStorage<T> implements StorageEngine<T> {
  private final Map<String, T> store = new HashMap<>();

  @Override
  public AsyncCompletion put(String key, T data) {
    store.put(key, data);
    return AsyncCompletion.completed();
  }

  @Override
  public AsyncResult<Optional<T>> get(String key) {
    return AsyncResult.completed(Optional.ofNullable(store.get(key)));
  }

  @Override
  public void close() {}
}

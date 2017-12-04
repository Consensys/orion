package net.consensys.athena.impl.storage;

import net.consensys.athena.api.storage.KeyValueStore;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageKey;
import net.consensys.athena.api.storage.StorageKeyBuilder;

import java.util.Optional;

public class StorageKeyValueStorageDelegate implements Storage {

  KeyValueStore delegate;
  private final StorageKeyBuilder keyBuilder;

  public StorageKeyValueStorageDelegate(KeyValueStore delegate, StorageKeyBuilder keyBuilder) {
    this.delegate = delegate;
    this.keyBuilder = keyBuilder;
  }

  @Override
  public StorageKey store(StorageData data) {
    StorageKey key = keyBuilder.build(data);
    delegate.store(key, data);
    return key;
  }

  @Override
  public Optional<StorageData> retrieve(StorageKey key) {
    return delegate.retrieve(key);
  }

  @Override
  public boolean isOpen() {
    return delegate.isOpen();
  }

  @Override
  public void close() {
    delegate.close();
  }
}

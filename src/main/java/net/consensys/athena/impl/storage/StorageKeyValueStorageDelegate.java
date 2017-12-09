package net.consensys.athena.impl.storage;

import net.consensys.athena.api.storage.KeyValueStore;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageId;
import net.consensys.athena.api.storage.StorageIdBuilder;

import java.util.Optional;

public class StorageKeyValueStorageDelegate implements Storage {

  KeyValueStore delegate;
  private final StorageIdBuilder keyBuilder;

  public StorageKeyValueStorageDelegate(KeyValueStore delegate, StorageIdBuilder keyBuilder) {
    this.delegate = delegate;
    this.keyBuilder = keyBuilder;
  }

  @Override
  public StorageId put(StorageData data) {
    StorageId key = keyBuilder.build(data);
    delegate.put(key, data);
    return key;
  }

  @Override
  public Optional<StorageData> get(StorageId key) {
    return delegate.get(key);
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

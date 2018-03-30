package net.consensys.orion.impl.storage;

import net.consensys.orion.api.enclave.EncryptedPayload;
import net.consensys.orion.api.storage.Storage;
import net.consensys.orion.api.storage.StorageEngine;
import net.consensys.orion.api.storage.StorageKeyBuilder;
import net.consensys.orion.impl.utils.Base64;

import java.util.Optional;

public class EncryptedPayloadStorage implements Storage<EncryptedPayload> {

  private final StorageEngine<EncryptedPayload> storageEngine;
  private final StorageKeyBuilder keyBuilder;

  public EncryptedPayloadStorage(StorageEngine<EncryptedPayload> storageEngine, StorageKeyBuilder keyBuilder) {
    this.storageEngine = storageEngine;
    this.keyBuilder = keyBuilder;
  }

  @Override
  public String put(EncryptedPayload data) {
    // build key
    String key = Base64.encode(keyBuilder.build(data.cipherText()));
    storageEngine.put(key, data);
    return key;
  }

  @Override
  public Optional<EncryptedPayload> get(String key) {
    return storageEngine.get(key);
  }

  @Override
  public void remove(String key) {
    storageEngine.remove(key);
  }
}

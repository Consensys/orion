package net.consensys.orion.impl.storage;

import net.consensys.cava.concurrent.AsyncResult;
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
  public AsyncResult<String> put(EncryptedPayload data) {
    String key = generateDigest(data);
    return storageEngine.put(key, data).thenSupply(() -> key);
  }

  @Override
  public String generateDigest(EncryptedPayload data) {
    return Base64.encode(keyBuilder.build(data.cipherText()));
  }


  @Override
  public AsyncResult<Optional<EncryptedPayload>> get(String key) {
    return storageEngine.get(key);
  }
}

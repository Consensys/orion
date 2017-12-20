package net.consensys.athena.impl.storage;

import net.consensys.athena.api.enclave.EncryptedPayload;
import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageEngine;
import net.consensys.athena.api.storage.StorageKeyBuilder;

import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Optional;

public class EncryptedPayloadStorage implements Storage<EncryptedPayload> {

  private final StorageEngine<EncryptedPayload> storageEngine;
  private final StorageKeyBuilder keyBuilder;

  public EncryptedPayloadStorage(
      StorageEngine<EncryptedPayload> storageEngine, StorageKeyBuilder keyBuilder) {
    this.storageEngine = storageEngine;
    this.keyBuilder = keyBuilder;
  }

  @Override
  public String put(EncryptedPayload data) {
    // build key
    String key =
        new String(
            Base64.getEncoder().encode(keyBuilder.build(data.getCipherText())),
            Charset.forName("UTF-8"));
    storageEngine.put(key, data);
    return key;
  }

  @Override
  public Optional<EncryptedPayload> get(String key) {
    return storageEngine.get(key);
  }
}

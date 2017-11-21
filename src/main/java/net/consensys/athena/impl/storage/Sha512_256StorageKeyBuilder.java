package net.consensys.athena.impl.storage;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.HashAlgorithm;
import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageKey;
import net.consensys.athena.api.storage.StorageKeyBuilder;

public class Sha512_256StorageKeyBuilder implements StorageKeyBuilder {

  private final Enclave enclave;

  public Sha512_256StorageKeyBuilder(Enclave enclave) {
    this.enclave = enclave;
  }

  @Override
  public StorageKey build(StorageData data) {
    byte[] digest = enclave.digest(HashAlgorithm.SHA_512_256, data.getRaw());

    return new SimpleStorage(digest);
  }
}

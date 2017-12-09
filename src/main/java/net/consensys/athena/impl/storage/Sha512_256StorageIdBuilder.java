package net.consensys.athena.impl.storage;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.HashAlgorithm;
import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageId;
import net.consensys.athena.api.storage.StorageIdBuilder;

public class Sha512_256StorageIdBuilder implements StorageIdBuilder {

  private final Enclave enclave;

  public Sha512_256StorageIdBuilder(Enclave enclave) {
    this.enclave = enclave;
  }

  @Override
  public StorageId build(StorageData data) {
    byte[] digest = enclave.digest(HashAlgorithm.SHA_512_256, data.getRaw());

    return new SimpleStorage(digest);
  }
}

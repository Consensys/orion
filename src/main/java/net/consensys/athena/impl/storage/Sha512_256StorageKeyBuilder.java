package net.consensys.athena.impl.storage;

import net.consensys.athena.api.enclave.Enclave;
import net.consensys.athena.api.enclave.HashAlgorithm;
import net.consensys.athena.api.storage.StorageKeyBuilder;

public class Sha512_256StorageKeyBuilder implements StorageKeyBuilder {

  private final Enclave enclave;

  public Sha512_256StorageKeyBuilder(Enclave enclave) {
    this.enclave = enclave;
  }

  @Override
  public byte[] build(byte[] data) {
    return enclave.digest(HashAlgorithm.SHA_512_256, data);
  }
}

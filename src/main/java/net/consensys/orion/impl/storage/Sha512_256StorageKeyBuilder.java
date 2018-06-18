package net.consensys.orion.impl.storage;

import net.consensys.cava.crypto.Hash;
import net.consensys.orion.api.storage.StorageKeyBuilder;

public class Sha512_256StorageKeyBuilder implements StorageKeyBuilder {

  @Override
  public byte[] build(byte[] data) {
    return Hash.sha2_512_256(data);
  }
}

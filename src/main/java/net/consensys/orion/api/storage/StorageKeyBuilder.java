package net.consensys.orion.api.storage;

public interface StorageKeyBuilder {
  byte[] build(byte[] data);
}

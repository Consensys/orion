package net.consensys.athena.api.storage;

public interface StorageKeyBuilder {
  byte[] build(byte[] data);
}

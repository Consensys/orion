package net.consensys.athena.api.storage;

public interface StorageKey {
  String getBase64Encoded();

  byte[] getRaw();
}
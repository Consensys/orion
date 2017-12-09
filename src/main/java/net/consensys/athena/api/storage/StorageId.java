package net.consensys.athena.api.storage;

public interface StorageId {
  String getBase64Encoded();

  byte[] getRaw();
}

package net.consensys.athena.api.storage;

public interface StorageData {
  String getBase64Encoded();

  byte[] getRaw();
}

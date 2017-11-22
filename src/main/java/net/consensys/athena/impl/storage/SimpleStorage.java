package net.consensys.athena.impl.storage;

import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageKey;

import java.util.Base64;

public class SimpleStorage implements StorageKey, StorageData {

  private byte[] key;

  public SimpleStorage(byte[] bytes) {
    this.key = bytes;
  }

  @Override
  public String getBase64Encoded() {
    return Base64.getEncoder().encodeToString(key);
  }

  @Override
  public byte[] getRaw() {
    return key;
  }
}

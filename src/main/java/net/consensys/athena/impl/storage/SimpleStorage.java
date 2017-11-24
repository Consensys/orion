package net.consensys.athena.impl.storage;

import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageKey;

import java.util.Arrays;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SimpleStorage that = (SimpleStorage) o;

    return Arrays.equals(key, that.key);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(key);
  }
}

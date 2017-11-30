package net.consensys.athena.impl.storage.file;

import net.consensys.athena.api.storage.Storage;
import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageKey;
import net.consensys.athena.api.storage.StorageKeyBuilder;
import net.consensys.athena.impl.storage.SimpleStorage;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

public class MapDbStorage implements Storage {

  private final DB db;
  private final HTreeMap<byte[], byte[]> storageData;
  private StorageKeyBuilder keyBuilder;

  public MapDbStorage(String path, StorageKeyBuilder keyBuilder) {
    db = DBMaker.fileDB(path).transactionEnable().make();
    this.keyBuilder = keyBuilder;
    storageData =
        db.hashMap("storageData", Serializer.BYTE_ARRAY, Serializer.BYTE_ARRAY).createOrOpen();
  }

  @Override
  public StorageKey store(StorageData data) {
    StorageKey key = keyBuilder.build(data);
    storageData.put(key.getRaw(), data.getRaw());
    db.commit();
    return key;
  }

  @Override
  public StorageData retrieve(StorageKey key) {
    byte[] rawData = storageData.get(key.getRaw());
    if (rawData == null) {
      return null;
    }
    return new SimpleStorage(rawData);
  }

  @Override
  public boolean isOpen() {
    return !db.isClosed();
  }

  @Override
  public void close() {
    db.close();
  }
}

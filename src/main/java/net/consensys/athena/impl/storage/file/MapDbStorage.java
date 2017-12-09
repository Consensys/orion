package net.consensys.athena.impl.storage.file;

import net.consensys.athena.api.storage.KeyValueStore;
import net.consensys.athena.api.storage.StorageData;
import net.consensys.athena.api.storage.StorageId;
import net.consensys.athena.impl.storage.SimpleStorage;

import java.util.Optional;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

public class MapDbStorage implements KeyValueStore {

  private final DB db;
  private final HTreeMap<byte[], byte[]> storageData;

  public MapDbStorage(String path) {
    db = DBMaker.fileDB(path).transactionEnable().make();
    storageData =
        db.hashMap("storageData", Serializer.BYTE_ARRAY, Serializer.BYTE_ARRAY).createOrOpen();
  }

  @Override
  public void put(StorageId key, StorageData data) {
    storageData.put(key.getRaw(), data.getRaw());
    db.commit();
  }

  @Override
  public Optional<StorageData> get(StorageId key) {
    byte[] rawData = storageData.get(key.getRaw());
    if (rawData == null) {
      return Optional.empty();
    }
    return Optional.of(new SimpleStorage(rawData));
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

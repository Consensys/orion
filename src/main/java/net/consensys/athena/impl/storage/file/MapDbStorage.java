package net.consensys.athena.impl.storage.file;

import net.consensys.athena.api.storage.StorageEngine;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

public class MapDbStorage<T> implements StorageEngine<T> {

  private static final Logger log = LogManager.getLogger();

  private final DB db;
  private final HTreeMap<byte[], T> storageData;

  public MapDbStorage(String path) {
    db = DBMaker.fileDB(path).transactionEnable().make();
    storageData = db.hashMap("storageData", Serializer.BYTE_ARRAY, Serializer.JAVA).createOrOpen();
  }

  @Override
  public void put(String key, T data) {
    // store data
    storageData.put(key.getBytes(StandardCharsets.UTF_8), data);
    db.commit();
  }

  @Override
  public Optional<T> get(String key) {
    return Optional.ofNullable(storageData.get(key.getBytes(StandardCharsets.UTF_8)));
  }

  @Override
  public void remove(String key) {
    if (storageData.remove(key.getBytes(StandardCharsets.UTF_8)) != null) {
      db.commit();
    }
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

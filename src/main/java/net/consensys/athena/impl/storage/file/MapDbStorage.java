package net.consensys.athena.impl.storage.file;

import net.consensys.athena.api.storage.StorageEngine;
import net.consensys.athena.api.storage.StorageException;

import java.io.UnsupportedEncodingException;
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
    try {
      storageData.put(key.getBytes("UTF-8"), data);
      db.commit();
    } catch (UnsupportedEncodingException e) {
      log.error(e.getMessage());
      throw new StorageException(e.getMessage());
    }
  }

  @Override
  public Optional<T> get(String key) {
    try {
      return Optional.ofNullable(storageData.get(key.getBytes("UTF-8")));
    } catch (UnsupportedEncodingException e) {
      log.error(e.getMessage());
      throw new StorageException(e.getMessage());
    }
  }

  @Override
  public void remove(String key) {
    try {
      if (storageData.remove(key.getBytes("UTF-8")) != null) {
        db.commit();
      }
    } catch (UnsupportedEncodingException e) {
      throw new StorageException(e.getMessage());
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

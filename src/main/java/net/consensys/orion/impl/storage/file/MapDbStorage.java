package net.consensys.orion.impl.storage.file;

import static net.consensys.orion.impl.http.server.HttpContentType.CBOR;
import static org.mapdb.Serializer.BYTE_ARRAY;

import net.consensys.orion.api.storage.StorageEngine;
import net.consensys.orion.impl.utils.Serializer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

public class MapDbStorage<T> implements StorageEngine<T> {

  private final Class<? extends T> typeParameterClass;
  private final DB db;
  private final HTreeMap<byte[], byte[]> storageData;

  public MapDbStorage(Class<? extends T> typeParameterClass, Path dbDir) {
    this.typeParameterClass = typeParameterClass;
    Path dbFile = dbDir.resolve("mapdb");
    db = DBMaker.fileDB(dbFile.toFile()).transactionEnable().make();
    storageData = db.hashMap("storageData", BYTE_ARRAY, BYTE_ARRAY).createOrOpen();
  }

  @Override
  public void put(String key, T data) {
    // store data
    storageData.put(key.getBytes(StandardCharsets.UTF_8), Serializer.serialize(CBOR, data));
    db.commit();
  }

  @Override
  public Optional<T> get(String key) {
    byte[] bytes = storageData.get(key.getBytes(StandardCharsets.UTF_8));
    if (bytes == null) {
      return Optional.empty();
    }

    return Optional.of(Serializer.deserialize(CBOR, typeParameterClass, bytes));
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

package net.consensys.orion.impl.storage.leveldb;

import net.consensys.orion.api.exception.OrionErrorCode;
import net.consensys.orion.api.storage.StorageEngine;
import net.consensys.orion.api.storage.StorageException;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Serializer;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

public class LevelDbStorage<T> implements StorageEngine<T> {

  private Optional<DB> db;
  private final Class<? extends T> typeParameterClass;

  public LevelDbStorage(Class<? extends T> typeParameterClass, String path) {
    this.typeParameterClass = typeParameterClass;
    final Options options = new Options();
    options.createIfMissing(true);
    try {
      db = Optional.of(JniDBFactory.factory.open(new File(path), options));
    } catch (final IOException e) {
      throw new StorageException(OrionErrorCode.STORAGE_OPEN, e);
    }
  }

  @Override
  public void put(String key, T data) {
    if (!db.isPresent()) {
      throw new StorageException(OrionErrorCode.STORAGE_CLOSED_WRITE, "Database was already closed");
    }
    db.get().put(key.getBytes(), Serializer.serialize(HttpContentType.CBOR, data));
  }

  @Override
  public Optional<T> get(String key) {
    if (!db.isPresent()) {
      throw new StorageException(OrionErrorCode.STORAGE_CLOSED_READ, "Database was already closed");
    }

    byte[] bytes = db.get().get(key.getBytes());
    if (bytes == null) {
      return Optional.empty();
    }
    return Optional.of(Serializer.deserialize(HttpContentType.CBOR, typeParameterClass, bytes));
  }

  @Override
  public void remove(String key) {
    if (!db.isPresent()) {
      throw new StorageException(OrionErrorCode.STORAGE_CLOSED_DELETE, "Database was already closed");
    }
    db.get().delete(key.getBytes());
  }

  @Override
  public boolean isOpen() {
    return db.isPresent();
  }

  @Override
  public void close() {
    if (!db.isPresent()) {
      return;
    }

    try {
      db.get().close();
    } catch (final IOException e) {
      throw new StorageException(OrionErrorCode.STORAGE_CLOSE, e);
    }

    db = Optional.empty();
  }
}

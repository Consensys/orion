package net.consensys.orion.impl.storage.leveldb;

import static java.nio.charset.StandardCharsets.UTF_8;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.kv.LevelDBKeyValueStore;
import net.consensys.orion.api.storage.StorageEngine;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Serializer;

import java.nio.file.Path;
import java.util.Optional;


public class LevelDbStorage<T> implements StorageEngine<T> {

  private LevelDBKeyValueStore store;
  private final Class<? extends T> typeParameterClass;

  public LevelDbStorage(Class<? extends T> typeParameterClass, Path path) {
    this.typeParameterClass = typeParameterClass;
    this.store = new LevelDBKeyValueStore(path);
  }

  @Override
  public AsyncCompletion put(String key, T data) {
    return store
        .putAsync(Bytes.wrap(key.getBytes(UTF_8)), Bytes.wrap(Serializer.serialize(HttpContentType.CBOR, data)));
  }

  @Override
  public AsyncResult<Optional<T>> get(String key) {
    return store.getAsync(Bytes.wrap(key.getBytes(UTF_8))).thenApply(
        optionalBytes -> optionalBytes
            .map(bytes -> Serializer.deserialize(HttpContentType.CBOR, typeParameterClass, bytes.toArrayUnsafe())));
  }

  @Override
  public void close() {
    store.close();
  }
}

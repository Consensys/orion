package net.consensys.orion.impl.storage.file;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.consensys.orion.impl.http.server.HttpContentType.CBOR;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.kv.MapDBKeyValueStore;
import net.consensys.orion.api.storage.StorageEngine;
import net.consensys.orion.impl.http.server.HttpContentType;
import net.consensys.orion.impl.utils.Serializer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;


public class MapDbStorage<T> implements StorageEngine<T> {

  private final Class<? extends T> typeParameterClass;
  private MapDBKeyValueStore store;

  public MapDbStorage(Class<? extends T> typeParameterClass, Path dbDir) {
    this.typeParameterClass = typeParameterClass;
    store = new MapDBKeyValueStore(dbDir.resolve("mapdb"));
  }

  @Override
  public AsyncCompletion put(String key, T data) {
    return store
        .putAsync(Bytes.wrap(key.getBytes(StandardCharsets.UTF_8)), Bytes.wrap(Serializer.serialize(CBOR, data)));

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

package net.consensys.orion.api.storage;

import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.concurrent.AsyncResult;

import java.io.Closeable;
import java.util.Optional;

public interface StorageEngine<T> extends Closeable {

  AsyncCompletion put(String key, T data);

  AsyncResult<Optional<T>> get(String key);

  @Override
  void close();
}

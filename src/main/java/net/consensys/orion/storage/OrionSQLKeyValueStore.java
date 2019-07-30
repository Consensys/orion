/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.orion.storage;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.kv.KeyValueStore;

import javax.persistence.EntityManager;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.CoroutineDispatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OrionSQLKeyValueStore implements KeyValueStore {
  private final JpaEntityManagerProvider jpaEntityManagerProvider;

  public OrionSQLKeyValueStore(final JpaEntityManagerProvider jpaEntityManagerProvider) {
    this.jpaEntityManagerProvider = jpaEntityManagerProvider;
  }

  @Nullable
  @Override
  public Bytes get(@NotNull final Bytes key, final Continuation<? super Bytes> ignore) {
    final EntityManager entityManager = jpaEntityManagerProvider.createEntityManager();
    final Store store = entityManager.find(Store.class, key.toArrayUnsafe());
    if (store != null) {
      return Bytes.wrap(store.getValue());
    } else {
      return null;
    }
  }

  @NotNull
  @Override
  public AsyncResult<Bytes> getAsync(final CoroutineDispatcher ignore, @NotNull final Bytes key) {
    return AsyncResult.executeBlocking(() -> get(key, null));
  }

  @NotNull
  @Override
  public AsyncResult<Bytes> getAsync(@NotNull final Bytes key) {
    return AsyncResult.executeBlocking(() -> get(key, null));
  }

  @Nullable
  @Override
  public Unit put(@NotNull final Bytes key, @NotNull final Bytes value, final Continuation<? super Unit> ignore) {
    EntityManager entityManager = jpaEntityManagerProvider.createEntityManager();
    entityManager.getTransaction().begin();

    final Store store = new Store();
    store.setKey(key.toArrayUnsafe());
    store.setValue(value.toArrayUnsafe());

    entityManager.merge(store);
    entityManager.flush();
    entityManager.getTransaction().commit();
    return Unit.INSTANCE;
  }

  @NotNull
  @Override
  public AsyncCompletion putAsync(
      final CoroutineDispatcher ignore,
      @NotNull final Bytes key,
      @NotNull final Bytes value) {
    return AsyncCompletion.executeBlocking(() -> put(key, value, null));
  }

  @NotNull
  @Override
  public AsyncCompletion putAsync(@NotNull final Bytes key, @NotNull final Bytes value) {
    return AsyncCompletion.executeBlocking(() -> put(key, value, null));
  }

  @Override
  public void close() {
    jpaEntityManagerProvider.close();
  }
}

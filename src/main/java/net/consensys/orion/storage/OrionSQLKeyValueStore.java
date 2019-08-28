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

import java.util.Base64;
import java.util.function.Function;
import javax.persistence.EntityManager;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.CoroutineDispatcher;

public class OrionSQLKeyValueStore implements KeyValueStore {
  private final JpaEntityManagerProvider jpaEntityManagerProvider;

  public OrionSQLKeyValueStore(final JpaEntityManagerProvider jpaEntityManagerProvider) {
    this.jpaEntityManagerProvider = jpaEntityManagerProvider;
  }

  private <T> T withEntityManager(final Function<EntityManager, T> entityManagerConsumer) {
    final EntityManager entityManager = jpaEntityManagerProvider.createEntityManager();
    try {
      entityManager.getTransaction().begin();
      return entityManagerConsumer.apply(entityManager);
    } finally {
      entityManager.getTransaction().commit();
      entityManager.close();
    }
  }

  @Override
  public Bytes get(final Bytes key, final Continuation<? super Bytes> ignore) {
    return withEntityManager(entityManager -> {
      final String b64String = Base64.getEncoder().encodeToString(key.toArrayUnsafe());
      final Store store = entityManager.find(Store.class, b64String);
      return store != null ? Bytes.wrap(store.getValue()) : null;
    });
  }

  @Override
  public AsyncResult<Bytes> getAsync(final CoroutineDispatcher ignore, final Bytes key) {
    return AsyncResult.executeBlocking(() -> get(key, null));
  }

  @Override
  public AsyncResult<Bytes> getAsync(final Bytes key) {
    return AsyncResult.executeBlocking(() -> get(key, null));
  }

  @Override
  public Unit put(final Bytes key, final Bytes value, final Continuation<? super Unit> ignore) {
    final Store store = new Store();
    final String b64String = Base64.getEncoder().encodeToString(key.toArrayUnsafe());
    store.setKey(b64String);
    store.setValue(value.toArrayUnsafe());
    withEntityManager(entityManager -> entityManager.merge(store));
    return Unit.INSTANCE;
  }

  @Override
  public AsyncCompletion putAsync(final CoroutineDispatcher ignore, final Bytes key, final Bytes value) {
    return AsyncCompletion.executeBlocking(() -> put(key, value, null));
  }

  @Override
  public AsyncCompletion putAsync(final Bytes key, final Bytes value) {
    return AsyncCompletion.executeBlocking(() -> put(key, value, null));
  }

  @Override
  public void close() {
    jpaEntityManagerProvider.close();
  }
}

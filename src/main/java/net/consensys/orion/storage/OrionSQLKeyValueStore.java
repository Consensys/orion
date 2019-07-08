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
import net.consensys.cava.kv.SQLKeyValueStore;
import net.consensys.orion.exception.OrionErrorCode;
import net.consensys.orion.exception.OrionException;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.CoroutineDispatcher;
import kotlinx.coroutines.Dispatchers;

public class OrionSQLKeyValueStore implements KeyValueStore {
  private SQLKeyValueStore delegate = null;

  private final String jdbcUrl;
  private final String tableName;
  private final String keyColumn;
  private final String valueColumn;

  BoneCP connectionPool;
  private final CoroutineDispatcher dispatcher = Dispatchers.getIO();

  public OrionSQLKeyValueStore(String jdbcUrl, String tableName, String keyColumn, String valueColumn)
      throws SQLException, IOException {
    this.jdbcUrl = jdbcUrl;
    this.tableName = tableName;
    this.keyColumn = keyColumn;
    this.valueColumn = valueColumn;
    this.delegate = new SQLKeyValueStore(jdbcUrl, tableName, keyColumn, valueColumn, dispatcher);
    setBoneCPConfig();
  }

  public OrionSQLKeyValueStore(String jdbcUrl) throws SQLException, IOException {
    this(jdbcUrl, "store", "key", "value");
  }

  @Override
  public AsyncResult<Bytes> getAsync(Bytes key) {
    return delegate.getAsync(key);
  }

  @Override
  public AsyncResult<Bytes> getAsync(CoroutineDispatcher dispatcher, Bytes key) {
    return delegate.getAsync(dispatcher, key);
  }

  @Override
  public AsyncCompletion putAsync(Bytes key, Bytes value) {
    return put(key, value, null);
  }

  @Override
  public AsyncCompletion putAsync(CoroutineDispatcher dispatcher, Bytes key, Bytes value) {
    return delegate.putAsync(dispatcher, key, value);
  }

  @Override
  public void close() {
    delegate.close();
  }

  @Override
  public Bytes get(Bytes key, Continuation<? super Bytes> continuation) {
    return (Bytes) delegate.get(key, continuation);
  }

  @Override
  public AsyncCompletion put(Bytes key, Bytes value, Continuation<? super Unit> continuation) {

    CompletableFuture<Boolean> cfs = new CompletableFuture<>();
    PreparedStatement preparedStatement;
    try {
      preparedStatement = connectionPool.getConnection().prepareStatement(
          String.format("MERGE INTO %s(%s,%s) VALUES(?,?)", tableName, keyColumn, valueColumn));
      preparedStatement.setBytes(1, key.toArrayUnsafe());
      preparedStatement.setBytes(2, value.toArrayUnsafe());
      preparedStatement.execute();
      cfs.complete(true);
    } catch (SQLException e) {
      e.printStackTrace();
      cfs.completeExceptionally(e);
    }

    try {
      Boolean successful = cfs.get();
      if (successful) {
        return AsyncCompletion.completed();
      } else {
        return AsyncCompletion.exceptional(new OrionException(OrionErrorCode.ENCLAVE_UNABLE_STORE_PRIVACY_GROUP));
      }
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
      return AsyncCompletion.exceptional(new OrionException(OrionErrorCode.ENCLAVE_UNABLE_STORE_PRIVACY_GROUP));
    }
  }

  private void setBoneCPConfig() throws SQLException {
    BoneCPConfig boneCPConfig = new BoneCPConfig();
    boneCPConfig.setJdbcUrl(jdbcUrl);
    connectionPool = new BoneCP(boneCPConfig);
  }

}

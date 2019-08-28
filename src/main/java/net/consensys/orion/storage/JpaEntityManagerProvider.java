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

import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import com.google.common.collect.ImmutableMap;

public class JpaEntityManagerProvider {
  private static final String JDBC_PREFIX = "jdbc:";
  private final EntityManagerFactory entityManagerFactory;

  private final Map<String, String> jdbcDrivers = ImmutableMap
      .<String, String>builder()
      .put("postgresql", "org.postgresql.Driver")
      .put("h2", "org.h2.Driver")
      .put("oracle", "oracle.jdbc.OracleDriver")
      .build();

  public JpaEntityManagerProvider(final String jdbcUrl) {
    final String databaseDriver = determineDatabaseDriver(jdbcUrl);
    this.entityManagerFactory = createEntityManagerFactory(jdbcUrl, databaseDriver);
  }

  public EntityManager createEntityManager() {
    return entityManagerFactory.createEntityManager();
  }

  private String determineDatabaseDriver(final String jdbcUrl) {
    final String dbName = databaseName(jdbcUrl);
    final String driverName = jdbcDrivers.get(dbName);
    if (driverName == null) {
      throw new IllegalStateException("No database driver found for jdbc url " + jdbcUrl);
    }
    return driverName;
  }

  private String databaseName(final String jdbcUrl) {
    final int jdbcPrefixOffset = JDBC_PREFIX.length();
    final int dbEndSeparator = jdbcUrl.indexOf(":", jdbcPrefixOffset);
    return jdbcUrl.substring(jdbcPrefixOffset, dbEndSeparator);
  }

  private EntityManagerFactory createEntityManagerFactory(final String jdbcUrl, final String driverName) {
    final Map<String, String> properties = new HashMap<>();
    properties.put("openjpa.RuntimeUnenhancedClasses", "supported");
    properties.put("openjpa.ConnectionURL", jdbcUrl);
    properties.put("openjpa.ConnectionDriverName", driverName);
    return Persistence.createEntityManagerFactory("orion", properties);
  }

  public void close() {
    entityManagerFactory.close();
  }
}

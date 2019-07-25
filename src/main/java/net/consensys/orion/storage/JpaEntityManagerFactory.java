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

import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import com.google.common.collect.ImmutableMap;

public class JpaEntityManagerFactory {
  private static final String JDBC_PREFIX = "jdbc:";
  private final Class<?>[] entityClasses;
  private final String jdbcUrl;
  private EntityManager entityManager;

  private final Map<String, String> jdbcDrivers = ImmutableMap
      .<String, String>builder()
      .put("postgresql", "org.postgresql.Driver")
      .put("h2", "org.h2.Driver")
      .build();

  public JpaEntityManagerFactory(final String jdbcUrl, Class<?>... entityClasses) {
    this.entityClasses = entityClasses;
    this.jdbcUrl = jdbcUrl;
  }

  public EntityManager createEntityManager() {
    if (entityManager == null) {
      final String dbName = databaseName();
      final String driverName = jdbcDrivers.get(dbName);
      // TODO throw error if no driver found
      entityManager = createEntityManagerFactory(driverName);
    }
    return entityManager;
  }

  private String databaseName() {
    int jdbcPrefixOffset = JDBC_PREFIX.length();
    int dbEndSeparator = jdbcUrl.indexOf(":", jdbcPrefixOffset);
    return jdbcUrl.substring(jdbcPrefixOffset, dbEndSeparator);
  }

  private EntityManager createEntityManagerFactory(final String driverName) {
    final String types = Arrays.stream(entityClasses).map(Class::getName).collect(joining(";"));
    final Map<String, String> properties = new HashMap<>();
    properties.put("openjpa.RuntimeUnenhancedClasses", "supported");
    properties.put("openjpa.ConnectionURL", jdbcUrl);
    properties.put("openjpa.ConnectionDriverName", driverName);
    properties.put("openjpa.MetaDataFactory", "jpa(Types=" + types + ")");

    final EntityManagerFactory factory = Persistence.createEntityManagerFactory("orion", properties);
    return factory.createEntityManager();
  }


}

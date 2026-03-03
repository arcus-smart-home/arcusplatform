/*
 * Copyright 2019 Arcus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iris.core.dao.cassandra;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchStatementBuilder;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.DefaultBatchType;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.ApiKeyDAO;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.security.apikey.ApiKey;

@Singleton
public class ApiKeyDAOImpl implements ApiKeyDAO {

   private static final String API_KEY_TABLE = "api_key";
   private static final String API_KEY_BY_HASH_TABLE = "api_key_by_hash";
   private static final String API_KEY_BY_ID_TABLE = "api_key_by_id";

   private static final Timer saveTimer = DaoMetrics.upsertTimer(ApiKeyDAO.class, "save");
   private static final Timer findByPlaceTimer = DaoMetrics.readTimer(ApiKeyDAO.class, "findByPlace");
   private static final Timer findByKeyHashTimer = DaoMetrics.readTimer(ApiKeyDAO.class, "findByKeyHash");
   private static final Timer deleteTimer = DaoMetrics.deleteTimer(ApiKeyDAO.class, "delete");
   private static final Timer deleteForPlaceTimer = DaoMetrics.deleteTimer(ApiKeyDAO.class, "deleteForPlace");
   private static final Timer expireTimer = DaoMetrics.upsertTimer(ApiKeyDAO.class, "expire");
   private static final Timer findLabelByIdTimer = DaoMetrics.readTimer(ApiKeyDAO.class, "findLabelById");
   private static final Timer updateLastUsedTimer = DaoMetrics.upsertTimer(ApiKeyDAO.class, "updateLastUsed");

   private static class Cols {
      public static final String PLACE_ID = "placeId";
      public static final String ID = "id";
      public static final String LABEL = "label";
      public static final String KEY_PREFIX = "keyPrefix";
      public static final String KEY_HASH = "keyHash";
      public static final String PERSON_ID = "personId";
      public static final String ACCOUNT_ID = "accountId";
      public static final String PERMISSIONS = "permissions";
      public static final String CREATED = "created";
      public static final String LAST_USED = "lastUsed";
      public static final String EXPIRES_AT = "expiresAt";
   }

   private final CqlSession session;
   private final PreparedStatement insertApiKey;
   private final PreparedStatement insertApiKeyByHash;
   private final PreparedStatement findByPlace;
   private final PreparedStatement findByKeyHash;
   private final PreparedStatement deleteApiKey;
   private final PreparedStatement deleteApiKeyByHash;
   private final PreparedStatement deleteAllForPlace;
   private final PreparedStatement insertApiKeyById;
   private final PreparedStatement findLabelById;
   private final PreparedStatement deleteApiKeyById;
   private final PreparedStatement expireApiKey;
   private final PreparedStatement expireApiKeyByHash;
   private final PreparedStatement updateLastUsedByHash;
   private final PreparedStatement updateLastUsedByPlace;

   @Inject
   public ApiKeyDAOImpl(CqlSession session) {
      this.session = session;
      this.insertApiKey = prepareInsert(API_KEY_TABLE);
      this.insertApiKeyByHash = prepareInsert(API_KEY_BY_HASH_TABLE);
      this.findByPlace = prepareFindByPlace();
      this.findByKeyHash = prepareFindByKeyHash();
      this.deleteApiKey = prepareDeleteApiKey();
      this.deleteApiKeyByHash = prepareDeleteApiKeyByHash();
      this.deleteAllForPlace = prepareDeleteAllForPlace();
      this.insertApiKeyById = session.prepare(
            "INSERT INTO " + API_KEY_BY_ID_TABLE + " (id, placeId, label) VALUES (?, ?, ?)");
      this.findLabelById = session.prepare(
            "SELECT label FROM " + API_KEY_BY_ID_TABLE + " WHERE id = ?");
      this.deleteApiKeyById = session.prepare(
            "DELETE FROM " + API_KEY_BY_ID_TABLE + " WHERE id = ?");
      this.expireApiKey = session.prepare(
            "UPDATE " + API_KEY_TABLE + " SET " + Cols.EXPIRES_AT + " = ? WHERE " + Cols.PLACE_ID + " = ? AND " + Cols.ID + " = ?");
      this.expireApiKeyByHash = session.prepare(
            "UPDATE " + API_KEY_BY_HASH_TABLE + " SET " + Cols.EXPIRES_AT + " = ? WHERE " + Cols.KEY_HASH + " = ?");
      this.updateLastUsedByHash = prepareUpdateLastUsed();
      this.updateLastUsedByPlace = session.prepare(
            "UPDATE " + API_KEY_TABLE + " SET " + Cols.LAST_USED + " = ? WHERE " + Cols.PLACE_ID + " = ? AND " + Cols.ID + " = ?");
   }

   @Override
   public void save(ApiKey key) {
      Preconditions.checkNotNull(key, "key must not be null");
      Preconditions.checkNotNull(key.getId(), "id must not be null");
      Preconditions.checkNotNull(key.getPlaceId(), "placeId must not be null");
      Preconditions.checkNotNull(key.getKeyHash(), "keyHash must not be null");

      BatchStatementBuilder batch = BatchStatement.builder(DefaultBatchType.LOGGED);
      batch.addStatement(bindInsert(insertApiKey, key));
      batch.addStatement(bindInsert(insertApiKeyByHash, key));
      batch.addStatement(insertApiKeyById.bind(key.getId(), key.getPlaceId(), key.getLabel()));

      try (Context ctxt = saveTimer.time()) {
         session.execute(batch.build());
      }
   }

   @Override
   public List<ApiKey> findByPlace(UUID placeId) {
      Preconditions.checkNotNull(placeId, "placeId must not be null");
      BoundStatement stmt = findByPlace.bind(placeId);
      try (Context ctxt = findByPlaceTimer.time()) {
         return session.execute(stmt).all().stream()
               .map(this::buildFromRow)
               .collect(Collectors.toList());
      }
   }

   @Override
   public ApiKey findByKeyHash(String keyHash) {
      Preconditions.checkNotNull(keyHash, "keyHash must not be null");
      BoundStatement stmt = findByKeyHash.bind(keyHash);
      try (Context ctxt = findByKeyHashTimer.time()) {
         Row row = session.execute(stmt).one();
         return row == null ? null : buildFromRow(row);
      }
   }

   @Override
   public String findLabelById(UUID id) {
      Preconditions.checkNotNull(id, "id must not be null");
      BoundStatement stmt = findLabelById.bind(id);
      try (Context ctxt = findLabelByIdTimer.time()) {
         Row row = session.execute(stmt).one();
         return row == null ? null : row.getString(Cols.LABEL);
      }
   }

   @Override
   public void expire(UUID placeId, UUID id, String keyHash, Instant expiresAt) {
      Preconditions.checkNotNull(placeId, "placeId must not be null");
      Preconditions.checkNotNull(id, "id must not be null");
      Preconditions.checkNotNull(keyHash, "keyHash must not be null");
      Preconditions.checkNotNull(expiresAt, "expiresAt must not be null");

      BatchStatementBuilder batch = BatchStatement.builder(DefaultBatchType.LOGGED);
      batch.addStatement(expireApiKey.bind(expiresAt, placeId, id));
      batch.addStatement(expireApiKeyByHash.bind(expiresAt, keyHash));

      try (Context ctxt = expireTimer.time()) {
         session.execute(batch.build());
      }
   }

   @Override
   public void delete(UUID placeId, UUID id, String keyHash) {
      Preconditions.checkNotNull(placeId, "placeId must not be null");
      Preconditions.checkNotNull(id, "id must not be null");
      Preconditions.checkNotNull(keyHash, "keyHash must not be null");

      BatchStatementBuilder batch = BatchStatement.builder(DefaultBatchType.LOGGED);
      batch.addStatement(deleteApiKey.bind(placeId, id));
      batch.addStatement(deleteApiKeyByHash.bind(keyHash));
      batch.addStatement(deleteApiKeyById.bind(id));

      try (Context ctxt = deleteTimer.time()) {
         session.execute(batch.build());
      }
   }

   @Override
   public void deleteForPlace(UUID placeId) {
      Preconditions.checkNotNull(placeId, "placeId must not be null");

      try (Context ctxt = deleteForPlaceTimer.time()) {
         List<ApiKey> keys = findByPlace(placeId);
         if (!keys.isEmpty()) {
            BatchStatementBuilder batch = BatchStatement.builder(DefaultBatchType.LOGGED);
            for (ApiKey key : keys) {
               batch.addStatement(deleteApiKeyByHash.bind(key.getKeyHash()));
               batch.addStatement(deleteApiKeyById.bind(key.getId()));
            }
            batch.addStatement(deleteAllForPlace.bind(placeId));
            session.execute(batch.build());
         }
      }
   }

   @Override
   public void updateLastUsed(UUID placeId, UUID id, String keyHash, Instant lastUsed) {
      Preconditions.checkNotNull(placeId, "placeId must not be null");
      Preconditions.checkNotNull(id, "id must not be null");
      Preconditions.checkNotNull(keyHash, "keyHash must not be null");

      BatchStatementBuilder batch = BatchStatement.builder(DefaultBatchType.LOGGED);
      batch.addStatement(updateLastUsedByHash.bind(lastUsed, keyHash));
      batch.addStatement(updateLastUsedByPlace.bind(lastUsed, placeId, id));

      try (Context ctxt = updateLastUsedTimer.time()) {
         session.execute(batch.build());
      }
   }

   private BoundStatement bindInsert(PreparedStatement ps, ApiKey key) {
      return ps.bind()
            .setUuid(Cols.PLACE_ID, key.getPlaceId())
            .setUuid(Cols.ID, key.getId())
            .setString(Cols.LABEL, key.getLabel())
            .setString(Cols.KEY_PREFIX, key.getKeyPrefix())
            .setString(Cols.KEY_HASH, key.getKeyHash())
            .setUuid(Cols.PERSON_ID, key.getPersonId())
            .setUuid(Cols.ACCOUNT_ID, key.getAccountId())
            .setSet(Cols.PERMISSIONS, key.getPermissions(), String.class)
            .setInstant(Cols.CREATED, key.getCreated())
            .setInstant(Cols.LAST_USED, key.getLastUsed())
            .setInstant(Cols.EXPIRES_AT, key.getExpiresAt());
   }

   private ApiKey buildFromRow(Row row) {
      ApiKey key = new ApiKey();
      key.setPlaceId(row.getUuid(Cols.PLACE_ID));
      key.setId(row.getUuid(Cols.ID));
      key.setLabel(row.getString(Cols.LABEL));
      key.setKeyPrefix(row.getString(Cols.KEY_PREFIX));
      key.setKeyHash(row.getString(Cols.KEY_HASH));
      key.setPersonId(row.getUuid(Cols.PERSON_ID));
      key.setAccountId(row.getUuid(Cols.ACCOUNT_ID));
      key.setPermissions(row.getSet(Cols.PERMISSIONS, String.class));
      key.setCreated(row.getInstant(Cols.CREATED));
      key.setLastUsed(row.getInstant(Cols.LAST_USED));
      key.setExpiresAt(row.getInstant(Cols.EXPIRES_AT));
      return key;
   }

   private PreparedStatement prepareInsert(String table) {
      return CassandraQueryBuilder.insert(table)
            .addColumn(Cols.PLACE_ID)
            .addColumn(Cols.ID)
            .addColumn(Cols.LABEL)
            .addColumn(Cols.KEY_PREFIX)
            .addColumn(Cols.KEY_HASH)
            .addColumn(Cols.PERSON_ID)
            .addColumn(Cols.ACCOUNT_ID)
            .addColumn(Cols.PERMISSIONS)
            .addColumn(Cols.CREATED)
            .addColumn(Cols.LAST_USED)
            .addColumn(Cols.EXPIRES_AT)
            .prepare(session);
   }

   private PreparedStatement prepareFindByPlace() {
      CassandraQueryBuilder qb = CassandraQueryBuilder.select(API_KEY_TABLE)
            .addWhereColumnEquals(Cols.PLACE_ID);
      return addAllColumns(qb).prepare(session);
   }

   private PreparedStatement prepareFindByKeyHash() {
      CassandraQueryBuilder qb = CassandraQueryBuilder.select(API_KEY_BY_HASH_TABLE)
            .addWhereColumnEquals(Cols.KEY_HASH);
      return addAllColumns(qb).prepare(session);
   }

   private CassandraQueryBuilder addAllColumns(CassandraQueryBuilder qb) {
      qb.addColumns(
            Cols.PLACE_ID, Cols.ID, Cols.LABEL, Cols.KEY_PREFIX, Cols.KEY_HASH,
            Cols.PERSON_ID, Cols.ACCOUNT_ID, Cols.PERMISSIONS, Cols.CREATED, Cols.LAST_USED, Cols.EXPIRES_AT
      );
      return qb;
   }

   private PreparedStatement prepareDeleteApiKey() {
      return CassandraQueryBuilder.delete(API_KEY_TABLE)
            .addWhereColumnEquals(Cols.PLACE_ID)
            .addWhereColumnEquals(Cols.ID)
            .prepare(session);
   }

   private PreparedStatement prepareDeleteApiKeyByHash() {
      return CassandraQueryBuilder.delete(API_KEY_BY_HASH_TABLE)
            .addWhereColumnEquals(Cols.KEY_HASH)
            .prepare(session);
   }

   private PreparedStatement prepareDeleteAllForPlace() {
      return CassandraQueryBuilder.delete(API_KEY_TABLE)
            .addWhereColumnEquals(Cols.PLACE_ID)
            .prepare(session);
   }

   private PreparedStatement prepareUpdateLastUsed() {
      return session.prepare(
            "UPDATE " + API_KEY_BY_HASH_TABLE + " SET " + Cols.LAST_USED + " = ? WHERE " + Cols.KEY_HASH + " = ?"
      );
   }
}

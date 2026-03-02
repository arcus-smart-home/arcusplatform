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

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
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

   private final Session session;
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
   public ApiKeyDAOImpl(Session session) {
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

      BatchStatement batch = new BatchStatement();
      batch.add(bindInsert(insertApiKey, key));
      batch.add(bindInsert(insertApiKeyByHash, key));
      batch.add(new BoundStatement(insertApiKeyById).bind(key.getId(), key.getPlaceId(), key.getLabel()));

      try (Context ctxt = saveTimer.time()) {
         session.execute(batch);
      }
   }

   @Override
   public List<ApiKey> findByPlace(UUID placeId) {
      Preconditions.checkNotNull(placeId, "placeId must not be null");
      BoundStatement stmt = new BoundStatement(findByPlace).bind(placeId);
      try (Context ctxt = findByPlaceTimer.time()) {
         return session.execute(stmt).all().stream()
               .map(this::buildFromRow)
               .collect(Collectors.toList());
      }
   }

   @Override
   public ApiKey findByKeyHash(String keyHash) {
      Preconditions.checkNotNull(keyHash, "keyHash must not be null");
      BoundStatement stmt = new BoundStatement(findByKeyHash).bind(keyHash);
      try (Context ctxt = findByKeyHashTimer.time()) {
         Row row = session.execute(stmt).one();
         return row == null ? null : buildFromRow(row);
      }
   }

   @Override
   public String findLabelById(UUID id) {
      Preconditions.checkNotNull(id, "id must not be null");
      BoundStatement stmt = new BoundStatement(findLabelById).bind(id);
      try (Context ctxt = findLabelByIdTimer.time()) {
         Row row = session.execute(stmt).one();
         return row == null ? null : row.getString(Cols.LABEL);
      }
   }

   @Override
   public void expire(UUID placeId, UUID id, String keyHash, Date expiresAt) {
      Preconditions.checkNotNull(placeId, "placeId must not be null");
      Preconditions.checkNotNull(id, "id must not be null");
      Preconditions.checkNotNull(keyHash, "keyHash must not be null");
      Preconditions.checkNotNull(expiresAt, "expiresAt must not be null");

      BatchStatement batch = new BatchStatement();
      batch.add(new BoundStatement(expireApiKey).bind(expiresAt, placeId, id));
      batch.add(new BoundStatement(expireApiKeyByHash).bind(expiresAt, keyHash));

      try (Context ctxt = expireTimer.time()) {
         session.execute(batch);
      }
   }

   @Override
   public void delete(UUID placeId, UUID id, String keyHash) {
      Preconditions.checkNotNull(placeId, "placeId must not be null");
      Preconditions.checkNotNull(id, "id must not be null");
      Preconditions.checkNotNull(keyHash, "keyHash must not be null");

      BatchStatement batch = new BatchStatement();
      batch.add(new BoundStatement(deleteApiKey).bind(placeId, id));
      batch.add(new BoundStatement(deleteApiKeyByHash).bind(keyHash));
      batch.add(new BoundStatement(deleteApiKeyById).bind(id));

      try (Context ctxt = deleteTimer.time()) {
         session.execute(batch);
      }
   }

   @Override
   public void deleteForPlace(UUID placeId) {
      Preconditions.checkNotNull(placeId, "placeId must not be null");

      try (Context ctxt = deleteForPlaceTimer.time()) {
         List<ApiKey> keys = findByPlace(placeId);
         if (!keys.isEmpty()) {
            BatchStatement batch = new BatchStatement();
            for (ApiKey key : keys) {
               batch.add(new BoundStatement(deleteApiKeyByHash).bind(key.getKeyHash()));
               batch.add(new BoundStatement(deleteApiKeyById).bind(key.getId()));
            }
            batch.add(new BoundStatement(deleteAllForPlace).bind(placeId));
            session.execute(batch);
         }
      }
   }

   @Override
   public void updateLastUsed(UUID placeId, UUID id, String keyHash, Date lastUsed) {
      Preconditions.checkNotNull(placeId, "placeId must not be null");
      Preconditions.checkNotNull(id, "id must not be null");
      Preconditions.checkNotNull(keyHash, "keyHash must not be null");

      BatchStatement batch = new BatchStatement();
      batch.add(new BoundStatement(updateLastUsedByHash).bind(lastUsed, keyHash));
      batch.add(new BoundStatement(updateLastUsedByPlace).bind(lastUsed, placeId, id));

      try (Context ctxt = updateLastUsedTimer.time()) {
         session.execute(batch);
      }
   }

   private BoundStatement bindInsert(PreparedStatement ps, ApiKey key) {
      return new BoundStatement(ps)
            .setUUID(Cols.PLACE_ID, key.getPlaceId())
            .setUUID(Cols.ID, key.getId())
            .setString(Cols.LABEL, key.getLabel())
            .setString(Cols.KEY_PREFIX, key.getKeyPrefix())
            .setString(Cols.KEY_HASH, key.getKeyHash())
            .setUUID(Cols.PERSON_ID, key.getPersonId())
            .setUUID(Cols.ACCOUNT_ID, key.getAccountId())
            .setSet(Cols.PERMISSIONS, key.getPermissions())
            .setTimestamp(Cols.CREATED, key.getCreated())
            .setTimestamp(Cols.LAST_USED, key.getLastUsed())
            .setTimestamp(Cols.EXPIRES_AT, key.getExpiresAt());
   }

   private ApiKey buildFromRow(Row row) {
      ApiKey key = new ApiKey();
      key.setPlaceId(row.getUUID(Cols.PLACE_ID));
      key.setId(row.getUUID(Cols.ID));
      key.setLabel(row.getString(Cols.LABEL));
      key.setKeyPrefix(row.getString(Cols.KEY_PREFIX));
      key.setKeyHash(row.getString(Cols.KEY_HASH));
      key.setPersonId(row.getUUID(Cols.PERSON_ID));
      key.setAccountId(row.getUUID(Cols.ACCOUNT_ID));
      key.setPermissions(row.getSet(Cols.PERMISSIONS, String.class));
      key.setCreated(row.getTimestamp(Cols.CREATED));
      key.setLastUsed(row.getTimestamp(Cols.LAST_USED));
      key.setExpiresAt(row.getTimestamp(Cols.EXPIRES_AT));
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

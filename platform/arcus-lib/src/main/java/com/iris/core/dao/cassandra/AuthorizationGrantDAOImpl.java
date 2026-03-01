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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DefaultConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchStatementBuilder;
import com.datastax.oss.driver.api.core.cql.BatchableStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.DefaultBatchType;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.AuthorizationGrantDAO;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.security.authz.AuthorizationGrant;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.*;
import com.datastax.oss.driver.api.querybuilder.relation.Relation;

@Singleton
public class AuthorizationGrantDAOImpl implements AuthorizationGrantDAO {

	private static final String AUTHORIZATION_GRANT_TABLE = "authorization_grant";
	private static final String AUTHORIZATION_GRANT_BY_PLACE_TABLE = "authorization_grant_by_place";

	private static final Timer upsertTimer = DaoMetrics.upsertTimer(AuthorizationGrantDAO.class, "save");
   private static final Timer findForEntityTimer = DaoMetrics.readTimer(AuthorizationGrantDAO.class, "findForEntity");
   private static final Timer findForPlaceTimer = DaoMetrics.readTimer(AuthorizationGrantDAO.class, "findForPlace");
   private static final Timer removeGrantTimer = DaoMetrics.deleteTimer(AuthorizationGrantDAO.class, "removeGrant");
   private static final Timer removeGrantsForEntityTimer = DaoMetrics.deleteTimer(AuthorizationGrantDAO.class, "removeGrantsForEntity");
   private static final Timer removeForPlaceTimer = DaoMetrics.deleteTimer(AuthorizationGrantDAO.class, "removeForPlace");

	private static class Cols {
		public static final String ENTITY_ID = "entityId";
		public static final String PLACE_ID  = "placeId";
		public static final String ACCOUNT_ID = "accountId";
		public static final String ACCOUNT_OWNER = "accountOwner";
		public static final String PERMISSIONS = "permissions";
		public static final String PLACE_NAME = "placeName";
	}

   private final CqlSession session;
   private final PreparedStatement upsert;
   private final PreparedStatement upsertByPlace;
   private final PreparedStatement findForEntity;
   private final PreparedStatement findForPlace;
   private final PreparedStatement removeGrant;
   private final PreparedStatement removeGrantFromPlace;
   private final PreparedStatement removeForPlace;
   private final PreparedStatement removeForEntity;

   @Inject
   public AuthorizationGrantDAOImpl(CqlSession session) {
      this.session = session;
      upsert = prepareUpsert();
      upsertByPlace = prepareUpsertByPlace();
      findForEntity = prepareFindForEntity();
      findForPlace = prepareFindForPlace();
      removeGrant = prepareRemoveGrant();
      removeGrantFromPlace = prepareRemoveGrantFromPlace();
      removeForPlace = prepareRemoveGrantForPlace();
      removeForEntity = prepareRemoveGrantForEntity();
   }

   @Override
   public void save(AuthorizationGrant grant) {
      Preconditions.checkNotNull(grant, "grant must not be null");
      Preconditions.checkNotNull(grant.getEntityId(), "entity id must not be null");
      Preconditions.checkNotNull(grant.getAccountId(), "account id must not be null");
      Preconditions.checkNotNull(grant.getPlaceId(), "place id must not be null");

      // uses upsert semantics where an insert statement will update the existing row if it already exists
      BatchStatementBuilder batch = BatchStatement.builder(DefaultBatchType.LOGGED);
      batch.addStatement(bindUpsert(upsert, grant));
      batch.addStatement(bindUpsert(upsertByPlace, grant));

      try(Context ctxt = upsertTimer.time()) {
    	  this.session.execute(batch.build());
      }
   }

   private BoundStatement bindUpsert(PreparedStatement upsert, AuthorizationGrant grant) {
      BoundStatement boundStatement = upsert.bind()
         .setUuid(Cols.ENTITY_ID, grant.getEntityId())
         .setUuid(Cols.PLACE_ID, grant.getPlaceId())
         .setUuid(Cols.ACCOUNT_ID, grant.getAccountId())
         .setBoolean(Cols.ACCOUNT_OWNER, grant.isAccountOwner())
         .setSet(Cols.PERMISSIONS, grant.getPermissions(), String.class)
         .setString(Cols.PLACE_NAME, grant.getPlaceName());
      return boundStatement;
   }

   @Override
   public List<AuthorizationGrant> findForEntity(UUID entityId) {
      Preconditions.checkNotNull(entityId, "entity id must not be null");

      BoundStatement boundStatement = findForEntity.bind(entityId);
      List<Row> rows;

      try(Context ctxt = findForEntityTimer.time()) {
    	  rows = session.execute(boundStatement).all();
      }

      return rows.stream().map((r) -> { return buildFromRow(r); }).collect(Collectors.toList());
   }

   @Override
   public List<AuthorizationGrant> findForPlace(UUID placeId) {
      Preconditions.checkNotNull(placeId, "place id must not be null");

      BoundStatement boundStatement = findForPlace.bind(placeId);
      try(Context ctxt = findForPlaceTimer.time()) {
         List<Row> rows = session.execute(boundStatement).all();
         return rows.stream().map((r) -> { return buildFromRow(r); }).collect(Collectors.toList());
      }
   }

   @Override
   public void removeGrant(UUID entityId, UUID placeId) {
      Preconditions.checkNotNull(entityId, "entity id must not be null");
      Preconditions.checkNotNull(placeId, "place id must not be null");

      BatchStatementBuilder batch = BatchStatement.builder(DefaultBatchType.LOGGED);
      batch.addStatement((BatchableStatement<?>) removeGrant.bind(entityId, placeId));
      batch.addStatement((BatchableStatement<?>) removeGrantFromPlace.bind(entityId, placeId));

      try(Context ctxt = removeGrantTimer.time()) {
    	  session.execute(batch.build());
      }
   }

   @Override
   public void removeGrantsForEntity(UUID entityId) {
      try(Context ctxt = removeGrantsForEntityTimer.time()) {
         List<AuthorizationGrant> grants = findForEntity(entityId);
         Statement<?> statement = deleteFrom("authorization_grant_by_place")
               .whereColumn("placeId").in(grants.stream().map(g -> literal(g.getPlaceId())).collect(Collectors.toList()))
               .whereColumn("entityId").isEqualTo(literal(entityId))
               .build();
         BatchStatementBuilder batch = BatchStatement.builder(DefaultBatchType.LOGGED);
         batch.addStatement((BatchableStatement<?>) statement);
         batch.addStatement((BatchableStatement<?>) removeForEntity.bind(entityId));
         session.execute(batch.build());
      }
   }

   @Override
   public void removeForPlace(UUID placeId) {
      try(Context ctxt = removeForPlaceTimer.time()) {
         List<AuthorizationGrant> grants = findForPlace(placeId);
         Statement<?> statement = deleteFrom(AUTHORIZATION_GRANT_TABLE)
               .whereColumn(Cols.ENTITY_ID).in(grants.stream().map(g -> literal(g.getEntityId())).collect(Collectors.toList()))
               .whereColumn(Cols.PLACE_ID).isEqualTo(literal(placeId))
               .build()
               .setConsistencyLevel(DefaultConsistencyLevel.LOCAL_QUORUM);
         BatchStatementBuilder batch = BatchStatement.builder(DefaultBatchType.LOGGED);
         batch.addStatement((BatchableStatement<?>) statement);
         batch.addStatement((BatchableStatement<?>) removeForPlace.bind(placeId));
         session.execute(batch.build());
      }
   }

   private PreparedStatement prepareUpsert() {
   	return CassandraQueryBuilder.insert(AUTHORIZATION_GRANT_TABLE)
   					.addColumn(Cols.ENTITY_ID)
   					.addColumn(Cols.PLACE_ID)
   					.addColumn(Cols.ACCOUNT_ID)
   					.addColumn(Cols.ACCOUNT_OWNER)
   					.addColumn(Cols.PERMISSIONS)
   					.addColumn(Cols.PLACE_NAME)
   					.prepare(session);
   }

   private PreparedStatement prepareUpsertByPlace() {
   	return CassandraQueryBuilder.insert(AUTHORIZATION_GRANT_BY_PLACE_TABLE)
   					.addColumn(Cols.PLACE_ID)
   					.addColumn(Cols.ENTITY_ID)
   					.addColumn(Cols.ACCOUNT_ID)
   					.addColumn(Cols.ACCOUNT_OWNER)
   					.addColumn(Cols.PERMISSIONS)
   					.addColumn(Cols.PLACE_NAME)
   					.prepare(session);
   }

   private PreparedStatement prepareFindForEntity() {
   	CassandraQueryBuilder queryBuilder = CassandraQueryBuilder.select(AUTHORIZATION_GRANT_TABLE)
   					.addWhereColumnEquals(Cols.ENTITY_ID);
   	return addAllColumns(queryBuilder).prepare(session);
   }

   private PreparedStatement prepareFindForPlace() {
   	CassandraQueryBuilder queryBuilder = CassandraQueryBuilder.select(AUTHORIZATION_GRANT_BY_PLACE_TABLE)
   					.addWhereColumnEquals(Cols.PLACE_ID);
   	return addAllColumns(queryBuilder).prepare(session);
   }

   private CassandraQueryBuilder addAllColumns(CassandraQueryBuilder queryBuilder) {
      queryBuilder.addColumns(Cols.PLACE_ID, Cols.ENTITY_ID, Cols.ACCOUNT_ID, Cols.ACCOUNT_OWNER, Cols.PERMISSIONS, Cols.PLACE_NAME);
      return queryBuilder;
   }

   private PreparedStatement prepareRemoveGrant() {
   	return CassandraQueryBuilder.delete(AUTHORIZATION_GRANT_TABLE)
   					.addWhereColumnEquals(Cols.ENTITY_ID)
   					.addWhereColumnEquals(Cols.PLACE_ID)
   					.prepare(session);
   }

   private PreparedStatement prepareRemoveGrantFromPlace() {
      return CassandraQueryBuilder.delete(AUTHORIZATION_GRANT_BY_PLACE_TABLE)
            .addWhereColumnEquals(Cols.ENTITY_ID)
            .addWhereColumnEquals(Cols.PLACE_ID)
            .prepare(session);
   }

   private PreparedStatement prepareRemoveGrantForPlace() {
   	return CassandraQueryBuilder.delete(AUTHORIZATION_GRANT_BY_PLACE_TABLE)
   					.addWhereColumnEquals(Cols.PLACE_ID)
   					.prepare(session);
   }

   private PreparedStatement prepareRemoveGrantForEntity() {
   	return CassandraQueryBuilder.delete(AUTHORIZATION_GRANT_TABLE)
   					.addWhereColumnEquals(Cols.ENTITY_ID)
   					.prepare(session);
   }

   private AuthorizationGrant buildFromRow(Row row) {
      AuthorizationGrant grant = new AuthorizationGrant();
      grant.setEntityId(row.getUuid(Cols.ENTITY_ID));
      grant.setPlaceId(row.getUuid(Cols.PLACE_ID));
      grant.setAccountId(row.getUuid(Cols.ACCOUNT_ID));
      grant.setAccountOwner(row.getBoolean(Cols.ACCOUNT_OWNER));
      grant.setPlaceName(row.getString(Cols.PLACE_NAME));
      grant.addPermissions(row.getSet(Cols.PERMISSIONS, String.class));
      return grant;
   }
}

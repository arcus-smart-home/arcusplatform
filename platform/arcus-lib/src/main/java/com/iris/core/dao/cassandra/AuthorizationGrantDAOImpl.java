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
import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.AuthorizationGrantDAO;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.security.authz.AuthorizationGrant;

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

   private final Session session;
   private final PreparedStatement upsert;
   private final PreparedStatement upsertByPlace;
   private final PreparedStatement findForEntity;
   private final PreparedStatement findForPlace;
   private final PreparedStatement removeGrant;
   private final PreparedStatement removeGrantFromPlace;
   private final PreparedStatement removeForPlace;
   private final PreparedStatement removeForEntity;

   @Inject
   public AuthorizationGrantDAOImpl(Session session) {
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
      BatchStatement batch = new BatchStatement();
      batch.add(bindUpsert(upsert, grant));
      batch.add(bindUpsert(upsertByPlace, grant));

      try(Context ctxt = upsertTimer.time()) {
    	  this.session.execute(batch);
      }
   }

   private BoundStatement bindUpsert(PreparedStatement upsert, AuthorizationGrant grant) {
      BoundStatement boundStatement = new BoundStatement(upsert)
         .setUUID(Cols.ENTITY_ID, grant.getEntityId())
         .setUUID(Cols.PLACE_ID, grant.getPlaceId())
         .setUUID(Cols.ACCOUNT_ID, grant.getAccountId())
         .setBool(Cols.ACCOUNT_OWNER, grant.isAccountOwner())
         .setSet(Cols.PERMISSIONS, grant.getPermissions())
         .setString(Cols.PLACE_NAME, grant.getPlaceName());
      return boundStatement;
   }

   @Override
   public List<AuthorizationGrant> findForEntity(UUID entityId) {
      Preconditions.checkNotNull(entityId, "entity id must not be null");

      BoundStatement boundStatement = new BoundStatement(findForEntity);
      List<Row> rows;

      try(Context ctxt = findForEntityTimer.time()) {
    	  rows = session.execute(boundStatement.bind(entityId)).all();
      }

      return rows.stream().map((r) -> { return buildFromRow(r); }).collect(Collectors.toList());
   }

   @Override
   public List<AuthorizationGrant> findForPlace(UUID placeId) {
      Preconditions.checkNotNull(placeId, "place id must not be null");

      BoundStatement boundStatement = new BoundStatement(findForPlace);
      try(Context ctxt = findForPlaceTimer.time()) {
         List<Row> rows = session.execute(boundStatement.bind(placeId)).all();
         return rows.stream().map((r) -> { return buildFromRow(r); }).collect(Collectors.toList());
      }
   }

   @Override
   public void removeGrant(UUID entityId, UUID placeId) {
      Preconditions.checkNotNull(entityId, "entity id must not be null");
      Preconditions.checkNotNull(placeId, "place id must not be null");

      BatchStatement stmt = new BatchStatement();
      stmt.add(new BoundStatement(removeGrant).bind(entityId, placeId));
      stmt.add(new BoundStatement(removeGrantFromPlace).bind(entityId, placeId));

      try(Context ctxt = removeGrantTimer.time()) {
    	  session.execute(stmt);
      }
   }

   @Override
   public void removeGrantsForEntity(UUID entityId) {
      try(Context ctxt = removeGrantsForEntityTimer.time()) {
         List<AuthorizationGrant> grants = findForEntity(entityId);
         Statement statement = QueryBuilder.delete().from("authorization_grant_by_place")
               .where(QueryBuilder.in("placeId", grants.stream().map(AuthorizationGrant::getPlaceId).collect(Collectors.toList())))
               .and(QueryBuilder.eq("entityId", entityId));
         BatchStatement batch = new BatchStatement();
         batch.add(statement);
         batch.add(new BoundStatement(removeForEntity).bind(entityId));
         session.execute(batch);
      }
   }

   @Override
   public void removeForPlace(UUID placeId) {
      try(Context ctxt = removeForPlaceTimer.time()) {
         List<AuthorizationGrant> grants = findForPlace(placeId);
         Statement statement = QueryBuilder.delete().from(AUTHORIZATION_GRANT_TABLE)
               .where(QueryBuilder.in(Cols.ENTITY_ID, grants.stream().map(AuthorizationGrant::getEntityId).collect(Collectors.toList())))
               .and(QueryBuilder.eq(Cols.PLACE_ID, placeId));
         statement.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
         BatchStatement batch = new BatchStatement();
         batch.add(statement);
         batch.add(new BoundStatement(removeForPlace).bind(placeId));
         session.execute(batch);
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
      grant.setEntityId(row.getUUID(Cols.ENTITY_ID));
      grant.setPlaceId(row.getUUID(Cols.PLACE_ID));
      grant.setAccountId(row.getUUID(Cols.ACCOUNT_ID));
      grant.setAccountOwner(row.getBool(Cols.ACCOUNT_OWNER));
      grant.setPlaceName(row.getString(Cols.PLACE_NAME));
      grant.addPermissions(row.getSet(Cols.PERMISSIONS, String.class));
      return grant;
   }
}


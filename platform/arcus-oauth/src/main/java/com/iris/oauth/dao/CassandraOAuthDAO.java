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
package com.iris.oauth.dao;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;
import com.iris.core.dao.exception.DaoException;
import com.iris.oauth.OAuthConfig;

@Singleton
public class CassandraOAuthDAO implements OAuthDAO {

   private static final Logger logger = LoggerFactory.getLogger(CassandraOAuthDAO.class);

   private static final String OAUTH_TABLE = "oauth";
   private static final String PERSON_OAUTH_TABLE = "person_oauth";
   private static final String TTL_COLUMN_NAME = "ttlremaining";
   private static final String TTL_COLUMN = "ttl(person) AS " + TTL_COLUMN_NAME;

   private enum OAuthCols { appid, tok_0_2, tok, type, person };
   private enum PersonOAuthCols { person, appid, access, refresh, attrs };
   private enum Type { CODE, ACCESS, REFRESH };

   private final CqlSession session;
   private final PreparedStatement insertCode;
   private final PreparedStatement insertAccess;
   private final PreparedStatement insertRefresh;
   private final PreparedStatement upsertPerson;
   private final PreparedStatement removeToken;
   private final PreparedStatement updatePersonTokens;
   private final PreparedStatement updatePersonAccess;
   private final PreparedStatement updatePersonAttrs;
   private final PreparedStatement getTokens;
   private final PreparedStatement removePerson;
   private final PreparedStatement personWith;
   private final PreparedStatement getAttrs;

   @Inject
   public CassandraOAuthDAO(CqlSession session, OAuthConfig config) {
      this.session = session;
      insertCode = prepareTokenInsert(TimeUnit.SECONDS.convert(config.getCodeTtlMinutes(), TimeUnit.MINUTES));
      insertAccess = prepareTokenInsert(TimeUnit.SECONDS.convert(config.getAccessTtlMinutes(), TimeUnit.MINUTES));
      insertRefresh = prepareTokenInsert(TimeUnit.SECONDS.convert(config.getRefreshTtlDays(), TimeUnit.DAYS));
      upsertPerson = prepareUpsertPerson();
      removeToken = prepareTokenWhere(CassandraQueryBuilder.delete(OAUTH_TABLE)).prepare(session);
      updatePersonTokens = prepareUpdatePerson(PersonOAuthCols.access.name(), PersonOAuthCols.refresh.name());
      updatePersonAccess = prepareUpdatePerson(PersonOAuthCols.access.name());
      updatePersonAttrs = prepareUpdatePerson(PersonOAuthCols.attrs.name());
      getTokens = prepareGetTokens();
      removePerson = preparePersonWhere(CassandraQueryBuilder.delete(PERSON_OAUTH_TABLE)).prepare(session);
      personWith = prepareTokenWhere(CassandraQueryBuilder.select(OAUTH_TABLE)).addColumns(OAuthCols.person.name(), TTL_COLUMN).prepare(session);
      getAttrs = preparePersonWhere(CassandraQueryBuilder.select(PERSON_OAUTH_TABLE)).addColumn(PersonOAuthCols.attrs.name()).prepare(session);
   }

   @Override
   public void insertCode(String appId, String code, UUID person, Map<String, String> attrs) {
      Preconditions.checkNotNull(appId, "appId is required");
      Preconditions.checkNotNull(code, "code is required");
      Preconditions.checkNotNull(person, "person is required");

      BoundStatement stmt = bindTokenInsert(insertCode, appId, code, Type.CODE, person);
      ResultSet rs = session.execute(stmt);
      checkApplied(rs, stmt);
      stmt = bindPersonInsert(appId, person, attrs);
      rs = session.execute(stmt);
      checkApplied(rs, stmt);
   }

   @Override
   public void removeCode(String appId, String code) {
      Preconditions.checkNotNull(appId, "appId is required");
      Preconditions.checkNotNull(code, "code is required");

      removeToken(appId, code, Type.CODE);
   }

   @Override
   public void removeAccess(String appId, String access) {
      Preconditions.checkNotNull(appId, "appId is required");
      Preconditions.checkNotNull(access, "access is required");

      removeToken(appId, access, Type.ACCESS);
   }

   @Override
   public void removeRefresh(String appId, String refresh) {
      Preconditions.checkNotNull(appId, "appId is required");
      Preconditions.checkNotNull(refresh, "refresh is required");

      removeToken(appId, refresh, Type.REFRESH);
   }

   @Override
   public void updateTokens(String appId, String access, String refresh, UUID person) {
      Preconditions.checkNotNull(appId, "appId is required");
      Preconditions.checkNotNull(access, "access is required");
      Preconditions.checkNotNull(refresh, "refresh is required");
      Preconditions.checkNotNull(person, "person is required");

      BoundStatement stmt = bindTokenInsert(insertAccess, appId, access, Type.ACCESS, person);
      ResultSet rs = session.execute(stmt);
      checkApplied(rs, stmt);

      if(refresh != null) {
         stmt = bindTokenInsert(insertRefresh, appId, refresh, Type.REFRESH, person);
         rs = session.execute(stmt);
         checkApplied(rs, stmt);
      }

      BoundStatement personUpdate = updatePersonTokens.bind(access, refresh, person, appId);
      rs = session.execute(personUpdate);
      checkApplied(rs, personUpdate);
   }

   @Override
   public void updateAccessToken(String appId, String access, UUID person) {
      Preconditions.checkNotNull(appId, "appId is required");
      Preconditions.checkNotNull(access, "access is required");
      Preconditions.checkNotNull(person, "person is required");

      BoundStatement stmt = bindTokenInsert(insertAccess, appId, access, Type.ACCESS, person);
      ResultSet rs = session.execute(stmt);
      checkApplied(rs, stmt);

      BoundStatement personUpdate = updatePersonAccess.bind(access, person, appId);
      rs = session.execute(personUpdate);
      checkApplied(rs, personUpdate);
   }

   @Override
   public void updateAttrs(String appId, UUID person, Map<String, String> attrs) {
      Preconditions.checkNotNull(appId, "appId is required");
      Preconditions.checkNotNull(person, "person is required");

      attrs = attrs == null ? ImmutableMap.of() : attrs;
      BoundStatement personUpdate = updatePersonAttrs.bind(attrs, person, appId);

      ResultSet rs = session.execute(personUpdate);
      checkApplied(rs, personUpdate);
   }

   @Override
   public Map<String, String> getAttrs(String appId, UUID person) {
      Preconditions.checkNotNull(appId, "appId is required");
      Preconditions.checkNotNull(person, "person is required");

      BoundStatement stmt = getAttrs.bind(person, appId);

      Row r = session.execute(stmt).one();
      if(r == null) {
         return Collections.emptyMap();
      }

      return r.getMap(PersonOAuthCols.attrs.name(), String.class, String.class);
   }

   @Override
   public void removePersonAndTokens(String appId, UUID person) {
      Preconditions.checkNotNull(appId, "appId is required");
      Preconditions.checkNotNull(person, "person is required");

      BoundStatement bound = getTokens.bind(person, appId);

      Row r = session.execute(bound).one();
      if(r != null) {
         removeAccess(appId, r.getString(PersonOAuthCols.access.name()));
         removeRefresh(appId, r.getString(PersonOAuthCols.refresh.name()));
      }

      bound = removePerson.bind(person, appId);
      session.execute(bound);
   }

   @Override
   public Pair<UUID, Integer> getPersonWithCode(String appId, String code) {
      Preconditions.checkNotNull(appId, "appId is required");
      Preconditions.checkNotNull(code, "code is required");

      return getPersonWith(appId, code, Type.CODE);
   }

   @Override
   public Pair<UUID, Integer> getPersonWithAccess(String appId, String access) {
      Preconditions.checkNotNull(appId, "appId is required");
      Preconditions.checkNotNull(access, "access is required");

      return getPersonWith(appId, access, Type.ACCESS);
   }

   @Override
   public Pair<UUID, Integer> getPersonWithRefresh(String appId, String refresh) {
      Preconditions.checkNotNull(appId, "appId is required");
      Preconditions.checkNotNull(refresh, "refresh is required");

      return getPersonWith(appId, refresh, Type.REFRESH);
   }


   private PreparedStatement prepareTokenInsert(long ttlSecs) {
      return CassandraQueryBuilder.insert(OAUTH_TABLE)
            .addColumns(allCols(OAuthCols.values()))
            .withTtlSec(ttlSecs)
            .ifNotExists()
            .prepare(session);
   }

   private BoundStatement bindTokenInsert(PreparedStatement stmt, String appId, String token, Type type, UUID person) {
      return stmt.bind(appId, tok_0_2(token), token, type.name(), person);
   }

   private void removeToken(String appId, String token, Type type) {
      session.execute(removeToken.bind(appId, tok_0_2(token), token, type.name()));
   }

   private BoundStatement bindPersonInsert(String appId, UUID person, Map<String,String> attrs) {
      attrs = attrs == null ? ImmutableMap.of() : attrs;
      return upsertPerson.bind(person, appId, attrs);
   }

   private PreparedStatement prepareUpsertPerson() {
      return CassandraQueryBuilder.insert(PERSON_OAUTH_TABLE)
            .addColumns(PersonOAuthCols.person.name(), PersonOAuthCols.appid.name(), PersonOAuthCols.attrs.name())
            .prepare(session);
   }

   private PreparedStatement prepareUpdatePerson(String... cols) {
      return preparePersonWhere(CassandraQueryBuilder.update(PERSON_OAUTH_TABLE).ifExists())
            .addColumns(cols)
            .prepare(session);
   }

   private PreparedStatement prepareGetTokens() {
      return preparePersonWhere(CassandraQueryBuilder.select(PERSON_OAUTH_TABLE))
            .addColumns(PersonOAuthCols.access.name(), PersonOAuthCols.refresh.name())
            .prepare(session);
   }

   @SuppressWarnings("rawtypes")
   private CassandraQueryBuilder preparePersonWhere(CassandraQueryBuilder builder) {
      return builder
            .addWhereColumnEquals(PersonOAuthCols.person.name())
            .addWhereColumnEquals(PersonOAuthCols.appid.name());
   }

   @SuppressWarnings("rawtypes")
   private CassandraQueryBuilder prepareTokenWhere(CassandraQueryBuilder builder) {
      return builder
            .addWhereColumnEquals(OAuthCols.appid.name())
            .addWhereColumnEquals(OAuthCols.tok_0_2.name())
            .addWhereColumnEquals(OAuthCols.tok.name())
            .addWhereColumnEquals(OAuthCols.type.name());
   }

   private Pair<UUID, Integer> getPersonWith(String appId, String token, Type type) {
      Row r = session.execute(personWith.bind(appId, tok_0_2(token), token, type.name())).one();
      return r == null ? null : new ImmutablePair<>(r.getUuid(OAuthCols.person.name()), r.getInt(TTL_COLUMN_NAME));
   }

   @SuppressWarnings("rawtypes")
   private Collection<String> allCols(Enum... cols) {
      Set<String> colSet = new HashSet<>();
      for(Enum col : cols) { colSet.add(col.name()); }
      return colSet;
   }

   private void checkApplied(ResultSet rs, Statement<?> stmt) {
      if(rs == null || !rs.wasApplied()) {
         logger.error("result set null or not applied executing statement {}", stmt);
         throw new DaoException("unexpected error updating oauth tokens");
      }
   }

   private String tok_0_2(String token) {
      return token.substring(0, 3);
   }
}

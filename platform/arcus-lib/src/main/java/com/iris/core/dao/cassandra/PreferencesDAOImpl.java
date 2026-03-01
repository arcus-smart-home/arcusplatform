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

import static com.iris.core.dao.DaoUtils.decodeAttributesFromJson;
import static com.iris.core.dao.DaoUtils.encodeAttributesToJson;

import java.util.Map;
import java.util.UUID;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchStatementBuilder;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.DefaultBatchType;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.PreferencesDAO;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.messages.type.Preferences;

@Singleton
public class PreferencesDAOImpl implements PreferencesDAO
{
   private static final String TABLE = "preferences";

   private static final Timer saveTimer = DaoMetrics.upsertTimer(PreferencesDAO.class, "save");
   private static final Timer mergeTimer = DaoMetrics.upsertTimer(PreferencesDAO.class, "merge");
   private static final Timer findByIdTimer = DaoMetrics.readTimer(PreferencesDAO.class, "findById");
   private static final Timer deleteForPersonTimer = DaoMetrics.deleteTimer(PreferencesDAO.class, "deleteForPerson");
   private static final Timer deleteTimer = DaoMetrics.deleteTimer(PreferencesDAO.class, "delete");
   private static final Timer deletePrefTimer = DaoMetrics.deleteTimer(PreferencesDAO.class, "deletePref");

   private static class Cols
   {
      public static final String PERSON_ID = "personId";
      public static final String PLACE_ID = "placeId";
      public static final String PREFS = "prefs";
   }

   private final CqlSession session;

   private final PreparedStatement saveStatement;
   private final PreparedStatement mergeStatement;
   private final PreparedStatement findByIdStatement;
   private final PreparedStatement deleteForPersonStatement;
   private final PreparedStatement deleteStatement;
   private final PreparedStatement deletePrefStatement;

   @Inject
   public PreferencesDAOImpl(CqlSession session)
   {
      this.session = session;

      saveStatement = CassandraQueryBuilder.insert(TABLE)
         .addColumns(Cols.PERSON_ID, Cols.PLACE_ID, Cols.PREFS)
         .prepare(session);

      /*
       * Must use an UPDATE statement here in order to get upsert behavior at *both* the row level and map level.  (An
       * INSERT statement only provides upsert behavior at the row level, *not* at the map level.  It overwrites the
       * entire map if the row already exists.)
       */
      mergeStatement = CassandraQueryBuilder.update(TABLE)
         .addMapColumn(Cols.PREFS)
         .addWhereColumnEquals(Cols.PERSON_ID)
         .addWhereColumnEquals(Cols.PLACE_ID)
         .prepare(session);

      findByIdStatement = CassandraQueryBuilder.select(TABLE)
         .addColumns(Cols.PREFS)
         .addWhereColumnEquals(Cols.PERSON_ID)
         .addWhereColumnEquals(Cols.PLACE_ID)
         .prepare(session);

      deleteForPersonStatement = CassandraQueryBuilder.delete(TABLE)
         .addWhereColumnEquals(Cols.PERSON_ID)
         .prepare(session);

      deleteStatement = CassandraQueryBuilder.delete(TABLE)
         .addWhereColumnEquals(Cols.PERSON_ID)
         .addWhereColumnEquals(Cols.PLACE_ID)
         .prepare(session);

      deletePrefStatement = CassandraQueryBuilder.delete(TABLE)
         .addMapColumn(Cols.PREFS)
         .addWhereColumnEquals(Cols.PERSON_ID)
         .addWhereColumnEquals(Cols.PLACE_ID)
         .prepare(session);
   }

   @Override
   public void save(UUID personId, UUID placeId, Map<String, Object> prefs)
   {
      Map<String, String> encodedPrefs = encodeAttributesToJson(prefs);

      BoundStatement boundStatement = saveStatement.bind()
         .setUuid(Cols.PERSON_ID, personId)
         .setUuid(Cols.PLACE_ID, placeId)
         .setMap(Cols.PREFS, encodedPrefs, String.class, String.class);

      try (Context context = saveTimer.time())
      {
         session.execute(boundStatement);
      }
   }

   @Override
   public void merge(UUID personId, UUID placeId, Map<String, Object> prefs)
   {
      Map<String, String> encodedPrefs = encodeAttributesToJson(prefs);

      BatchStatementBuilder batchBuilder = BatchStatement.builder(DefaultBatchType.LOGGED);

      for (Map.Entry<String, String> encodedPref : encodedPrefs.entrySet())
      {
         BoundStatement boundStatement = mergeStatement.bind()
            .setString(0, encodedPref.getKey())
            .setString(1, encodedPref.getValue())
            .setUuid(Cols.PERSON_ID, personId)
            .setUuid(Cols.PLACE_ID, placeId);

         batchBuilder.addStatement(boundStatement);
      }

      try (Context context = mergeTimer.time())
      {
         session.execute(batchBuilder.build());
      }
   }

   @Override
   public Map<String, Object> findById(UUID personId, UUID placeId)
   {
      BoundStatement boundStatement = findByIdStatement.bind()
         .setUuid(Cols.PERSON_ID, personId)
         .setUuid(Cols.PLACE_ID, placeId);

      Row row;

      try (Context context = findByIdTimer.time())
      {
         row = session.execute(boundStatement).one();
      }

      if (row == null)
      {
         return null;
      }

      Map<String, String> prefsEncoded = row.getMap(Cols.PREFS, String.class, String.class);

      return decodeAttributesFromJson(prefsEncoded, Preferences.TYPE);
   }

   @Override
   public void deleteForPerson(UUID personId)
   {
      BoundStatement boundStatement = deleteForPersonStatement.bind()
         .setUuid(Cols.PERSON_ID, personId);

      try (Context context = deleteForPersonTimer.time())
      {
         session.execute(boundStatement);
      }
   }

   @Override
   public void delete(UUID personId, UUID placeId)
   {
      BoundStatement boundStatement = deleteStatement.bind()
         .setUuid(Cols.PERSON_ID, personId)
         .setUuid(Cols.PLACE_ID, placeId);

      try (Context context = deleteTimer.time())
      {
         session.execute(boundStatement);
      }
   }

   @Override
   public void deletePref(UUID personId, UUID placeId, String prefKey)
   {
      BoundStatement boundStatement = deletePrefStatement.bind()
         .setString(0, prefKey)
         .setUuid(Cols.PERSON_ID, personId)
         .setUuid(Cols.PLACE_ID, placeId);

      try (Context context = deletePrefTimer.time())
      {
         session.execute(boundStatement);
      }
   }
}

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
package com.iris.voice.proactive;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;
import com.iris.core.dao.metrics.DaoMetrics;

@Singleton
public class ProactiveCredsDAO {

   private static final Timer upsertTimer = DaoMetrics.upsertTimer(ProactiveCredsDAO.class, "upsert");
   private static final Timer removeTimer = DaoMetrics.deleteTimer(ProactiveCredsDAO.class, "remove");
   private static final Timer credentialsForPlaceTimer = DaoMetrics.readTimer(ProactiveCredsDAO.class, "credentialsForPlace");

   private static final String TABLE = "voice_proactive_creds";

   private enum Columns { placeId, assistant, access, accessExpiry, refresh }

   private final Session session;
   private final PreparedStatement findByPlaceId;
   private final PreparedStatement upsert;
   private final PreparedStatement delete;

   @Inject
   public ProactiveCredsDAO(Session session) {
      this.session = session;

      this.findByPlaceId = CassandraQueryBuilder.select(TABLE)
         .addColumns(EnumSet.allOf(Columns.class).stream().map(Enum::name).collect(Collectors.toSet()))
         .addWhereColumnEquals(Columns.placeId.name())
         .prepare(session);

      this.upsert = CassandraQueryBuilder.insert(TABLE)
         .addColumns(EnumSet.allOf(Columns.class).stream().map(Enum::name).collect(Collectors.toSet()))
         .prepare(session);

      this.delete = CassandraQueryBuilder.delete(TABLE)
         .addWhereColumnEquals(Columns.placeId.name())
         .addWhereColumnEquals(Columns.assistant.name())
         .prepare(session);
   }


   public Map<String, ProactiveCreds> credentialsForPlace(UUID placeId) {
      try(Timer.Context ctxt = credentialsForPlaceTimer.time()) {
         BoundStatement stmt = new BoundStatement(findByPlaceId);
         stmt.setUUID(Columns.placeId.name(), placeId);

         ResultSet rs = session.execute(stmt);
         return rs.all().stream()
            .collect(Collectors.toMap(
               row -> row.getString(Columns.assistant.name()),
               row -> new ProactiveCreds(row.getString(Columns.access.name()), row.getDate(Columns.accessExpiry.name()), row.getString(Columns.refresh.name()))
               )
            );
      }
   }

   public void upsert(UUID placeId, String assistant, ProactiveCreds credentials) {
      try(Timer.Context ctxt = upsertTimer.time()) {
         BoundStatement stmt = new BoundStatement(upsert);
         stmt.setUUID(Columns.placeId.name(), placeId);
         stmt.setString(Columns.assistant.name(), assistant);
         stmt.setString(Columns.access.name(), credentials.getAccess());
         if(credentials.getAccessExpiry() != null) {
            stmt.setDate(Columns.accessExpiry.name(), credentials.getAccessExpiry());
         } else {
            stmt.setToNull(Columns.accessExpiry.name());
         }
         if(credentials.getRefresh() != null) {
            stmt.setString(Columns.refresh.name(), credentials.getRefresh());
         } else {
            stmt.setToNull(Columns.refresh.name());
         }
         session.execute(stmt);
      }
   }

   public void remove(UUID placeId, String assistant) {
      try(Timer.Context ctxt = removeTimer.time()) {
         BoundStatement stmt = new BoundStatement(delete);
         stmt.setUUID(Columns.placeId.name(), placeId);
         stmt.setString(Columns.assistant.name(), assistant);
         session.execute(stmt);
      }
   }

}


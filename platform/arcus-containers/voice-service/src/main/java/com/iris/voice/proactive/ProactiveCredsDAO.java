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

import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.codahale.metrics.Timer;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
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

   private final CqlSession session;
   private final PreparedStatement findByPlaceId;
   private final PreparedStatement upsert;
   private final PreparedStatement delete;

   @Inject
   public ProactiveCredsDAO(CqlSession session) {
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
         BoundStatement stmt = findByPlaceId.bind(placeId);

         ResultSet rs = session.execute(stmt);
         return rs.all().stream()
            .collect(Collectors.toMap(
               row -> row.getString(Columns.assistant.name()),
               row -> new ProactiveCreds(
                  row.getString(Columns.access.name()),
                  row.isNull(Columns.accessExpiry.name()) ? null : Date.from(row.getInstant(Columns.accessExpiry.name())),
                  row.getString(Columns.refresh.name()))
               )
            );
      }
   }

   public void upsert(UUID placeId, String assistant, ProactiveCreds credentials) {
      try(Timer.Context ctxt = upsertTimer.time()) {
         BoundStatement stmt = upsert.bind(
            placeId,
            assistant,
            credentials.getAccess(),
            credentials.getAccessExpiry() != null ? credentials.getAccessExpiry().toInstant() : null,
            credentials.getRefresh()
         );
         session.execute(stmt);
      }
   }

   public void remove(UUID placeId, String assistant) {
      try(Timer.Context ctxt = removeTimer.time()) {
         BoundStatement stmt = delete.bind(placeId, assistant);
         session.execute(stmt);
      }
   }

}

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
package com.iris.voice.context;

import static com.datastax.driver.core.querybuilder.QueryBuilder.add;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.remove;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.messages.service.VoiceService.StartPlaceRequest;
import com.iris.platform.partition.PlatformPartition;

@Singleton
public class VoiceDAO {

   private static final Timer streamPlacesByPartitionTimer = DaoMetrics.readTimer(VoiceDAO.class, "streamPlacesByPartition");
   private static final Timer recordEnabledAssistantTimer = DaoMetrics.updateTimer(VoiceDAO.class, "recordEnabledAssistant");
   private static final Timer removeAssistantTimer = DaoMetrics.readTimer(VoiceDAO.class, "removeAssistant");
   private static final Timer readAssistantsTimer = DaoMetrics.readTimer(VoiceDAO.class, "readAssistants");

   private static final String TABLE = "place";

   private enum Columns { googlehome, voiceAssistants, id, partitionid }

   private final Session session;
   private final PreparedStatement streamPlacesByPartition;
   private final PreparedStatement readAssistants;

   @Inject
   public VoiceDAO(Session session) {
      this.session = session;

      streamPlacesByPartition = CassandraQueryBuilder.select(TABLE)
         .addColumns(Columns.id.name(), Columns.googlehome.name(), Columns.voiceAssistants.name())
         .addWhereColumnEquals(Columns.partitionid.name())
         .prepare(session);

      readAssistants = CassandraQueryBuilder.select(TABLE)
         .addColumns(Columns.googlehome.name(), Columns.voiceAssistants.name())
         .addWhereColumnEquals(Columns.id.name())
         .prepare(session);
   }

   public Stream<UUID> streamPlacesByPartition(PlatformPartition partition) {
      try(Timer.Context ctxt = streamPlacesByPartitionTimer.time()) {
         ResultSet rs = session.execute(new BoundStatement(streamPlacesByPartition).bind(partition.getId()));
         return StreamSupport.stream(rs.spliterator(), false)
               .filter(row ->
                  (!row.getSet(Columns.voiceAssistants.name(), String.class).isEmpty()) ||
                  (!row.isNull(Columns.googlehome.name()) && row.getBool(Columns.googlehome.name()))
               )
               .map((r) -> r.getUUID(Columns.id.name()));
      }
   }

   public void recordAssistant(UUID placeId, String assistant) {
      try(Timer.Context ctxt = recordEnabledAssistantTimer.time()) {
         session.execute(QueryBuilder
            .update(TABLE)
            .with(add(Columns.voiceAssistants.name(), assistant))
            .where(eq(Columns.id.name(), placeId))
         );
      }
   }

   public void removeAssistant(UUID placeId, String assistant) {
      try(Timer.Context ctxt = removeAssistantTimer.time()) {
         session.execute(QueryBuilder
            .update(TABLE)
            .with(remove(Columns.voiceAssistants.name(), assistant))
            .where(eq(Columns.id.name(), placeId))
         );
      }
   }

   public Set<String> readAssistants(UUID placeId) {
      try(Timer.Context ctxt = readAssistantsTimer.time()) {
         ResultSet rs = session.execute(new BoundStatement(readAssistants).bind(placeId));
         Row r = rs.one();
         if(r == null) {
            return ImmutableSet.of();
         }

         // read repair google home
         Set<String> authorizations = new HashSet<>(r.getSet(Columns.voiceAssistants.name(), String.class));
         if(!r.isNull(Columns.googlehome.name()) && r.getBool(Columns.googlehome.name()) && !authorizations.contains(StartPlaceRequest.ASSISTANT_GOOGLE)) {
            recordAssistant(placeId, StartPlaceRequest.ASSISTANT_GOOGLE);
            authorizations.add(StartPlaceRequest.ASSISTANT_GOOGLE);
         }
         return authorizations;
      }
   }
}


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

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.update;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.codahale.metrics.Timer;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
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

   private final CqlSession session;
   private final PreparedStatement streamPlacesByPartition;
   private final PreparedStatement readAssistants;

   @Inject
   public VoiceDAO(CqlSession session) {
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
         ResultSet rs = session.execute(streamPlacesByPartition.bind(partition.getId()));
         return StreamSupport.stream(rs.spliterator(), false)
               .filter(row ->
                  (!row.getSet(Columns.voiceAssistants.name(), String.class).isEmpty()) ||
                  (!row.isNull(Columns.googlehome.name()) && row.getBoolean(Columns.googlehome.name()))
               )
               .map((r) -> r.getUuid(Columns.id.name()));
      }
   }

   public void recordAssistant(UUID placeId, String assistant) {
      try(Timer.Context ctxt = recordEnabledAssistantTimer.time()) {
         session.execute(
            update(TABLE)
               .appendSetElement(Columns.voiceAssistants.name(), literal(assistant))
               .whereColumn(Columns.id.name()).isEqualTo(literal(placeId))
               .build()
         );
      }
   }

   public void removeAssistant(UUID placeId, String assistant) {
      try(Timer.Context ctxt = removeAssistantTimer.time()) {
         session.execute(
            update(TABLE)
               .removeSetElement(Columns.voiceAssistants.name(), literal(assistant))
               .whereColumn(Columns.id.name()).isEqualTo(literal(placeId))
               .build()
         );
      }
   }

   public Set<String> readAssistants(UUID placeId) {
      try(Timer.Context ctxt = readAssistantsTimer.time()) {
         ResultSet rs = session.execute(readAssistants.bind(placeId));
         Row r = rs.one();
         if(r == null) {
            return ImmutableSet.of();
         }

         // read repair google home
         Set<String> authorizations = new HashSet<>(r.getSet(Columns.voiceAssistants.name(), String.class));
         if(!r.isNull(Columns.googlehome.name()) && r.getBoolean(Columns.googlehome.name()) && !authorizations.contains(StartPlaceRequest.ASSISTANT_GOOGLE)) {
            recordAssistant(placeId, StartPlaceRequest.ASSISTANT_GOOGLE);
            authorizations.add(StartPlaceRequest.ASSISTANT_GOOGLE);
         }
         return authorizations;
      }
   }
}

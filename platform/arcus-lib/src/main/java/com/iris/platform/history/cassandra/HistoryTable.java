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
/**
 *
 */
package com.iris.platform.history.cassandra;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;
import com.iris.messages.model.CompositeId;
import com.iris.platform.history.HistoryAppenderConfig;
import com.iris.util.IrisUUID;

/**
 *
 */
public abstract class HistoryTable<I> {
   public static final int COLUMN_COUNT = 6;

   public static final class Columns {
      public static final String PLACE_ID         = "placeId";
      public static final String PERSON_ID        = "personId";
      public static final String RULE_ID          = "ruleId";
      public static final String DEVICE_ID        = "deviceId";
      public static final String HUB_ID           = "hubId";
      public static final String SUBSYSTEM        = "subsystem";
      public static final String TIMESTAMP        = "time";
      public static final String MESSAGE_KEY      = "messageKey";
      public static final String PARAMS           = "params";
      public static final String SUBJECT_ADDRESS  = "subjectAddress";
      public static final String INCIDENT_ID      = "incidentId";
   }

   @Singleton
   public static final class CriticalPlaceTable extends HistoryTable<UUID> {
      public static final String TABLE_NAME = "histlog_place_critical";

      @Inject
      public CriticalPlaceTable(@Named(CassandraHistory.NAME) Session session, HistoryAppenderConfig config) {
         super(
               CassandraQueryBuilder
                  .insert(TABLE_NAME)
                  .addColumns(Columns.PLACE_ID, Columns.TIMESTAMP, Columns.MESSAGE_KEY, Columns.PARAMS, Columns.SUBJECT_ADDRESS)
                  .withTtlSec(TimeUnit.HOURS.toSeconds(config.getCriticalPlaceTtlHours()))
                  .prepare(session),
               CassandraQueryBuilder
                  .select(TABLE_NAME)
                  .addColumns(Columns.PLACE_ID, Columns.TIMESTAMP, Columns.MESSAGE_KEY, Columns.PARAMS, Columns.SUBJECT_ADDRESS)
                  .addWhereColumnEquals(Columns.PLACE_ID)
                  .withConsistencyLevel(ConsistencyLevel.LOCAL_ONE)
                  .prepare(session),
               CassandraQueryBuilder
                  .select(TABLE_NAME)
                  .addColumns(Columns.PLACE_ID, Columns.TIMESTAMP, Columns.MESSAGE_KEY, Columns.PARAMS, Columns.SUBJECT_ADDRESS)
                  .where(Columns.PLACE_ID + " = ? AND " + Columns.TIMESTAMP + " <= ?")
                  .withConsistencyLevel(ConsistencyLevel.LOCAL_ONE)
                  .prepare(session)
         );
      }

   }

   @Singleton
   public static final class DetailedPlaceTable extends HistoryTable<UUID> {
      public static final String TABLE_NAME = "histlog_place_detailed";

      @Inject
      public DetailedPlaceTable(@Named(CassandraHistory.NAME) Session session, HistoryAppenderConfig config) {
         super(
               CassandraQueryBuilder
                  .insert(TABLE_NAME)
                  .addColumns(Columns.PLACE_ID, Columns.TIMESTAMP, Columns.MESSAGE_KEY, Columns.PARAMS, Columns.SUBJECT_ADDRESS)
                  .withTtlSec(TimeUnit.HOURS.toSeconds(config.getDetailedPlaceTtlHours()))
                  .prepare(session),
               CassandraQueryBuilder
                  .select(TABLE_NAME)
                  .addColumns(Columns.PLACE_ID, Columns.TIMESTAMP, Columns.MESSAGE_KEY, Columns.PARAMS, Columns.SUBJECT_ADDRESS)
                  .addWhereColumnEquals(Columns.PLACE_ID)
                  .withConsistencyLevel(ConsistencyLevel.LOCAL_ONE)
                  .prepare(session),
               CassandraQueryBuilder
                  .select(TABLE_NAME)
                  .addColumns(Columns.PLACE_ID, Columns.TIMESTAMP, Columns.MESSAGE_KEY, Columns.PARAMS, Columns.SUBJECT_ADDRESS)
                  .where(Columns.PLACE_ID + " = ? AND " + Columns.TIMESTAMP + " <= ?")
                  .withConsistencyLevel(ConsistencyLevel.LOCAL_ONE)
                  .prepare(session)
         );
      }

   }

   @Singleton
   public static final class DetailedPersonTable extends HistoryTable<UUID> {
      public static final String TABLE_NAME = "histlog_person_detailed";

      @Inject
      public DetailedPersonTable(@Named(CassandraHistory.NAME) Session session, HistoryAppenderConfig config) {
         super(
               CassandraQueryBuilder
                  .insert(TABLE_NAME)
                  .addColumns(Columns.PERSON_ID, Columns.TIMESTAMP, Columns.MESSAGE_KEY, Columns.PARAMS, Columns.SUBJECT_ADDRESS)
                  .withTtlSec(TimeUnit.HOURS.toSeconds(config.getDetailedPersonTtlHours()))
                  .prepare(session),
               CassandraQueryBuilder
                  .select(TABLE_NAME)
                  .addColumns(Columns.PERSON_ID, Columns.TIMESTAMP, Columns.MESSAGE_KEY, Columns.PARAMS, Columns.SUBJECT_ADDRESS)
                  .addWhereColumnEquals(Columns.PERSON_ID)
                  .withConsistencyLevel(ConsistencyLevel.LOCAL_ONE)
                  .prepare(session),
               CassandraQueryBuilder
                  .select(TABLE_NAME)
                  .addColumns(Columns.PERSON_ID, Columns.TIMESTAMP, Columns.MESSAGE_KEY, Columns.PARAMS, Columns.SUBJECT_ADDRESS)
                  .where(Columns.PERSON_ID + " = ? AND " + Columns.TIMESTAMP + " <= ?")
                  .withConsistencyLevel(ConsistencyLevel.LOCAL_ONE)
                  .prepare(session)
         );
      }

   }

   @Singleton
   public static final class DetailedRuleTable extends HistoryTable<CompositeId<UUID, Integer>> {
      public static final String TABLE_NAME = "histlog_rule_detailed";

      @Inject
      public DetailedRuleTable(@Named(CassandraHistory.NAME) Session session, HistoryAppenderConfig config) {
         super(
               CassandraQueryBuilder
                  .insert(TABLE_NAME)
                  .addColumns(Columns.PLACE_ID, Columns.RULE_ID, Columns.TIMESTAMP, Columns.MESSAGE_KEY, Columns.PARAMS, Columns.SUBJECT_ADDRESS)
                  .withTtlSec(TimeUnit.HOURS.toSeconds(config.getDetailedRuleTtlHours()))
                  .prepare(session),
               CassandraQueryBuilder
                  .select(TABLE_NAME)
                  .addColumns(Columns.PLACE_ID, Columns.RULE_ID, Columns.TIMESTAMP, Columns.MESSAGE_KEY, Columns.PARAMS, Columns.SUBJECT_ADDRESS)
                  .addWhereColumnEquals(Columns.PLACE_ID)
                  .addWhereColumnEquals(Columns.RULE_ID)
                  .withConsistencyLevel(ConsistencyLevel.LOCAL_ONE)
                  .prepare(session),
               CassandraQueryBuilder
                  .select(TABLE_NAME)
                  .addColumns(Columns.PLACE_ID, Columns.RULE_ID, Columns.TIMESTAMP, Columns.MESSAGE_KEY, Columns.PARAMS, Columns.SUBJECT_ADDRESS)
                  .where(Columns.PLACE_ID + "= ? AND " + Columns.RULE_ID + " = ? AND " + Columns.TIMESTAMP + " <= ?")
                  .withConsistencyLevel(ConsistencyLevel.LOCAL_ONE)
                  .prepare(session)
         );
      }

      @Override
      public BoundStatement listById(CompositeId<UUID, Integer> id) {
         return listById.bind(id.getPrimaryId(), id.getSecondaryId());
      }

      @Override
      public BoundStatement listByIdBefore(
            CompositeId<UUID, Integer> id,
            @Nullable Date before
      ) {
         if(before == null) {
            return listById(id);
         }
         return listByIdBefore(id, IrisUUID.timeUUID(before, Long.MAX_VALUE));
      }

      @Override
      public BoundStatement listByIdBefore(
            CompositeId<UUID, Integer> id,
            @Nullable UUID before
      ) {
         if(before == null) {
            return listById(id);
         }
         return listByIdBefore.bind(id.getPrimaryId(), id.getSecondaryId(), before);
      }

   }


   @Singleton
   public static final class DetailedSubsystemTable extends HistoryTable<CompositeId<UUID, String>> {
      public static final String TABLE_NAME = "histlog_subsys_detailed";

      @Inject
      public DetailedSubsystemTable(@Named(CassandraHistory.NAME) Session session, HistoryAppenderConfig config) {
         super(
               CassandraQueryBuilder
                  .insert(TABLE_NAME)
                  .addColumns(Columns.PLACE_ID, Columns.SUBSYSTEM, Columns.TIMESTAMP, Columns.MESSAGE_KEY, Columns.PARAMS, Columns.SUBJECT_ADDRESS)
                  .withTtlSec(TimeUnit.HOURS.toSeconds(config.getDetailedSubsysTtlHours()))
                  .prepare(session),
               CassandraQueryBuilder
                  .select(TABLE_NAME)
                  .addColumns(Columns.PLACE_ID, Columns.SUBSYSTEM, Columns.TIMESTAMP, Columns.MESSAGE_KEY, Columns.PARAMS, Columns.SUBJECT_ADDRESS)
                  .addWhereColumnEquals(Columns.PLACE_ID)
                  .addWhereColumnEquals(Columns.SUBSYSTEM)
                  .withConsistencyLevel(ConsistencyLevel.LOCAL_ONE)
                  .prepare(session),
               CassandraQueryBuilder
                  .select(TABLE_NAME)
                  .addColumns(Columns.PLACE_ID, Columns.SUBSYSTEM, Columns.TIMESTAMP, Columns.MESSAGE_KEY, Columns.PARAMS, Columns.SUBJECT_ADDRESS)
                  .where(Columns.PLACE_ID + "= ? AND " + Columns.SUBSYSTEM + " = ? AND " + Columns.TIMESTAMP + " <= ?")
                  .withConsistencyLevel(ConsistencyLevel.LOCAL_ONE)
                  .prepare(session)
         );
      }

      @Override
      public BoundStatement listById(CompositeId<UUID, String> id) {
         return listById.bind(id.getPrimaryId(), id.getSecondaryId());
      }

      @Override
      public BoundStatement listByIdBefore(
            CompositeId<UUID, String> id,
            @Nullable Date before
      ) {
         if(before == null) {
            return listById(id);
         }
         return listByIdBefore(id, IrisUUID.timeUUID(before, Long.MAX_VALUE));
      }

      @Override
      public BoundStatement listByIdBefore(
            CompositeId<UUID, String> id,
            @Nullable UUID before
      ) {
         if(before == null) {
            return listById(id);
         }
         return listByIdBefore.bind(id.getPrimaryId(), id.getSecondaryId(), before);
      }

   }


   @Singleton
   public static final class DetailedDeviceTable extends HistoryTable<UUID> {
      public static final String TABLE_NAME = "histlog_device_detailed";

      @Inject
      public DetailedDeviceTable(@Named(CassandraHistory.NAME) Session session, HistoryAppenderConfig config) {
         super(
               CassandraQueryBuilder
                  .insert(TABLE_NAME)
                  .addColumns(Columns.DEVICE_ID, Columns.TIMESTAMP, Columns.MESSAGE_KEY, Columns.PARAMS, Columns.SUBJECT_ADDRESS)
                  .withTtlSec(TimeUnit.HOURS.toSeconds(config.getDetailedDeviceTtlHours()))
                  .prepare(session),
               CassandraQueryBuilder
                  .select(TABLE_NAME)
                  .addColumns(Columns.DEVICE_ID, Columns.TIMESTAMP, Columns.MESSAGE_KEY, Columns.PARAMS, Columns.SUBJECT_ADDRESS)
                  .addWhereColumnEquals(Columns.DEVICE_ID)
                  .withConsistencyLevel(ConsistencyLevel.LOCAL_ONE)
                  .prepare(session),
               CassandraQueryBuilder
                  .select(TABLE_NAME)
                  .addColumns(Columns.DEVICE_ID, Columns.TIMESTAMP, Columns.MESSAGE_KEY, Columns.PARAMS, Columns.SUBJECT_ADDRESS)
                  .where(Columns.DEVICE_ID + " = ? AND " + Columns.TIMESTAMP + " <= ?")
                  .withConsistencyLevel(ConsistencyLevel.LOCAL_ONE)
                  .prepare(session)
         );
      }

   }

   @Singleton
   public static final class DetailedHubTable extends HistoryTable<String> {
      public static final String TABLE_NAME = "histlog_hub_detailed";

      @Inject
      public DetailedHubTable(@Named(CassandraHistory.NAME) Session session, HistoryAppenderConfig config) {
         super(
               CassandraQueryBuilder
                  .insert(TABLE_NAME)
                  .addColumns(Columns.HUB_ID, Columns.TIMESTAMP, Columns.MESSAGE_KEY, Columns.PARAMS, Columns.SUBJECT_ADDRESS)
                  .withTtlSec(TimeUnit.HOURS.toSeconds(config.getDetailedHubTtlHours()))
                  .prepare(session),
               CassandraQueryBuilder
                  .select(TABLE_NAME)
                  .addColumns(Columns.HUB_ID, Columns.TIMESTAMP, Columns.MESSAGE_KEY, Columns.PARAMS, Columns.SUBJECT_ADDRESS)
                  .addWhereColumnEquals(Columns.HUB_ID)
                  .withConsistencyLevel(ConsistencyLevel.LOCAL_ONE)
                  .prepare(session),
               CassandraQueryBuilder
                  .select(TABLE_NAME)
                  .addColumns(Columns.HUB_ID, Columns.TIMESTAMP, Columns.MESSAGE_KEY, Columns.PARAMS, Columns.SUBJECT_ADDRESS)
                  .where(Columns.HUB_ID + " = ? AND " + Columns.TIMESTAMP + " <= ?")
                  .withConsistencyLevel(ConsistencyLevel.LOCAL_ONE)
                  .prepare(session)
         );
      }

   }

   @Singleton
   public static final class DetailedAlarmTable extends HistoryTable<UUID> {
      public static final String TABLE_NAME = "histlog_alarm_detailed";

      @Inject
      public DetailedAlarmTable(@Named(CassandraHistory.NAME) Session session, HistoryAppenderConfig config) {
         super(
               CassandraQueryBuilder
                     .insert(TABLE_NAME)
                     .addColumns(Columns.INCIDENT_ID, Columns.TIMESTAMP, Columns.MESSAGE_KEY, Columns.PARAMS, Columns.SUBJECT_ADDRESS)
                     .withTtlSec(TimeUnit.HOURS.toSeconds(config.getDetailedAlarmTtlHours()))
                     .prepare(session),
               CassandraQueryBuilder
                     .select(TABLE_NAME)
                     .addColumns(Columns.INCIDENT_ID, Columns.TIMESTAMP, Columns.MESSAGE_KEY, Columns.PARAMS, Columns.SUBJECT_ADDRESS)
                     .addWhereColumnEquals(Columns.INCIDENT_ID)
                     .withConsistencyLevel(ConsistencyLevel.LOCAL_ONE)
                     .prepare(session),
               CassandraQueryBuilder
                     .select(TABLE_NAME)
                     .addColumns(Columns.INCIDENT_ID, Columns.TIMESTAMP, Columns.MESSAGE_KEY, Columns.PARAMS, Columns.SUBJECT_ADDRESS)
                     .where(Columns.INCIDENT_ID + " = ? AND " + Columns.TIMESTAMP + " <= ?")
                     .withConsistencyLevel(ConsistencyLevel.LOCAL_ONE)
                     .prepare(session)
         );
      }

   }

   protected final PreparedStatement insert;
   protected final PreparedStatement listById;
   protected final PreparedStatement listByIdBefore;

   protected HistoryTable(
         PreparedStatement insert,
         PreparedStatement listById,
         PreparedStatement listByIdBefore
   ) {
      this.insert = insert;
      this.listById = listById;
      this.listByIdBefore = listByIdBefore;
   }

   public PreparedStatement insert() {
      return insert;
   }

   public BoundStatement listById(I id) {
      return listById.bind(id);
   }

   public BoundStatement listByIdBefore(I id, @Nullable Date before) {
      if(before == null) {
         return listById(id);
      }
      return listByIdBefore(id, IrisUUID.timeUUID(before, Long.MAX_VALUE));
   }
   
   public BoundStatement listByIdBefore(I id, @Nullable UUID before) {
      if(before == null) {
         return listById(id);
      }
      return listByIdBefore.bind(id, before);
   }

}


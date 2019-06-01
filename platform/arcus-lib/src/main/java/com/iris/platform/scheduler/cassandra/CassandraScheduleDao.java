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
package com.iris.platform.scheduler.cassandra;

import java.util.Date;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;
import com.iris.core.dao.cassandra.CassandraQueryExecutor;
import com.iris.info.IrisApplicationInfo;
import com.iris.messages.address.Address;
import com.iris.platform.partition.Partitioner;
import com.iris.platform.partition.PlatformPartition;
import com.iris.platform.scheduler.ScheduleDao;
import com.iris.platform.scheduler.SchedulerConfig;
import com.iris.platform.scheduler.model.PartitionOffset;
import com.iris.platform.scheduler.model.ScheduledCommand;

/**
 *
 */
@Singleton
public class CassandraScheduleDao implements ScheduleDao {
   private static final Logger logger = LoggerFactory.getLogger(CassandraScheduleDao.class);

   // config
   private final long windowSizeMs;
   private final long defaultExpirationTimeMs;

   // injected services
   private final Session session;
   private final Partitioner partitioner;

   // queries
   private final PreparedStatement listOffsets;
   private final PreparedStatement upsertOffset;
   private final PreparedStatement streamCommandsByOffset;
   private final PreparedStatement upsertCommand;
   private final PreparedStatement deleteCommand;

   private final Function<Row, PartitionOffset> rowToOffset = (row) -> rowToOffset(row);
   private final Function<Row, Optional<ScheduledCommand>> rowToCommand = (row) -> rowToCommand(row);

   /**
    *
    */
   @Inject
   public CassandraScheduleDao(
         SchedulerConfig config,
         Session session,
         Partitioner partitioner
   ) {
      this.session = session;
      this.partitioner = partitioner;

      this.windowSizeMs = TimeUnit.SECONDS.toMillis(config.getWindowSizeSec());
      this.defaultExpirationTimeMs = TimeUnit.SECONDS.toMillis(config.getDefaultExpirationTimeSec());

      this.listOffsets =
            CassandraQueryBuilder
               .select(SchedulerOffsetTable.NAME)
               .addColumns(SchedulerOffsetTable.Columns.LAST_EXECUTED_BUCKET, SchedulerOffsetTable.Columns.PARTITION_ID, SchedulerOffsetTable.Columns.SERVER_ID)
               .prepare(session);
      this.upsertOffset =
            CassandraQueryBuilder
               .update(SchedulerOffsetTable.NAME)
               .addColumn(SchedulerOffsetTable.Columns.SERVER_ID)
               .addColumn(SchedulerOffsetTable.Columns.LAST_EXECUTED_BUCKET)
               .addWhereColumnEquals(SchedulerOffsetTable.Columns.PARTITION_ID)
               .prepare(session);

      this.streamCommandsByOffset =
            CassandraQueryBuilder
               .select(ScheduledEventTable.NAME)
               .addColumns(
                     ScheduledEventTable.Columns.EXPIRES_AT, ScheduledEventTable.Columns.PARTITION_ID,
                     ScheduledEventTable.Columns.PLACE_ID, ScheduledEventTable.Columns.SCHEDULED_TIME,
                     ScheduledEventTable.Columns.SCHEDULER, ScheduledEventTable.Columns.TIME_BUCKET
                )
               .addWhereColumnEquals(ScheduledEventTable.Columns.PARTITION_ID)
               .addWhereColumnEquals(ScheduledEventTable.Columns.TIME_BUCKET)
               .prepare(session);
      this.upsertCommand =
            CassandraQueryBuilder
               .insert(ScheduledEventTable.NAME)
               .addColumn(ScheduledEventTable.Columns.PARTITION_ID)
               .addColumn(ScheduledEventTable.Columns.TIME_BUCKET)
               .addColumn(ScheduledEventTable.Columns.PLACE_ID)
               .addColumn(ScheduledEventTable.Columns.SCHEDULER)
               .addColumn(ScheduledEventTable.Columns.SCHEDULED_TIME)
               .prepare(session);
      this.deleteCommand =
            CassandraQueryBuilder
               .delete(ScheduledEventTable.NAME)
               .addWhereColumnEquals(ScheduledEventTable.Columns.PARTITION_ID)
               .addWhereColumnEquals(ScheduledEventTable.Columns.TIME_BUCKET)
               .addWhereColumnEquals(ScheduledEventTable.Columns.SCHEDULED_TIME)
               .addWhereColumnEquals(ScheduledEventTable.Columns.SCHEDULER)
               .prepare(session);
   }

   /* (non-Javadoc)
    * @see com.iris.platform.scheduler.ScheduleDao#getTimeBucketDurationMs()
    */
   @Override
   public long getTimeBucketDurationMs() {
      return windowSizeMs;
   }

   /* (non-Javadoc)
    * @see com.iris.platform.scheduler.ScheduleDao#listPartitionOffsets()
    */
   @Override
   public List<PartitionOffset> listPartitionOffsets() {
      ResultSet rs = session.execute(listOffsets.bind());
      return CassandraQueryExecutor.list(rs, rowToOffset);
   }

   /* (non-Javadoc)
    * @see com.iris.platform.scheduler.ScheduleDao#getPartitionOffsetFor(java.util.UUID, java.util.Date)
    */
   @Override
   public PartitionOffset getPartitionOffsetFor(UUID placeId, Date time) {
      PlatformPartition partition = partitioner.getPartitionForPlaceId(placeId);
      return getPartitionOffsetFor(partition, time);
   }

   /* (non-Javadoc)
    * @see com.iris.platform.scheduler.ScheduleDao#getPartitionOffsetFor(com.iris.platform.partition.Partition, java.util.Date)
    */
   @Override
   public PartitionOffset getPartitionOffsetFor(PlatformPartition partition, Date time) {
      long bucket = Math.floorDiv(time.getTime(), windowSizeMs) * windowSizeMs;
      return new PartitionOffset(partition, new Date(bucket), windowSizeMs);
   }

   /* (non-Javadoc)
    * @see com.iris.platform.scheduler.ScheduleDao#completeOffset(com.iris.platform.scheduler.model.PartitionOffset)
    */
   @Override
   public PartitionOffset completeOffset(PartitionOffset offset) {
      Statement statement = upsertOffset.bind(IrisApplicationInfo.getContainerName(), offset.getOffset(), offset.getPartition().getId());
      session.execute(statement);
      return offset.getNextPartitionOffset();
   }

   /* (non-Javadoc)
    * @see com.iris.platform.scheduler.ScheduleDao#streamByPartitionOffset(com.iris.platform.scheduler.model.PartitionOffset)
    */
   @Override
   public Stream<ScheduledCommand> streamByPartitionOffset(PartitionOffset offset) {
      // TODO enable multi-threaded streaming
      Statement statement = streamCommandsByOffset.bind(offset.getPartition().getId(), offset.getOffset());
      ResultSet rs = session.execute(statement);
      return CassandraQueryExecutor.streamOptional(rs, rowToCommand);
   }

   /* (non-Javadoc)
    * @see com.iris.platform.scheduler.ScheduleDao#streamByPartitionOffsets(java.util.Set)
    */
   @Override
   public Stream<ScheduledCommand> streamByPartitionOffsets(Set<PartitionOffset> offsets) {
      // TODO Auto-generated method stub
      return null;
   }

   /* (non-Javadoc)
    * @see com.iris.platform.scheduler.ScheduleDao#schedule(java.util.UUID, com.iris.messages.address.Address, java.util.Date, com.google.common.base.Optional)
    */
   @Override
   public ScheduledCommand schedule(
         UUID placeId,
         Address schedulerAddress,
         Date scheduledTime,
         OptionalLong validForMs
   ) {
      Date expiresAt = new Date(System.currentTimeMillis() + validForMs.orElse(defaultExpirationTimeMs));
      PartitionOffset offset = getPartitionOffsetFor(placeId, scheduledTime);

      BoundStatement statement = upsertCommand.bind();
      statement.setInt(ScheduledEventTable.Columns.PARTITION_ID, offset.getPartition().getId());
      statement.setTimestamp(ScheduledEventTable.Columns.TIME_BUCKET, offset.getOffset());
      statement.setTimestamp(ScheduledEventTable.Columns.SCHEDULED_TIME, scheduledTime);
      statement.setUUID(ScheduledEventTable.Columns.PLACE_ID, placeId);
      statement.setString(ScheduledEventTable.Columns.SCHEDULER, schedulerAddress.getRepresentation());

      session.execute(statement);
      ScheduledCommand command = new ScheduledCommand();
      command.setOffset(offset);
      command.setPlaceId(placeId);
      command.setScheduledTime(scheduledTime);
      command.setSchedulerAddress(schedulerAddress);
      command.setExpirationTime(expiresAt);

      return command;
   }

   /* (non-Javadoc)
    * @see com.iris.platform.scheduler.ScheduleDao#reschedule(com.iris.platform.scheduler.model.ScheduledCommand, java.util.Date, com.google.common.base.Optional)
    */
   @Override
   public ScheduledCommand reschedule(ScheduledCommand command, Date newFireTime, OptionalLong validForMs) {
      ScheduledCommand updated = schedule(
            command.getPlaceId(),
            command.getSchedulerAddress(),
            newFireTime,
            validForMs
      );
      // deleting the old is only best-effort
      try {
         unschedule(command);
      }
      catch(Exception e) {
         logger.warn("Unable to delete command [{}]: {}", command, e.getMessage(), e);
      }
      return updated;
   }

   /* (non-Javadoc)
    * @see com.iris.platform.scheduler.ScheduleDao#unschedule(java.util.UUID, com.iris.messages.address.Address, java.util.Date)
    */
   @Override
   public void unschedule(UUID placeId, Address schedulerAddress, Date scheduledTime) {
      PartitionOffset offset = getPartitionOffsetFor(placeId, scheduledTime);
      Statement statement = deleteCommand.bind(
            offset.getPartition().getId(),
            offset.getOffset(),
            scheduledTime,
            schedulerAddress.getRepresentation()
      );
      session.execute(statement);
   }

   protected PartitionOffset rowToOffset(Row row) {
      PlatformPartition partition = partitioner.getPartitionById(row.getInt(SchedulerOffsetTable.Columns.PARTITION_ID));
      PartitionOffset offset = new PartitionOffset(
            partition,
            row.getTimestamp(SchedulerOffsetTable.Columns.LAST_EXECUTED_BUCKET),
            windowSizeMs
      );
      return offset;
   }

   protected Optional<ScheduledCommand> rowToCommand(Row row) {
      try {
         ScheduledCommand command = new ScheduledCommand();
         command.setPlaceId(row.getUUID(ScheduledEventTable.Columns.PLACE_ID));
         command.setScheduledTime(row.getTimestamp(ScheduledEventTable.Columns.SCHEDULED_TIME));
         command.setSchedulerAddress(Address.fromString(row.getString(ScheduledEventTable.Columns.SCHEDULER)));
         command.setExpirationTime(row.getTimestamp(ScheduledEventTable.Columns.EXPIRES_AT));

         PlatformPartition partition = partitioner.getPartitionById(row.getInt(SchedulerOffsetTable.Columns.PARTITION_ID));
         PartitionOffset offset = new PartitionOffset(
               partition,
               row.getTimestamp(ScheduledEventTable.Columns.TIME_BUCKET),
               windowSizeMs
         );
         command.setOffset(offset);

         return Optional.of(command);
      }
      catch(Exception e) {
         logger.warn("Unable to load row [{}]: {}", row, e.getMessage(), e);
         return Optional.absent();
      }
   }

}


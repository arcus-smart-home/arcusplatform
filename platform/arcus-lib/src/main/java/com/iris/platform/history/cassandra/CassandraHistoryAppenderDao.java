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
package com.iris.platform.history.cassandra;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.messages.model.CompositeId;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.cassandra.HistoryTable.CriticalPlaceTable;
import com.iris.platform.history.cassandra.HistoryTable.DetailedAlarmTable;
import com.iris.platform.history.cassandra.HistoryTable.DetailedDeviceTable;
import com.iris.platform.history.cassandra.HistoryTable.DetailedHubTable;
import com.iris.platform.history.cassandra.HistoryTable.DetailedPersonTable;
import com.iris.platform.history.cassandra.HistoryTable.DetailedPlaceTable;
import com.iris.platform.history.cassandra.HistoryTable.DetailedRuleTable;
import com.iris.platform.history.cassandra.HistoryTable.DetailedSubsystemTable;
import com.iris.util.IrisUUID;

@Singleton
public class CassandraHistoryAppenderDao implements HistoryAppenderDAO {
   private static final Logger logger = LoggerFactory.getLogger(CassandraHistoryAppenderDao.class);
   
   private static final Timer criticalPlaceLogTimer = DaoMetrics.insertTimer(HistoryAppenderDAO.class, "critical.place");
   private static final Timer detailedPlaceLogTimer = DaoMetrics.insertTimer(HistoryAppenderDAO.class, "detailed.place");
   private static final Timer detailedDeviceLogTimer = DaoMetrics.insertTimer(HistoryAppenderDAO.class, "detailed.device");
   private static final Timer detailedHubLogTimer = DaoMetrics.insertTimer(HistoryAppenderDAO.class, "detailed.hub");
   private static final Timer detailedPersonLogTimer = DaoMetrics.insertTimer(HistoryAppenderDAO.class, "detailed.person");
   private static final Timer detailedRuleLogTimer = DaoMetrics.insertTimer(HistoryAppenderDAO.class, "detailed.rule");
   private static final Timer detailedSubsystemLogTimer = DaoMetrics.insertTimer(HistoryAppenderDAO.class, "detailed.subsystem");
   private static final Timer detailedAlarmLogTimer = DaoMetrics.insertTimer(HistoryAppenderDAO.class, "detailed.alarm");
   
   private final Session session;
   private final CriticalPlaceTable criticalPlaceTable;
   private final DetailedPlaceTable detailedPlaceTable;
   private final DetailedPersonTable detailedPersonTable;
   private final DetailedDeviceTable detailedDeviceTable;
   private final DetailedHubTable detailedHubTable;
   private final DetailedRuleTable detailedRuleTable;
   private final DetailedSubsystemTable detailedSubsystemTable;
   private final DetailedAlarmTable detailedAlarmTable;
   
   private final AtomicLong nextId = new AtomicLong(0);

   @Inject
   public CassandraHistoryAppenderDao(
         @Named(CassandraHistory.NAME) Session session,
         CriticalPlaceTable criticalPlaceTable,
         DetailedPlaceTable detailedPlaceTable,
         DetailedPersonTable detailedPersonTable,
         DetailedDeviceTable detailedDeviceTable,
         DetailedHubTable detailedHubTable,
         DetailedRuleTable detailedRuleTable,
         DetailedSubsystemTable detailedSubsystemTable,
         DetailedAlarmTable detailedAlarmTable
      ) {
      this.session = session;
      this.criticalPlaceTable = criticalPlaceTable;
      this.detailedPlaceTable = detailedPlaceTable;
      this.detailedPersonTable = detailedPersonTable;
      this.detailedDeviceTable = detailedDeviceTable;
      this.detailedHubTable = detailedHubTable;
      this.detailedRuleTable = detailedRuleTable;
      this.detailedSubsystemTable = detailedSubsystemTable;
      this.detailedAlarmTable = detailedAlarmTable;
   }

   @Override
   public void appendHistoryEvent(HistoryLogEntry event) {
      try{
         doAppend(event);
         logger.debug("Inserting: {}", event);
      }catch (RuntimeException e){
         logger.warn("Unable to insert event: {}", event, e);
         throw e;
      }
   }

   @SuppressWarnings("unchecked")
   protected void doAppend(HistoryLogEntry event) {
      List<Object> values = new ArrayList<Object>(HistoryTable.COLUMN_COUNT);
      PreparedStatement stmt;

      Context metricsTimer = null;
      
      try{
         switch (event.getType()) {
         case CRITICAL_PLACE_LOG:
            stmt = criticalPlaceTable.insert();
            values.add(event.getId());
            metricsTimer = criticalPlaceLogTimer.time();
            break;
         case DETAILED_PLACE_LOG:
            stmt = detailedPlaceTable.insert();
            values.add(event.getId());
            metricsTimer = detailedPlaceLogTimer.time(); 
            break;
         case DETAILED_PERSON_LOG:
            stmt = detailedPersonTable.insert();
            values.add(event.getId());
            metricsTimer = detailedPersonLogTimer.time();
            break;
         case DETAILED_DEVICE_LOG:
            stmt = detailedDeviceTable.insert();
            values.add(event.getId());
            metricsTimer = detailedDeviceLogTimer.time();
            break;
         case DETAILED_HUB_LOG:
             stmt = detailedHubTable.insert();
             values.add(event.getId());
             metricsTimer = detailedHubLogTimer.time();
             break;
         case DETAILED_RULE_LOG:
            stmt = detailedRuleTable.insert();
            CompositeId<UUID, Integer> rid = (CompositeId<UUID, Integer>) event.getId();
            values.add(rid.getPrimaryId());
            values.add(rid.getSecondaryId());
            metricsTimer = detailedRuleLogTimer.time();
            break;
         case DETAILED_SUBSYSTEM_LOG:
            stmt = detailedSubsystemTable.insert();
            CompositeId<UUID, String> sid = (CompositeId<UUID, String>) event.getId();
            values.add(sid.getPrimaryId());
            values.add(sid.getSecondaryId());
            metricsTimer = detailedSubsystemLogTimer.time();
            break;
         case DETAILED_ALARM_LOG:
            stmt = detailedAlarmTable.insert();
            values.add(event.getId());
            metricsTimer = detailedAlarmLogTimer.time();
            break;
         default:
            throw new IllegalArgumentException("Unsupported log type:" + event.getType());
         }
         values.add(nextTimeUuid(event.getTimestamp()));
         values.add(event.getMessageKey());
         if (event.getValues() == null){
            values.add(ImmutableList.of());
         }
         else{
            values.add(event.getNonNullValues());
         }
         values.add(event.getSubjectAddress());
         
         session.execute(stmt.bind(values.toArray()));

      }finally{
         if (metricsTimer != null) {
            metricsTimer.stop();
         }
      }
   }

	private UUID nextTimeUuid(long timestamp) {
		// In a UUID the 0x10000000000th bit is reserved, to prevent repeating numbers
		// when we hit that value, just roll over at that point
		// If two samples hit in the same millisecond during a rollover they will be out of
		// order, I expect this to be very rare.
		// Additionally I expect AtomicLong#getAndIncrement to be cheaper than next random (which is what no specification will use)
		// So attempt to maintain ordering most of the time
		long nextEntry = Math.floorMod(nextId.getAndIncrement(), 0x10000000000L);
		return IrisUUID.timeUUID(timestamp, nextEntry);
	}

}


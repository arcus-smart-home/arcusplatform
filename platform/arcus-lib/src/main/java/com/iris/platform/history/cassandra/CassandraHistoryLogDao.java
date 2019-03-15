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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.messages.model.ChildId;
import com.iris.messages.model.CompositeId;
import com.iris.platform.PagedResults;
import com.iris.platform.history.HistoryLogDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.HistoryLogEntryType;
import com.iris.platform.history.SubsystemId;
import com.iris.platform.history.cassandra.HistoryTable.Columns;
import com.iris.platform.history.cassandra.HistoryTable.CriticalPlaceTable;
import com.iris.platform.history.cassandra.HistoryTable.DetailedAlarmTable;
import com.iris.platform.history.cassandra.HistoryTable.DetailedDeviceTable;
import com.iris.platform.history.cassandra.HistoryTable.DetailedHubTable;
import com.iris.platform.history.cassandra.HistoryTable.DetailedPersonTable;
import com.iris.platform.history.cassandra.HistoryTable.DetailedPlaceTable;
import com.iris.platform.history.cassandra.HistoryTable.DetailedRuleTable;
import com.iris.platform.history.cassandra.HistoryTable.DetailedSubsystemTable;

@Singleton
public class CassandraHistoryLogDao implements HistoryLogDAO {
   private static final Logger logger = LoggerFactory.getLogger(CassandraHistoryLogDao.class);
   private final Session session;
   private final CriticalPlaceTable criticalPlaceTable;
   private final DetailedPlaceTable detailedPlaceTable;
   private final DetailedPersonTable detailedPersonTable;
   private final DetailedDeviceTable detailedDeviceTable;
   private final DetailedHubTable detailedHubTable;
   private final DetailedRuleTable detailedRuleTable;
   private final DetailedSubsystemTable detailedSubsystemTable;
   private final DetailedAlarmTable detailedAlarmTable;

   @Inject
   public CassandraHistoryLogDao(
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

   /* (non-Javadoc)
    * @see com.iris.platform.history.HistoryEventDAO#listCriticalEventsByPlace(java.util.UUID, int)
    */
   @Override
   public PagedResults<HistoryLogEntry> listCriticalEntriesByPlace(UUID placeId, int limit) {
      return doList( criticalPlaceTable.listById(placeId), HistoryLogEntryType.CRITICAL_PLACE_LOG, limit );
   }

   /* (non-Javadoc)
    * @see com.iris.platform.history.HistoryEventDAO#listEventsByPlace(java.util.UUID, int)
    */
   @Override
   public PagedResults<HistoryLogEntry> listEntriesByPlace(UUID placeId, int limit) {
      return doList( detailedPlaceTable.listById(placeId), HistoryLogEntryType.DETAILED_PLACE_LOG, limit );
   }

   /* (non-Javadoc)
    * @see com.iris.platform.history.HistoryEventDAO#listEventsByDevice(java.util.UUID, int)
    */
   @Override
   public PagedResults<HistoryLogEntry> listEntriesByDevice(UUID deviceId, int limit) {
      return doList( detailedDeviceTable.listById(deviceId), HistoryLogEntryType.DETAILED_DEVICE_LOG, limit );
   }
   
   /* (non-Javadoc)
    * @see com.iris.platform.history.HistoryEventDAO#listEventsByHub(java.lang.String, int)
    */
   @Override
   public PagedResults<HistoryLogEntry> listEntriesByHub(String hubId, int limit) {
      return doList( detailedHubTable.listById(hubId), HistoryLogEntryType.DETAILED_HUB_LOG, limit );
   }

   /* (non-Javadoc)
    * @see com.iris.platform.history.HistoryEventDAO#listEventsByActor(java.util.UUID, int)
    */
   @Override
   public PagedResults<HistoryLogEntry> listEntriesByActor(UUID actorId, int limit) {
      return doList( detailedPersonTable.listById(actorId), HistoryLogEntryType.DETAILED_PERSON_LOG, limit );
   }

   /* (non-Javadoc)
    * @see com.iris.platform.history.HistoryEventDAO#listEventsByRule(com.iris.messages.model.ChildId, int)
    */
   @Override
   public PagedResults<HistoryLogEntry> listEntriesByRule(ChildId ruleId, int limit) {
      return doList( detailedRuleTable.listById(ruleId), HistoryLogEntryType.DETAILED_RULE_LOG, limit );
   }
   
   /* (non-Javadoc)
    * @see com.iris.platform.history.HistoryLogDAO#listEntriesBySubsystem(com.iris.platform.history.SubsystemId, int)
    */
   @Override
   public PagedResults<HistoryLogEntry> listEntriesBySubsystem(SubsystemId subsystemId, int limit) {
      return doList( detailedSubsystemTable.listById(subsystemId), HistoryLogEntryType.DETAILED_SUBSYSTEM_LOG, limit );
   }

   @Override
	public PagedResults<HistoryLogEntry> listEntriesByAlarmIncident(UUID incidentId, int limit) {
		return doList( detailedAlarmTable.listById(incidentId), HistoryLogEntryType.DETAILED_ALARM_LOG, limit );
	}

	@Override
   public PagedResults<HistoryLogEntry> listEntriesByQuery(ListEntriesQuery query) {
      BoundStatement stmt = bind(query);
      return doList(stmt, query.getType(), query.getLimit(), query.getFilter() == null ? Predicates.alwaysTrue() : query.getFilter());
   }

   private BoundStatement bind(ListEntriesQuery query) {
      switch(query.getType()) {
      case CRITICAL_PLACE_LOG:
         return criticalPlaceTable.listByIdBefore((UUID) query.getId(), query.getBeforeUuid());
      case DETAILED_PLACE_LOG:
         return detailedPlaceTable.listByIdBefore((UUID) query.getId(), query.getBeforeUuid());
      case DETAILED_DEVICE_LOG:
         return detailedDeviceTable.listByIdBefore((UUID) query.getId(), query.getBeforeUuid());
      case DETAILED_HUB_LOG:
          return detailedHubTable.listByIdBefore((String) query.getId(), query.getBeforeUuid());
      case DETAILED_PERSON_LOG:
         return detailedPersonTable.listByIdBefore((UUID) query.getId(), query.getBeforeUuid());
      case DETAILED_RULE_LOG:
         return detailedRuleTable.listByIdBefore((CompositeId<UUID, Integer>) query.getId(), query.getBeforeUuid());
      case DETAILED_SUBSYSTEM_LOG:
         return detailedSubsystemTable.listByIdBefore((CompositeId<UUID, String>) query.getId(), query.getBeforeUuid());
      case DETAILED_ALARM_LOG:
      	return detailedAlarmTable.listByIdBefore((UUID) query.getId(), query.getBeforeUuid());
      default:
         throw new UnsupportedOperationException("Unsupported query type " + query.getType());
      }
   }

   private PagedResults<HistoryLogEntry> doList(
         BoundStatement select, 
         HistoryLogEntryType type,
         int limit
   ) {
      return doList(
            select,
            type,
            limit,
            Predicates.alwaysTrue()
      );
   }
   
   private PagedResults<HistoryLogEntry> doList(
         BoundStatement select, 
         HistoryLogEntryType type,
         int limit,
         Predicate<HistoryLogEntry> filter
   ) {
      List<HistoryLogEntry> result = new ArrayList<>(limit);
      select.setFetchSize(limit + 1);
      ResultSet rs = session.execute( select );
      Row row = rs.one();
      for(int i=0; i<MAX_PROCESSED_ROWS && row != null && result.size() < limit; i++) {
         try {
            HistoryLogEntry event = translate(type, row);
            if(filter.apply(event)) {
               result.add(event);
            }
         }
         catch(Exception e) {
            logger.warn("Unable to deserialize row {}", row, e);
         }
         row = rs.one();
      }
      if(row == null) {
         return PagedResults.newPage(result); 
      }
      else {
         return PagedResults.newPage(result, row.getUUID(Columns.TIMESTAMP).toString());
      }
   }

   private HistoryLogEntry translate(HistoryLogEntryType type, Row row) {
      HistoryLogEntry event = new HistoryLogEntry();
      event.setType(type);
      switch(type) {
      case CRITICAL_PLACE_LOG:
      case DETAILED_PLACE_LOG:
         event.setId(row.getUUID(Columns.PLACE_ID));
         break;
      case DETAILED_PERSON_LOG:
         event.setId(row.getUUID(Columns.PERSON_ID));
         break;
      case DETAILED_DEVICE_LOG:
         event.setId(row.getUUID(Columns.DEVICE_ID));
         break;
      case DETAILED_HUB_LOG:
          event.setId(row.getString(Columns.HUB_ID));
          break;
      case DETAILED_RULE_LOG:
         ChildId id = new ChildId(
               row.getUUID(Columns.PLACE_ID),
               row.getInt(Columns.RULE_ID)
         );
         event.setId(id);
         break;
      case DETAILED_SUBSYSTEM_LOG:
         SubsystemId subsystemId = new SubsystemId(
               row.getUUID(Columns.PLACE_ID),
               row.getString(Columns.SUBSYSTEM)
         );
         event.setId(subsystemId);
         break;
      case DETAILED_ALARM_LOG:
      	event.setId(row.getUUID(Columns.INCIDENT_ID));
      	break;
      default:
         throw new IllegalArgumentException("Unsupported log type:" + event.getType());
      }
      event.setTimestamp(row.getUUID(Columns.TIMESTAMP));
      event.setMessageKey(row.getString(Columns.MESSAGE_KEY));
      event.setValues(row.getList(Columns.PARAMS, String.class));
      event.setSubjectAddress(row.getString(Columns.SUBJECT_ADDRESS));
      return event;
   }

}


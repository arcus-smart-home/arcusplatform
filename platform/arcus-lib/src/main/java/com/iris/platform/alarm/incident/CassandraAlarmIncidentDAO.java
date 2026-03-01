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
package com.iris.platform.alarm.incident;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.common.alarm.AlertType;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;
import com.iris.core.dao.cassandra.CassandraQueryBuilder.CassandraSelectBuilder;
import com.iris.core.dao.cassandra.CassandraQueryExecutor;
import com.iris.io.json.JSON;
import com.iris.messages.address.Address;
import com.iris.messages.type.TrackerEvent;
import com.iris.platform.PagedResults;
import com.iris.util.TypeMarker;

@Singleton
public class CassandraAlarmIncidentDAO implements AlarmIncidentDAO {
   private static final Logger logger = LoggerFactory.getLogger(CassandraAlarmIncidentDAO.class);
   
   private enum Column {
      placeid, incidentid, alertstate, activealerts, additionalalerts, alert, 
      cancelledby, prealertendtime, endtime, monitoringstate, tracker, 
      mockincident, monitored, confirmed, platformstate, hubstate
   }

   private static final String[] ALL_COLUMNS;
   private static final TypeMarker<Map<String, Object>> trackerMarker = new TypeMarker<Map<String, Object>>() {};
   private static final String TABLE = "alarmincident";

   static {
      Column[] columns = Column.values();
      ALL_COLUMNS = new String[columns.length];
      for(int i = 0; i < columns.length; i++) {
         ALL_COLUMNS[i] = columns[i].name();
      }
   }

   @Named("incident.ttl.secs")
   @Inject(optional = true)
   private int incidentTtl = (int) TimeUnit.DAYS.toSeconds(30);

   private final CqlSession session;
   private final PreparedStatement findById;
   private final PreparedStatement listByPlace;
   private final PreparedStatement listByPlaceBefore;
   private final PreparedStatement upsert;
   private final PreparedStatement upsertWithTtl;
   private final PreparedStatement updateMonitoringState;
   private final PreparedStatement delete;

   @Inject
   public CassandraAlarmIncidentDAO(CqlSession session) {
      this.session = session;
      findById = 
            select()
               .addColumns(ALL_COLUMNS)
               .addWhereColumnEquals(Column.placeid.name())
               .addWhereColumnEquals(Column.incidentid.name())
               .prepare(session);
      listByPlace = 
            select()
               .addWhereColumnEquals(Column.placeid.name())
               .prepare(session);
      listByPlaceBefore = 
            select()
               .where(String.format("%s = ? AND %s <= ?", Column.placeid.name(), Column.incidentid.name()))
               .prepare(session);
      upsert = prepareUpsert(-1);
      upsertWithTtl = prepareUpsert(incidentTtl);
      updateMonitoringState = CassandraQueryBuilder.update(TABLE)
            .addColumn(Column.monitoringstate.name())
            .addWhereColumnEquals(Column.placeid.name())
            .addWhereColumnEquals(Column.incidentid.name())
            .ifClause(Column.monitoringstate.name() + " = ?")
            .prepare(session);
      delete = CassandraQueryBuilder.delete(TABLE)
            .addWhereColumnEquals(Column.placeid.name())
            .addWhereColumnEquals(Column.incidentid.name())
            .prepare(session);
   }

   private CassandraSelectBuilder select() {
      return CassandraQueryBuilder.select(TABLE).addColumns(ALL_COLUMNS);
   }
   
   private PreparedStatement prepareUpsert(int ttl) {
      return CassandraQueryBuilder.insert(TABLE)
            .addColumns(ALL_COLUMNS)
            .withTtlSec(ttl)
            .prepare(session);
   }

   @Override
   public AlarmIncident findById(UUID placeId, UUID incidentId) {
      BoundStatement bs = findById.bind( placeId, incidentId );
      ResultSet rs = session.execute( bs );
      return buildIncident( rs.one() );
   }
   
   @Override
   public AlarmIncident latest(UUID placeId) {
      BoundStatement bs = listByPlace.bind(placeId);
      bs = bs.setPageSize(1);
      return buildIncident(session.execute(bs).one());
   }

   @Override
   public PagedResults<AlarmIncident> listIncidentsByQuery(AlarmIncidentQuery query) {
      BoundStatement bs;
      if(StringUtils.isEmpty(query.getToken())) {
         bs = listByPlace.bind(query.getPlaceId());
      }
      else {
         bs = listByPlaceBefore.bind(query.getPlaceId(), UUID.fromString(query.getToken()));
      }
      return CassandraQueryExecutor.page(session, bs, query.getLimit(), (row) -> this.buildIncident(row), Column.incidentid.name());
   }

   @Override
   public void upsert(AlarmIncident incident) {
      PreparedStatement pStmt = incident.isCleared() ? upsertWithTtl : upsert;
      BoundStatement bound = pStmt.bind()
         .setUuid(Column.placeid.name(), incident.getPlaceId())
         .setUuid(Column.incidentid.name(), incident.getId())
         .setString(Column.alertstate.name(), incident.getAlertState().name())
         .setString(Column.platformstate.name(),
               incident.getPlatformAlertState() == null
                  ? incident.getAlertState().name()
                  : incident.getPlatformAlertState().name())
         .setString(Column.hubstate.name(),
               incident.getHubAlertState() == null
                  ? null
                  : incident.getHubAlertState().name())
         .setSet(Column.activealerts.name(), incident.getActiveAlerts(), UUID.class)
         .setSet(Column.additionalalerts.name(), incident.getAdditionalAlerts().stream().map(AlertType::name).collect(Collectors.toSet()), String.class)
         .setString(Column.alert.name(), incident.getAlert().name())
         .setString(Column.cancelledby.name(), incident.getCancelledBy() == null ? null : incident.getCancelledBy().getRepresentation())
         .setInstant(Column.prealertendtime.name(), toInstant(incident.getPrealertEndTime()))
         .setInstant(Column.endtime.name(), toInstant(incident.getEndTime()))
         .setString(Column.monitoringstate.name(), incident.getMonitoringState().name())
         .setList(Column.tracker.name(), incident.getTracker().stream().map((te) -> JSON.toJson(te.toMap())).collect(Collectors.toList()), String.class)
         .setBoolean(Column.mockincident.name(), incident.isMockIncident())
         .setBoolean(Column.monitored.name(), incident.isMonitored())
         .setBoolean(Column.confirmed.name(),  incident.isConfirmed());
      session.execute(bound);
   }

   @Override
   public boolean updateMonitoringState(UUID placeId, UUID incidentId, AlarmIncident.MonitoringState state) {
      AlarmIncident.MonitoringState required = AlarmIncident.MonitoringState.NONE;
      switch(state) {
         case NONE:
         case PENDING: break;
         case DISPATCHING:
         case CANCELLED:
            required = AlarmIncident.MonitoringState.PENDING;
            break;
         case DISPATCHED: required = AlarmIncident.MonitoringState.DISPATCHING; break;
      }
      BoundStatement bound = updateMonitoringState.bind(state.name(), placeId, incidentId, required.name());
      ResultSet rs = session.execute(bound);
      return rs.wasApplied();
   }

   @Override
   public void delete(UUID placeId, UUID incidentId) {
      BoundStatement bound = delete.bind(placeId, incidentId);
      session.execute(bound);
   }

   private AlarmIncident buildIncident(Row r) {
      if(r == null) {
         return null;
      }
      try {
         List<TrackerEvent> events = r.getList(Column.tracker.name(), String.class).stream().map((s) -> new TrackerEvent(JSON.fromJson(s, trackerMarker))).collect(Collectors.toList());
         AlarmIncident.Builder builder = AlarmIncident.builder()
            .withAlert(AlertType.valueOf(r.getString(Column.alert.name())))
            .withMonitoringState(AlarmIncident.MonitoringState.valueOf(r.getString(Column.monitoringstate.name())))
            .withId(r.getUuid(Column.incidentid.name()))
            .withAlertState(AlarmIncident.AlertState.valueOf(r.getString(Column.alertstate.name())))
            .withPrealertEndTime(r.isNull(Column.prealertendtime.name()) ? null : Date.from(r.getInstant(Column.prealertendtime.name())))
            .withEndTime(r.isNull(Column.endtime.name()) ? null : Date.from(r.getInstant(Column.endtime.name())))
            .withPlaceId(r.getUuid(Column.placeid.name()))
            .withMonitored(r.getBoolean(Column.monitored.name()))
            .withMockIncident(r.getBoolean(Column.mockincident.name()))
            .addActiveAlertIds(r.getSet(Column.activealerts.name(), UUID.class))
            .addAdditionalAlerts(r.getSet(Column.additionalalerts.name(), String.class).stream().map(AlertType::valueOf).collect(Collectors.toSet()))
            .addTrackerEvents(events)
            .withConfirmed(r.getBoolean(Column.confirmed.name()));
         
         if(!r.isNull(Column.cancelledby.name())) {
            builder.withCancelledBy(Address.fromString(r.getString(Column.cancelledby.name())));
         }
         if(!r.isNull(Column.platformstate.name())) {
            builder.withPlatformAlertState(AlarmIncident.AlertState.valueOf(r.getString(Column.platformstate.name())));
         }
         if(!r.isNull(Column.hubstate.name())) {
            builder.withHubAlertState(AlarmIncident.AlertState.valueOf(r.getString(Column.hubstate.name())));
         }

         return builder.build();
      }
      catch(Exception e) {
         logger.warn("Invalid row [{}]", r, e);
         return null;
      }
   }

   private static Instant toInstant(Date date) {
      return date == null ? null : date.toInstant();
   }

}


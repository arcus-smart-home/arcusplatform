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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
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

   private final Session session;
   private final PreparedStatement findById;
   private final PreparedStatement listByPlace;
   private final PreparedStatement listByPlaceBefore;
   private final PreparedStatement upsert;
   private final PreparedStatement upsertWithTtl;
   private final PreparedStatement updateMonitoringState;
   private final PreparedStatement delete;

   @Inject
   public CassandraAlarmIncidentDAO(Session session) {
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
      bs.setFetchSize(1);
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
      BoundStatement bound = new BoundStatement(pStmt);
      bound.setUUID(Column.placeid.name(), incident.getPlaceId());
      bound.setUUID(Column.incidentid.name(), incident.getId());
      bound.setString(Column.alertstate.name(), incident.getAlertState().name());
      if(incident.getPlatformAlertState() == null) {
         bound.setString(Column.platformstate.name(), incident.getAlertState().name());
      }
      else {
         bound.setString(Column.platformstate.name(), incident.getPlatformAlertState().name());
      }
      if(incident.getHubAlertState() == null) {
         bound.setToNull(Column.hubstate.name());
      }
      else {
         bound.setString(Column.hubstate.name(), incident.getHubAlertState().name());
      }
      bound.setSet(Column.activealerts.name(), incident.getActiveAlerts());
      bound.setSet(Column.additionalalerts.name(), incident.getAdditionalAlerts().stream().map(AlertType::name).collect(Collectors.toSet()));
      bound.setString(Column.alert.name(), incident.getAlert().name());
      bound.setString(Column.cancelledby.name(), incident.getCancelledBy() == null ? null : incident.getCancelledBy().getRepresentation());
      bound.setTimestamp(Column.prealertendtime.name(), incident.getPrealertEndTime());
      bound.setTimestamp(Column.endtime.name(), incident.getEndTime());
      bound.setString(Column.monitoringstate.name(), incident.getMonitoringState().name());
      bound.setList(Column.tracker.name(), incident.getTracker().stream().map((te) -> JSON.toJson(te.toMap())).collect(Collectors.toList()));
      bound.setBool(Column.mockincident.name(), incident.isMockIncident());
      bound.setBool(Column.monitored.name(), incident.isMonitored());
      bound.setBool(Column.confirmed.name(),  incident.isConfirmed());
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
      BoundStatement bound = new BoundStatement(updateMonitoringState);
      bound.bind(state.name(), placeId, incidentId, required.name());
      ResultSet rs = session.execute(bound);
      return rs.wasApplied();
   }

   @Override
   public void delete(UUID placeId, UUID incidentId) {
      BoundStatement bound = new BoundStatement(delete);
      bound.bind(placeId, incidentId);
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
            .withId(r.getUUID(Column.incidentid.name()))
            .withAlertState(AlarmIncident.AlertState.valueOf(r.getString(Column.alertstate.name())))
            .withPrealertEndTime(r.getTimestamp(Column.prealertendtime.name()))
            .withEndTime(r.getTimestamp(Column.endtime.name()))
            .withPlaceId(r.getUUID(Column.placeid.name()))
            .withMonitored(r.getBool(Column.monitored.name()))
            .withMockIncident(r.getBool(Column.mockincident.name()))
            .addActiveAlertIds(r.getSet(Column.activealerts.name(), UUID.class))
            .addAdditionalAlerts(r.getSet(Column.additionalalerts.name(), String.class).stream().map(AlertType::valueOf).collect(Collectors.toSet()))
            .addTrackerEvents(events)
            .withConfirmed(r.getBool(Column.confirmed.name()));
         
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
   
}


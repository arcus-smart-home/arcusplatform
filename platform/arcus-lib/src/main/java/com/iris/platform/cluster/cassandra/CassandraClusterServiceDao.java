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
package com.iris.platform.cluster.cassandra;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.core.IrisApplicationModule;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.info.IrisApplicationInfo;
import com.iris.platform.cluster.ClusterConfig;
import com.iris.platform.cluster.ClusterServiceDao;
import com.iris.platform.cluster.ClusterServiceRecord;
import com.iris.platform.cluster.exception.ClusterIdLostException;
import com.iris.platform.cluster.exception.ClusterIdUnavailableException;
import com.iris.platform.cluster.exception.ClusterServiceDaoException;
import com.iris.platform.partition.PartitionConfig;

/**
 *
 */
public class CassandraClusterServiceDao implements ClusterServiceDao {

   private final PreparedStatement insert;
   private final PreparedStatement update;
   private final PreparedStatement heartbeat;
   private final PreparedStatement delete;
   private final PreparedStatement listByService;

   private final long timeoutMs;

   private final Clock clock;
   private final Session session;
   private final int members;
   private final String host;
   private final String service;

   /**
    *
    */
   @Inject
   public CassandraClusterServiceDao(
         Clock clock,
         Session session,
         PartitionConfig config,
         ClusterConfig clusterConfig,
         @Named(IrisApplicationModule.NAME_APPLICATION_NAME) String service
   ) {
      this.clock = clock;
      this.session = session;
      this.members = config.getMembers();
      this.host = IrisApplicationInfo.getHostName();
      this.service = service;
      this.timeoutMs = clusterConfig.getTimeoutMs();

      this.insert =
            CassandraQueryBuilder
               .insert(ClusterServiceTable.NAME)
               .addColumns(
                     ClusterServiceTable.Columns.HOST,
                     ClusterServiceTable.Columns.REGISTERED,
                     ClusterServiceTable.Columns.HEARTBEAT,
                     ClusterServiceTable.Columns.SERVICE,
                     ClusterServiceTable.Columns.CLUSTER_ID
               )
               .ifNotExists()
               .withConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
               .prepare(session);
      // NOTE: if we moved used a ttl onto the heartbeat field we could
      //       change the conditional to heartbeat == null and have cassandra manage the timeout
      //       however I think this makes testing and debugging harder
      //       Also we would still need a separate insert because heartbeat == null will NOT match
      //       rows that don't exist
      this.update =
            CassandraQueryBuilder
               .update(ClusterServiceTable.NAME)
               .addColumns(
                     ClusterServiceTable.Columns.HOST,
                     ClusterServiceTable.Columns.REGISTERED,
                     ClusterServiceTable.Columns.HEARTBEAT
               )
               .addWhereColumnEquals(ClusterServiceTable.Columns.SERVICE)
               .addWhereColumnEquals(ClusterServiceTable.Columns.CLUSTER_ID)
               .ifClause(ClusterServiceTable.Columns.HEARTBEAT + " < ?")
               .withConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
               .prepare(session);

      this.delete =
            CassandraQueryBuilder
               .delete(ClusterServiceTable.NAME)
               .addWhereColumnEquals(ClusterServiceTable.Columns.SERVICE)
               .addWhereColumnEquals(ClusterServiceTable.Columns.CLUSTER_ID)
               .ifClause(ClusterServiceTable.Columns.HEARTBEAT + " = ?")
               .withConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
               .prepare(session)
               ;

      this.heartbeat =
            CassandraQueryBuilder
               .update(ClusterServiceTable.NAME)
               .addColumn(ClusterServiceTable.Columns.HEARTBEAT)
               .addWhereColumnEquals(ClusterServiceTable.Columns.SERVICE)
               .addWhereColumnEquals(ClusterServiceTable.Columns.CLUSTER_ID)
               .ifClause(ClusterServiceTable.Columns.HOST + " = ? AND " + ClusterServiceTable.Columns.REGISTERED + " = ?")
               .withConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
               .prepare(session)
               ;
      this.listByService =
            CassandraQueryBuilder
               .select(ClusterServiceTable.NAME)
               .addColumns(ClusterServiceTable.Columns.ALL)
               .addWhereColumnEquals(ClusterServiceTable.Columns.SERVICE)
               .withConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
               .prepare(session)
               ;
   }

   @Override
   public ClusterServiceRecord register() throws ClusterIdUnavailableException {
      List<Integer> others =
            listMembersByService(service)
               .stream()
               .sorted(Comparator.comparing(ClusterServiceRecord::getLastHeartbeat))
               .map(ClusterServiceRecord::getMemberId)
               .collect(Collectors.toList());
      try(Timer.Context timer = ClusterServiceMetrics.registerTimer.time()) {
         Instant heartbeat = clock.instant();
         // try to grab an empty first
         for(int i = 0; i < members; i++) {
            if(others.contains(i)) {
               continue;
            }

            int memberId = i;
            if(tryInsert(memberId, heartbeat)) {
               return registered(memberId, heartbeat);
            }
            else {
               ClusterServiceMetrics.clusterRegistrationMissCounter.inc();
            }
         }
         // try to grab an existing one then
         for(int memberId: others) {
            if(memberId >= members) {
               // this service has been down-sized and this is no longer valid
               continue;
            }

            if(tryUpdate(memberId, heartbeat)) {
               return registered(memberId, heartbeat);
            }
            else {
               ClusterServiceMetrics.clusterRegistrationMissCounter.inc();
            }
         }
         ClusterServiceMetrics.clusterRegistrationFailedCounter.inc();
         throw new ClusterIdUnavailableException("No cluster ids for service [" + service + "] were available");
      }
   }

   private ClusterServiceRecord registered(int memberId, Instant heartbeat) {
      ClusterServiceMetrics.clusterIdRegisteredCounter.inc();
      IrisApplicationInfo.setContainerName(service + "-" + memberId);
      ClusterServiceRecord record = new ClusterServiceRecord();
      record.setHost(host);
      record.setMemberId(memberId);
      record.setService(service);
      record.setRegistered(heartbeat);
      record.setLastHeartbeat(heartbeat);
      return record;
   }

   @Override
   public ClusterServiceRecord heartbeat(ClusterServiceRecord record) throws ClusterServiceDaoException {
      try(Timer.Context timer = ClusterServiceMetrics.heartbeatTimer.time()) {
         Instant instant = clock.instant();
         Date now = new Date(instant.toEpochMilli());
         BoundStatement bs = heartbeat.bind(now, record.getService(), record.getMemberId(), record.getHost(), new Date(record.getRegistered().toEpochMilli()));
         ResultSet rs = session.execute( bs );
         if(!rs.wasApplied()) {
            ClusterServiceMetrics.clusterIdLostCounter.inc();
            throw new ClusterIdLostException("Another service has taken the member id");
         }

         ClusterServiceRecord copy = record.copy();
         copy.setLastHeartbeat(instant);
         return copy;
      }
   }

   @Override
   public boolean deregister(ClusterServiceRecord record) {
      try(Timer.Context timer = ClusterServiceMetrics.deregisterTimer.time()) {
         BoundStatement bs = delete.bind(record.getService(), record.getMemberId(), new Date(record.getLastHeartbeat().toEpochMilli()));
         ResultSet rs = session.execute( bs );
         return rs.wasApplied();
      }
   }

   @Override
   public List<ClusterServiceRecord> listMembersByService(String service) {
      List<ClusterServiceRecord> records = new ArrayList<ClusterServiceRecord>();
      try(Timer.Context timer = ClusterServiceMetrics.listByServiceTimer.time()) {
         ResultSet rs = session.execute( listByService.bind(service) );
         for(Row row: rs) {
            ClusterServiceRecord record = transform(row);
            if(record != null) {
               records.add(record);
            }
         }
      }
      return records;
   }

   private boolean tryInsert(int memberId, Instant heartbeat) {
      Date ts = new Date(heartbeat.toEpochMilli());
      return tryRegister(memberId, heartbeat, insert.bind(host, ts, ts, service, memberId));
   }

   private boolean tryUpdate(int memberId, Instant heartbeat) {
      Date ts = new Date(heartbeat.toEpochMilli());
      Date oldTs = new Date(heartbeat.toEpochMilli() - timeoutMs);
      return tryRegister(memberId, heartbeat, update.bind(host, ts, ts, service, memberId, oldTs));
   }

   private boolean tryRegister(int memberId, Instant heartbeat, BoundStatement statement) {
      ResultSet rs = session.execute( statement );
      return rs.wasApplied();
   }

   private ClusterServiceRecord transform(Row row) {
      ClusterServiceRecord record = new ClusterServiceRecord();
      record.setHost(row.getString(ClusterServiceTable.Columns.HOST));
      record.setService(row.getString(ClusterServiceTable.Columns.SERVICE));
      record.setMemberId(row.getInt(ClusterServiceTable.Columns.CLUSTER_ID));
      Date registered = row.getDate(ClusterServiceTable.Columns.REGISTERED);
      if(registered != null) {
         record.setRegistered(registered.toInstant());
      }
      Date heartbeat = row.getDate(ClusterServiceTable.Columns.HEARTBEAT);
      if(heartbeat != null) {
         record.setLastHeartbeat(heartbeat.toInstant());
      }
      return record;
   }

   private static class ClusterServiceMetrics {
      static final Timer registerTimer = DaoMetrics.upsertTimer(ClusterServiceDao.class, "register");
      static final Timer heartbeatTimer = DaoMetrics.updateTimer(ClusterServiceDao.class, "heartbeat");
      static final Timer deregisterTimer = DaoMetrics.deleteTimer(ClusterServiceDao.class, "deregister");
      static final Timer listByServiceTimer = DaoMetrics.readTimer(ClusterServiceDao.class, "listMembersByService");
      static final Counter clusterIdRegisteredCounter = DaoMetrics.counter(ClusterServiceDao.class, "clusterid.registered");
      static final Counter clusterIdLostCounter = DaoMetrics.counter(ClusterServiceDao.class, "clusterid.lost");
      static final Counter clusterRegistrationMissCounter = DaoMetrics.counter(ClusterServiceDao.class, "clusterregistration.collision");
      static final Counter clusterRegistrationFailedCounter = DaoMetrics.counter(ClusterServiceDao.class, "clusterregistration.failed");
   }

}


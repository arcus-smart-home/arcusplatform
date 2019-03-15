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
package com.iris.platform.rule.cassandra;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.google.common.base.Preconditions;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.messages.model.ChildId;
import com.iris.platform.rule.PlaceEntity;

/**
 *
 */
public abstract class BaseRuleEnvironmentDaoImpl<T extends PlaceEntity<T>> {
   protected static PreparedStatement listByPlaceStatement(Session session, String type) {
      CassandraQueryBuilder queryBuilder = CassandraQueryBuilder.select(RuleEnvironmentTable.NAME)
      				.addWhereColumnEquals(RuleEnvironmentTable.Column.PLACE_ID.columnName())
      				.where(RuleEnvironmentTable.Column.TYPE.columnName() + " = '" + type + "'");
      return RuleEnvironmentTable.addAllColumns(queryBuilder).prepare(session);
   }

   protected static PreparedStatement findByIdStatement(Session session, String type) {
      CassandraQueryBuilder queryBuilder = CassandraQueryBuilder.select(RuleEnvironmentTable.NAME)
      				.where(whereIdEq(type));
      return RuleEnvironmentTable.addAllColumns(queryBuilder).prepare(session);
   }


   protected static PreparedStatement currentSequenceNumberStatement(Session session, String sequenceFieldName) {
      return CassandraQueryBuilder.select("place")
      				.addColumn(sequenceFieldName)
      				.addWhereColumnEquals("id")
      				.prepare(session);
   }

   protected static PreparedStatement incrementSequenceIf(Session session, String sequenceFieldName) {
      return CassandraQueryBuilder.update("place")
      				.addColumn(sequenceFieldName)
      				.addWhereColumnEquals("id")
      				.ifClause(sequenceFieldName + " = ?")
      				.prepare(session);

   }

   protected static PreparedStatement deleteByIdStatement(Session session, String type) {
      return CassandraQueryBuilder.delete(RuleEnvironmentTable.NAME)
      				.where(whereIdEq(type))
      				.prepare(session);
   }

   protected static String whereIdEq(String type) {
      return
         RuleEnvironmentTable.Column.PLACE_ID + " = ? "
               + "AND " + RuleEnvironmentTable.Column.TYPE + " = '"  + type + "' "
               + "AND " + RuleEnvironmentTable.Column.ID + " = ?";
   }

   protected final Session session;
   protected final PlaceDaoMetrics metrics;

   private final String sequenceName;
   private final PreparedStatement findById;
   private final PreparedStatement listByPlace;
   private final PreparedStatement currentSequenceNumber;
   private final PreparedStatement incrementSequenceIf;
   private final PreparedStatement deleteById;

   private final int maxRetries = 3;

   public BaseRuleEnvironmentDaoImpl(Session session, String type) {
      this.session = session;
      this.metrics = new PlaceDaoMetrics(getClass());
      this.sequenceName = type + "Sequence";
      this.findById = findByIdStatement(session, type);
      this.listByPlace = listByPlaceStatement(session, type);
      this.currentSequenceNumber = currentSequenceNumberStatement(session, sequenceName);
      this.incrementSequenceIf = incrementSequenceIf(session, sequenceName);
      this.deleteById = deleteByIdStatement(session, type);
   }

   // TODO should this sit the placedao?
   protected int nextId(UUID placeId) {
      Preconditions.checkNotNull(placeId, "Must specify a place id");

      Integer currentId;
      try(Context c = metrics.startCurrentSequenceTimer()) {
         BoundStatement bs = currentSequenceNumber.bind(placeId);
         Row row = session.execute( bs ).one();
         if(row == null) {
            // TODO not found exception
            throw new IllegalArgumentException("No place with id [" + placeId + "] exists");
         }
         if(row.isNull(sequenceName)) {
            currentId = null;
         }
         else {
            currentId = row.getInt(sequenceName);
         }
      }
      return nextId(placeId, currentId);
   }

   protected int nextId(UUID placeId, Integer currentId) {
      Preconditions.checkNotNull(placeId, "Must specify a place id");

      int nextId = currentId == null ? 1 : currentId + 1;
      try(Context c = metrics.startIncrementSequenceTimer()) {
         // TODO track retries
         ResultSet rs;
         int retries = maxRetries;
         rs = session.execute( incrementSequenceIf.bind( nextId, placeId, currentId ) );
         while(!rs.wasApplied() && retries > 0) {
            currentId = rs.one().getInt(sequenceName);
            nextId=currentId+1;
            rs = session.execute( incrementSequenceIf.bind( nextId, placeId, currentId ) );
            retries--;
         }
         metrics.updateSequenceRetries(maxRetries - retries);
         if(!rs.wasApplied()) {
            // TODO DaoException?
            throw new IllegalStateException("Unable to generate nextId after " + maxRetries + " attempts");
         }
         return nextId;
      }
   }

   protected abstract T buildEntity(Row row);

   protected Statement prepareInsert(T bean, Date ts) {
      int id = nextId(bean.getPlaceId());
      bean.setId(new ChildId(bean.getPlaceId(), id));
      bean.setCreated(ts);
      bean.setModified(ts);

      return prepareUpsert(bean, ts);
   }

   protected Statement prepareUpdate(T bean, Date ts) {
      bean.setModified(ts);

      return prepareUpsert(bean, ts);
   }

   protected abstract Statement prepareUpsert(T bean, Date ts);

   public List<T> listByPlace(UUID placeId) {
      Preconditions.checkNotNull(placeId, "placeId may not be null");

      List<T> beans = new ArrayList<T>();
      try(Context c = metrics.startListByPlaceTimer()) {
         ResultSet rs = session.execute( listByPlace.bind(placeId) );
         for(Row row: rs) {
            T bean = buildEntity(row);
            beans.add(bean);
         }
         return beans;
      }
   }

   @Nullable
   public T findById(UUID placeId, Integer sequenceId) {
      Preconditions.checkNotNull(placeId);
      Preconditions.checkNotNull(sequenceId);

      try(Context c = metrics.startDaoReadTimer()) {
         BoundStatement bs = findById.bind(placeId, sequenceId);
         Row row = session.execute( bs ).one();
         if(row == null) {
            return null;
         }

         return buildEntity(row);
      }
   }

   public void save(T bean) {
      Preconditions.checkNotNull(bean, "definition may not be null");
      Preconditions.checkArgument(bean.getPlaceId() != null, "object must be associated with a place");

      boolean insert = !bean.isPersisted();
      Context c;
      Statement stmt;

      if(insert) {
         c = metrics.startDaoCreateTimer();
      }
      else {
         c = metrics.startDaoUpdateTimer();
      }
      try {
         Date ts = new Date();
         if(insert) {
            stmt = prepareInsert(bean, ts);
         }
         else {
            stmt = prepareUpdate(bean, ts);
         }

         ResultSet rs = session.execute( stmt );
         if(!rs.wasApplied()) {
            throw new IllegalStateException("Failed to persist object");
         }
      }
      finally {
         c.close();
      }
   }

   public boolean delete(UUID placeId, Integer sequenceId) {
      if(placeId == null || sequenceId == null) {
         return false;
      }
      try(Context c = metrics.startDaoDeleteTimer()) {
         BoundStatement bs = deleteById.bind(placeId, sequenceId);
         return session.execute( bs ).wasApplied();
      }
   }

   private class PlaceDaoMetrics {

      private final Timer listByPlaceTimer;
      private final Timer findByIdTimer;
      private final Timer insertTimer;
      private final Timer updateTimer;
      private final Timer deleteTimer;
      private final Timer currentSequenceTimer;
      private final Timer incrementSequenceTimer;
      private final Histogram sequenceRetriesHistogram;

      private PlaceDaoMetrics(Class<?> clazz) {
         listByPlaceTimer = DaoMetrics.readTimer(clazz, "listByPlace");
         findByIdTimer =  DaoMetrics.readTimer(clazz, "findById");
         insertTimer = DaoMetrics.insertTimer(clazz, "save");
         updateTimer = DaoMetrics.updateTimer(clazz, "save");
         deleteTimer = DaoMetrics.deleteTimer(clazz, "delete");
         currentSequenceTimer = DaoMetrics.readTimer(clazz, "nextId");
         incrementSequenceTimer = DaoMetrics.updateTimer(clazz, "nextId");
         sequenceRetriesHistogram = DaoMetrics.histogram(clazz, "sequenceretries");
      }

      void updateSequenceRetries(int retries) {
         sequenceRetriesHistogram.update(retries);
      }

      Context startIncrementSequenceTimer() {
         return incrementSequenceTimer.time();
      }

      Context startCurrentSequenceTimer() {
         return currentSequenceTimer.time();
      }

      Context startListByPlaceTimer() {
         return listByPlaceTimer.time();
      }

      Context startDaoDeleteTimer() {
         return deleteTimer.time();
      }

      Context startDaoCreateTimer() {
         return insertTimer.time();
      }

      Context startDaoUpdateTimer() {
         return updateTimer.time();
      }

      Context startDaoReadTimer() {
         return findByIdTimer.time();
      }
   }

}


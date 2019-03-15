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
package com.iris.platform.rule.cassandra;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.platform.rule.ActionDao;
import com.iris.platform.rule.ActionDefinition;
import com.iris.platform.rule.RuleDao;
import com.iris.platform.rule.RuleDefinition;
import com.iris.platform.rule.RuleEnvironment;
import com.iris.platform.rule.RuleEnvironmentDao;
import com.iris.platform.rule.cassandra.RuleEnvironmentTable.Column;
import com.iris.platform.scene.SceneDao;
import com.iris.platform.scene.SceneDefinition;

@Singleton
public class RuleEnvironmentDaoImpl implements RuleEnvironmentDao {
   private static final Logger logger = LoggerFactory.getLogger(RuleEnvironmentDaoImpl.class);

   protected static PreparedStatement streamAll(Session session) {
      CassandraQueryBuilder queryBuilder = CassandraQueryBuilder
               .select(RuleEnvironmentTable.NAME);
      return RuleEnvironmentTable.addAllColumns(queryBuilder).prepare(session);
   }

   protected static PreparedStatement listByPlaceStatement(Session session) {
      CassandraQueryBuilder queryBuilder =
            CassandraQueryBuilder
               .select(RuleEnvironmentTable.NAME)
               .addWhereColumnEquals(RuleEnvironmentTable.Column.PLACE_ID.columnName());
      return RuleEnvironmentTable.addAllColumns(queryBuilder).prepare(session);
   }

   protected static PreparedStatement deleteByPlaceStatement(Session session) {
      return
            CassandraQueryBuilder
               .delete(RuleEnvironmentTable.NAME)
               .addWhereColumnEquals(RuleEnvironmentTable.Column.PLACE_ID.columnName())
               .prepare(session);
   }

   private final RuleEnvironmentDaoMetrics metrics = new RuleEnvironmentDaoMetrics();

   private final Session session;
   private final ActionDaoImpl actionDao;
   private final RuleDaoImpl ruleDao;
   private final SceneDaoImpl sceneDao;

   private final PreparedStatement streamAll;
   private final PreparedStatement listByPlace;
   private final PreparedStatement deleteByPlace;

   @Inject
   public RuleEnvironmentDaoImpl(Session session, ActionDao actionDao, RuleDao ruleDao, SceneDao sceneDao) {
      this.session = session;
      try {
         this.actionDao = (ActionDaoImpl) actionDao;
         this.ruleDao = (RuleDaoImpl) ruleDao;
         this.sceneDao = (SceneDaoImpl) sceneDao;
      }
      catch(ClassCastException e) {
         throw new IllegalStateException("Configuration error, the Cassandra RuleEnvironmentDao has been bound, but non-Cassandra instances of action, scene and/or rule DAOs are bound");
      }

      this.streamAll = streamAll(session);
      this.listByPlace = listByPlaceStatement(session);
      this.deleteByPlace = deleteByPlaceStatement(session);
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.RuleEnvironmentDao#streamAll()
    */
   @Override
   public Stream<RuleEnvironment> streamAll() {
      Context timer = RuleEnvironmentDaoMetrics.streamAllTimer.time();
      Iterator<Row> rows = session.execute(streamAll.bind()).iterator();
      Iterator<RuleEnvironment> result = new RuleEnvironmentIterator(timer, rows);
      Spliterator<RuleEnvironment> stream = Spliterators.spliteratorUnknownSize(result, Spliterator.IMMUTABLE | Spliterator.NONNULL);
      return StreamSupport.stream(stream, false);
   }

   @Override
   public RuleEnvironment findByPlace(UUID placeId) {
      Preconditions.checkNotNull(placeId, "placeId may not be null");

      RuleEnvironmentAggregator aggregator = new RuleEnvironmentAggregator(placeId);
      // TODO check that place exists?
      try(Context c = RuleEnvironmentDaoMetrics.findByPlaceTimer.time()) {
         BoundStatement bs = listByPlace.bind(placeId);
         for(Row row: session.execute( bs )) {
            aggregator.addRow(row);
         }
      }
      return aggregator.build();
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.RuleEnvironmentDao#deleteByPlace(java.util.UUID)
    */
   @Override
   public void deleteByPlace(UUID placeId) {
      if(placeId == null) {
         return;
      }

      try(Context c = RuleEnvironmentDaoMetrics.deleteByPlaceTimer.time()) {
         BoundStatement bs = deleteByPlace.bind(placeId);
         session.execute( bs );
         // TODO throw if was applied = false
      }

   }

   private static class RuleEnvironmentDaoMetrics {
      static final Timer streamAllTimer = DaoMetrics.readTimer(RuleEnvironmentDao.class, "streamAll");
      static final Timer findByPlaceTimer = DaoMetrics.readTimer(RuleEnvironmentDao.class, "findByPlace");
      static final Timer deleteByPlaceTimer = DaoMetrics.deleteTimer(RuleEnvironmentDao.class, "deleteByPlaceTimer");
   }

   private class RuleEnvironmentIterator implements Iterator<RuleEnvironment> {
      private Context timer;
      private Iterator<Row> delegate;
      private RuleEnvironment next;
      private Row row;

      RuleEnvironmentIterator(Context timer, Iterator<Row> delegate) {
         this.timer = timer;
         this.delegate = delegate;
         this.next = readNext();
      }

      private RuleEnvironment readNext() {
         if(row == null) {
            if(delegate.hasNext()) {
               row = delegate.next();
            }
            else {
               return null;
            }
         }

         UUID placeId = row.getUUID(Column.PLACE_ID.columnName());
         RuleEnvironmentAggregator aggregator = new RuleEnvironmentAggregator(placeId);
         while(row != null && Objects.equal(aggregator.placeId, placeId)) {
            aggregator.addRow(row);
            if(delegate.hasNext()) {
               row = delegate.next();
               placeId = row.getUUID(Column.PLACE_ID.columnName());
            }
            else {
               row = null;
               placeId = null;
            }
         }
         return aggregator.build();
      }

      @Override
      public boolean hasNext() {
         return next != null;
      }

      @Override
      public RuleEnvironment next() {
         if(next == null) {
            throw new NoSuchElementException();
         }
         RuleEnvironment e = this.next;
         next = readNext();
         if(next == null) {
            timer.close();
         }
         return e;
      }

   }

   private class RuleEnvironmentAggregator {
      private UUID placeId;
      private List<ActionDefinition> actions = new ArrayList<ActionDefinition>();
      private List<RuleDefinition> rules = new ArrayList<RuleDefinition>();
      private List<SceneDefinition> scenes = new ArrayList<SceneDefinition>();

      RuleEnvironmentAggregator(UUID placeId) {
         this.placeId = placeId;
      }

      public void addRow(Row row) {
         String type = row.getString(Column.TYPE.columnName());
         if(RuleDaoImpl.TYPE.equals(type)) {
            RuleDefinition definition = ruleDao.buildEntity(row);
            rules.add(definition);
         }
         else if(ActionDaoImpl.TYPE.equals(type)) {
            ActionDefinition definition = actionDao.buildEntity(row);
            actions.add(definition);
         }
         else if(SceneDaoImpl.TYPE.equals(type)) {
            SceneDefinition definition = sceneDao.buildEntity(row);
            scenes.add(definition);
         }
         else {
            logger.debug("Unexpected rule environment type [{}], dropping row", type);
         }
      }

      public RuleEnvironment build() {
         RuleEnvironment environment = new RuleEnvironment();
         environment.setPlaceId(placeId);
         environment.setActions(actions);
         environment.setRules(rules);
         environment.setScenes(scenes);
         return environment;
      }
   }
}


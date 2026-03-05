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

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.UUID;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.platform.rule.ActionDao;
import com.iris.platform.rule.ActionDefinition;
import com.iris.platform.rule.cassandra.RuleEnvironmentTable.ActionColumn;
import com.iris.platform.rule.cassandra.RuleEnvironmentTable.Column;

/**
 *
 */
@Singleton
public class ActionDaoImpl
      extends BaseRuleEnvironmentDaoImpl<ActionDefinition>
      implements ActionDao
{
   static final String TYPE = "action";

   private static final String [] UPSERT_COLUMNS = new String [] {
      Column.CREATED.columnName(),
      Column.MODIFIED.columnName(),
      Column.NAME.columnName(),
      Column.DESCRIPTION.columnName(),
      Column.TAGS.columnName(),
      ActionColumn.ACTION.columnName()
   };

   private final CqlSession session;

   private final PreparedStatement updateLastExecution;
   private final PreparedStatement upsert;

   private static PreparedStatement upsertStatement(CqlSession session) {
      return CassandraQueryBuilder.update(RuleEnvironmentTable.NAME)
      				.addColumns(UPSERT_COLUMNS)
      				.where(whereIdEq(TYPE))
      				.prepare(session);
   }

   // TODO move this to a different table?
   private static PreparedStatement updateLastExecutionStatement(CqlSession session) {
      return CassandraQueryBuilder.update(RuleEnvironmentTable.NAME)
      				.addColumn(ActionColumn.LAST_EXECUTED.columnName())
      				.where(whereIdEq(TYPE))
      				.prepare(session);
   }

   @Inject
   public ActionDaoImpl(CqlSession session) {
      super(session, TYPE);
      this.session = session;

      this.upsert = upsertStatement(session);
      this.updateLastExecution = updateLastExecutionStatement(session);
   }

   protected ActionDefinition buildEntity(Row row) {
      ActionDefinition ad = new ActionDefinition();
      ad.setPlaceId(row.getUuid(Column.PLACE_ID.columnName()));
      ad.setSequenceId(row.getInt(Column.ID.columnName()));
      ad.setCreated(row.isNull(Column.CREATED.columnName()) ? null : Date.from(row.getInstant(Column.CREATED.columnName())));
      ad.setModified(row.isNull(Column.MODIFIED.columnName()) ? null : Date.from(row.getInstant(Column.MODIFIED.columnName())));
      ad.setLastExecuted(row.isNull(ActionColumn.LAST_EXECUTED.columnName()) ? null : Date.from(row.getInstant(ActionColumn.LAST_EXECUTED.columnName())));
      ad.setName(row.getString(Column.NAME.columnName()));
      ad.setDescription(row.getString(Column.DESCRIPTION.columnName()));
      ad.setTags(row.getSet(Column.TAGS.columnName(), String.class));

      ByteBuffer action = row.isNull(ActionColumn.ACTION.columnName()) ? null : row.getByteBuffer(ActionColumn.ACTION.columnName());
      if(action != null) {
         byte [] array = new byte[action.remaining()];
         action.get(array);
         ad.setAction(array);
      }
      return ad;
   }

   protected Statement<?> prepareUpsert(ActionDefinition ad, Date ts) {
      // note this method does not update lastExecuted
      BoundStatement bs = upsert.bind();
      bs = bs.setUuid(Column.PLACE_ID.columnName(), ad.getPlaceId());
      bs = bs.setInt(Column.ID.columnName(), ad.getSequenceId());
      bs = bs.setInstant(Column.CREATED.columnName(), ad.getCreated() == null ? null : ad.getCreated().toInstant());
      bs = bs.setInstant(Column.MODIFIED.columnName(), ad.getModified() == null ? null : ad.getModified().toInstant());
      bs = bs.setString(Column.NAME.columnName(), ad.getName());
      bs = bs.setString(Column.DESCRIPTION.columnName(), ad.getDescription());
      bs = bs.setSet(Column.TAGS.columnName(), ad.getTags(), String.class);
      bs = bs.setByteBuffer(ActionColumn.ACTION.columnName(), ByteBuffer.wrap(ad.getAction()));
      return bs;
   }

   @Override
   public Date updateLastExecutionTime(UUID placeId, int actionId) {
      Preconditions.checkNotNull(placeId, "placeId may not be null");
      Preconditions.checkArgument(actionId >= 0, "action must be persisted");

      Date ts = new Date();
      try(Context c = ActionDaoMetrics.updateLastExecTimer.time()) {
         BoundStatement bs = updateLastExecution.bind(ts, placeId, actionId);
         session.execute( bs );
      }
      return ts;
   }

   private static class ActionDaoMetrics {
      static final Timer updateLastExecTimer = DaoMetrics.updateTimer(ActionDao.class, "updateLastExecutionTime");
   }
}

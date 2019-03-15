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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;
import com.iris.core.dao.metrics.ColumnRepairMetrics;
import com.iris.io.json.JSON;
import com.iris.messages.model.CompositeId;
import com.iris.platform.rule.LegacyRuleDefinition;
import com.iris.platform.rule.LegacyRuleDefinition.Expression;
import com.iris.platform.rule.RuleDao;
import com.iris.platform.rule.RuleDefinition;
import com.iris.platform.rule.StatefulRuleDefinition;
import com.iris.platform.rule.cassandra.RuleEnvironmentTable.Column;
import com.iris.platform.rule.cassandra.RuleEnvironmentTable.RuleColumn;
import com.iris.platform.rule.catalog.action.config.ActionConfig;
import com.iris.platform.rule.catalog.condition.config.ConditionConfig;
import com.iris.util.TypeMarker;

/**
 *
 */
@Singleton
public class RuleDaoImpl
      extends BaseRuleEnvironmentDaoImpl<RuleDefinition>
      implements RuleDao
{
   static final String TYPE = "rule";
   private static final TypeMarker<List<Expression>> EXPRESSION_TYPE =
         TypeMarker.listOf(Expression.class);

   private static final String [] UPSERT_COLUMNS = new String [] {
      Column.CREATED.columnName(),
      Column.MODIFIED.columnName(),
      Column.NAME.columnName(),
      Column.DESCRIPTION.columnName(),
      Column.TAGS.columnName(),
      RuleColumn.DISABLED.columnName(),
      RuleColumn.SUSPENDED.columnName(),
      RuleColumn.EXPRESSIONS.columnName(),
      RuleColumn.TEMPLATE2.columnName(),
      RuleColumn.VARIABLES.columnName(),
      RuleColumn.ACTIONCONFIG.columnName(),
      RuleColumn.CONDITIONCONFIG.columnName(),
   };

   private final PreparedStatement upsert;
   private final PreparedStatement updateVariables;

   private static PreparedStatement upsertStatement(Session session) {
      return CassandraQueryBuilder.update(RuleEnvironmentTable.NAME)
      				.addColumns(UPSERT_COLUMNS)
      				.where(whereIdEq(TYPE))
      				.prepare(session);
   }

   private static PreparedStatement updateVariables(Session session) {
      return CassandraQueryBuilder.update(RuleEnvironmentTable.NAME)
      				.addColumn(RuleColumn.VARIABLES.columnName())
      				.where(whereIdEq(TYPE))
      				.ifClause(Column.MODIFIED + " = ?")
      				.prepare(session);
   }

   @Inject
   public RuleDaoImpl(Session session) {
      super(session, TYPE);

      this.upsert = upsertStatement(session);
      this.updateVariables=updateVariables(session);
   }

   @Override
   protected RuleDefinition buildEntity(Row row) {
      RuleDefinition definition=null;
      String expressionsJson = row.getString(RuleColumn.EXPRESSIONS.columnName());
      if(!StringUtils.isEmpty(expressionsJson)) {
         LegacyRuleDefinition legacy = new LegacyRuleDefinition();
         legacy.setExpressions(JSON.fromJson(expressionsJson, EXPRESSION_TYPE));
         definition=legacy;
      }
      else{
         StatefulRuleDefinition stateful = new StatefulRuleDefinition();
         String actionJson = row.getString(RuleColumn.ACTIONCONFIG.columnName());
         String conditionJson = row.getString(RuleColumn.CONDITIONCONFIG.columnName());

         if(!actionJson.isEmpty()){
            ActionConfig config = JSON.fromJson(actionJson,ActionConfig.class);
            stateful.setAction(config);
         }
         if(!conditionJson.isEmpty()){
            ConditionConfig config = JSON.fromJson(conditionJson, ConditionConfig.class);
            stateful.setCondition(config);
         }
         definition=stateful;
      }

      definition.setPlaceId(row.getUUID(Column.PLACE_ID.columnName()));
      definition.setSequenceId(row.getInt(Column.ID.columnName()));
      definition.setCreated(row.getDate(Column.CREATED.columnName()));
      definition.setModified(row.getDate(Column.MODIFIED.columnName()));
      definition.setName(row.getString(Column.NAME.columnName()));
      definition.setDescription(row.getString(Column.DESCRIPTION.columnName()));
      definition.setTags(row.getSet(Column.TAGS.columnName(), String.class));
      definition.setDisabled(row.getBool(RuleColumn.DISABLED.columnName()));
      definition.setSuspended(row.getBool(RuleColumn.SUSPENDED.columnName()));

      String template = row.getString(RuleColumn.TEMPLATE2.columnName());
      if(template == null) {
         ColumnRepairMetrics.incRuleTemplateCounter();
         template = row.getString(RuleColumn.TEMPLATE.columnName());
      }

      definition.setRuleTemplate(template);

      String variablesJson = row.getString(RuleColumn.VARIABLES.columnName());
      if(!StringUtils.isEmpty(variablesJson)) {
         Map<String,Object> variables = JSON.fromJson(variablesJson, TypeMarker.mapOf(Object.class));
         definition.setVariables(variables);
      }

      return definition;
   }

   @Override
   protected Statement prepareUpsert(RuleDefinition definition, Date ts) {
      // note this method does not update lastExecuted
      BoundStatement bs = upsert.bind();
      bs.setUUID(Column.PLACE_ID.columnName(), definition.getPlaceId());
      bs.setInt(Column.ID.columnName(), definition.getSequenceId());
      bs.setDate(Column.CREATED.columnName(), definition.getCreated());
      bs.setDate(Column.MODIFIED.columnName(), definition.getModified());
      bs.setString(Column.NAME.columnName(), definition.getName());
      bs.setString(Column.DESCRIPTION.columnName(), definition.getDescription());
      bs.setSet(Column.TAGS.columnName(), definition.getTags());
      bs.setBool(RuleColumn.DISABLED.columnName(), definition.isDisabled());
      bs.setBool(RuleColumn.SUSPENDED.columnName(), definition.isSuspended());
      bs.setString(RuleColumn.TEMPLATE2.columnName(), definition.getRuleTemplate());
      bs.setString(RuleColumn.VARIABLES.columnName(), JSON.toJson(definition.getVariables()));

      if((definition instanceof StatefulRuleDefinition)) {
         StatefulRuleDefinition stateful = (StatefulRuleDefinition)definition;
         bs.setString(RuleColumn.ACTIONCONFIG.columnName(), JSON.toJson(stateful.getAction()));
         bs.setString(RuleColumn.CONDITIONCONFIG.columnName(),JSON.toJson(stateful.getCondition()));
         bs.setToNull(RuleColumn.EXPRESSIONS.columnName());
      }

      if((definition instanceof LegacyRuleDefinition)) {
         bs.setToNull(RuleColumn.ACTIONCONFIG.columnName());
         bs.setToNull(RuleColumn.CONDITIONCONFIG.columnName());
         bs.setString(RuleColumn.EXPRESSIONS.columnName(), JSON.toJson(((LegacyRuleDefinition) definition).getExpressions()));
      }

      return bs;
   }

   @Override
   public void updateVariables(CompositeId<UUID, Integer> id, Map<String, Object> variables, Date modified) {
      BoundStatement bs = updateVariables.bind();
      bs.setUUID(Column.PLACE_ID.columnName(), id.getPrimaryId());
      bs.setInt(Column.ID.columnName(), id.getSecondaryId());
      bs.setString(RuleColumn.VARIABLES.columnName(), JSON.toJson(variables));
      bs.setDate(Column.MODIFIED.columnName(),modified);
      ResultSet rs = session.execute(bs);
      if(!rs.wasApplied()) {
         throw new IllegalStateException(String.format("Unable to update rule variables. Rule [%s] has been modified since read",id));
      }
   }


}


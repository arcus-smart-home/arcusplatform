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

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.servererrors.InvalidQueryException;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;

/**
 *
 */
public class RuleEnvironmentTable {
   public static final String NAME = "RuleEnvironment";

   static enum Column {
      PLACE_ID("placeId"),
      TYPE,
      ID,
      CREATED,
      MODIFIED,
      NAME,
      DESCRIPTION,
      TAGS;

      private final String columnName;
      Column() {
         this.columnName = name().toLowerCase();
      }

      Column(String columnName) {
         this.columnName = columnName;
      }

      public String columnName() {
         return columnName;
      }

      @Override
      public String toString() {
         return columnName;
      }
   }

   static enum RuleColumn {
      DISABLED("ruleDisabled"),
      SUSPENDED("ruleSuspended"),
      EXPRESSIONS("ruleExpressions"),
      @Deprecated
      TEMPLATE("ruleTemplate"),
      TEMPLATE2("ruleTemplate2"),
      VARIABLES("ruleVariables"),
      ACTIONCONFIG,
      CONDITIONCONFIG;

      private final String columnName;
      RuleColumn() {
         this.columnName = name().toLowerCase();
      }

      RuleColumn(String columnName) {
         this.columnName = columnName;
      }

      public String columnName() {
         return columnName;
      }

      @Override
      public String toString() {
         return columnName;
      }
   }

   static enum ActionColumn {
      ACTION,
      LAST_EXECUTED("actionLastExecuted");

      private final String columnName;
      ActionColumn() {
         this.columnName = name().toLowerCase();
      }

      ActionColumn(String columnName) {
         this.columnName = columnName;
      }

      public String columnName() {
         return columnName;
      }

      @Override
      public String toString() {
         return columnName;
      }
   }

   static enum SceneColumn {
      ACTION,
      SATISFIABLE("satisfiable"), // TODO move to base
      NOTIFICATION,
      TEMPLATE("sceneTemplate"),
      LAST_FIRE_TIME("sceneLastFireTime"),
      LAST_FIRE_STATE("sceneLastFireState"),
      ENABLED("sceneEnabled"),
      FILTER_UNCHANGED("sceneFilterUnchanged")
      ;

      private final String columnName;
      SceneColumn() {
         this.columnName = name().toLowerCase();
      }

      SceneColumn(String columnName) {
         this.columnName = columnName;
      }

      public String columnName() {
         return columnName;
      }

      @Override
      public String toString() {
         return columnName;
      }
   }

   /**
    * True when the table has the column. A column added by a model manager change set is missing
    * until that change set runs, and a statement that names it fails to prepare, so the DAOs leave
    * such a column out until it's there rather than fail to start.
    */
   static boolean hasColumn(CqlSession session, String column) {
      try {
         session.prepare("SELECT " + column + " FROM " + NAME + " LIMIT 1");
         return true;
      }
      catch(InvalidQueryException e) {
         return false;
      }
   }

   public static CassandraQueryBuilder addAllColumns(CqlSession session, CassandraQueryBuilder queryBuilder) {
      // added by changelog-scene-filter
      boolean filterUnchanged = hasColumn(session, SceneColumn.FILTER_UNCHANGED.columnName());
      for(RuleEnvironmentTable.Column c : RuleEnvironmentTable.Column.values()) {
         queryBuilder.addColumn(c.columnName());
      }
      for(RuleEnvironmentTable.ActionColumn c : RuleEnvironmentTable.ActionColumn.values()) {
         queryBuilder.addColumn(c.columnName());
      }
      for(RuleEnvironmentTable.RuleColumn c : RuleEnvironmentTable.RuleColumn.values()) {
         queryBuilder.addColumn(c.columnName());
      }
      for(RuleEnvironmentTable.SceneColumn c : RuleEnvironmentTable.SceneColumn.values()) {
         // already added by ActionColumn
         if(c == SceneColumn.ACTION) {
            continue;
         }
         if(c == SceneColumn.FILTER_UNCHANGED && !filterUnchanged) {
            continue;
         }
         queryBuilder.addColumn(c.columnName());
      }
      return queryBuilder;
   }

}


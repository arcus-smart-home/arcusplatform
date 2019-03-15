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
      ENABLED("sceneEnabled")
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

   public static CassandraQueryBuilder addAllColumns(CassandraQueryBuilder queryBuilder) {
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
         queryBuilder.addColumn(c.columnName());
      }
      return queryBuilder;
   }

}


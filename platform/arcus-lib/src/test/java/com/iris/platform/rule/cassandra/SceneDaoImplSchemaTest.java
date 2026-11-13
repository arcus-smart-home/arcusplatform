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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.easymock.EasyMock;
import org.junit.Test;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.servererrors.InvalidQueryException;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.platform.rule.cassandra.RuleEnvironmentTable.SceneColumn;
import com.iris.platform.scene.SceneDefinition;

/**
 * The scene DAO prepares its statements when it is built, and a statement that names a column the
 * table doesn't have fails to prepare. Until the model manager adds sceneFilterUnchanged, the DAO
 * leaves it out, so rule-service and history-service keep starting.
 */
public class SceneDaoImplSchemaTest {
   private static final String COLUMN = SceneColumn.FILTER_UNCHANGED.columnName();
   private static final String PROBE = "SELECT " + COLUMN + " FROM " + RuleEnvironmentTable.NAME + " LIMIT 1";

   private final List<String> prepared = new ArrayList<>();

   private CqlSession session(boolean columnExists) {
      CqlSession session = EasyMock.createMock(CqlSession.class);
      PreparedStatement statement = EasyMock.createNiceMock(PreparedStatement.class);
      Node node = EasyMock.createNiceMock(Node.class);
      EasyMock.expect(session.prepare(EasyMock.anyString())).andAnswer(() -> {
         String query = (String) EasyMock.getCurrentArguments()[0];
         if(query.equals(PROBE)) {
            if(!columnExists) {
               throw new InvalidQueryException(node, "Undefined column name " + COLUMN.toLowerCase(Locale.ROOT));
            }
         }
         else {
            prepared.add(query);
         }
         return statement;
      }).anyTimes();
      EasyMock.replay(session, statement, node);
      return session;
   }

   @SuppressWarnings("unchecked")
   private SceneDaoImpl dao(boolean columnExists) {
      return new SceneDaoImpl(session(columnExists), EasyMock.createNiceMock(BeanAttributesTransformer.class));
   }

   private boolean anyPreparedNamesTheColumn() {
      return prepared.stream().anyMatch((q) -> q.toLowerCase(Locale.ROOT).contains(COLUMN.toLowerCase(Locale.ROOT)));
   }

   @Test
   public void testStatementsLeaveTheColumnOutUntilItExists() {
      dao(false);
      assertFalse("nothing may name " + COLUMN + " before the change set runs: " + prepared, anyPreparedNamesTheColumn());
   }

   @Test
   public void testStatementsUseTheColumnOnceItExists() {
      dao(true);
      assertTrue(anyPreparedNamesTheColumn());
   }

   private static Row row(boolean hasColumn, Boolean value) {
      Row row = EasyMock.createNiceMock(Row.class);
      ColumnDefinitions columns = EasyMock.createNiceMock(ColumnDefinitions.class);
      EasyMock.expect(row.getColumnDefinitions()).andReturn(columns).anyTimes();
      EasyMock.expect(columns.contains(COLUMN)).andReturn(hasColumn).anyTimes();
      EasyMock.expect(row.isNull(EasyMock.anyString())).andAnswer(() -> !COLUMN.equals(EasyMock.getCurrentArguments()[0]) || value == null).anyTimes();
      if(value != null) {
         EasyMock.expect(row.getBoolean(COLUMN)).andReturn(value).anyTimes();
      }
      EasyMock.replay(row, columns);
      return row;
   }

   @Test
   public void testARowFromATableWithoutTheColumnGetsTheDefault() {
      SceneDefinition scene = dao(false).buildEntity(row(false, null));
      assertEquals(SceneDefinition.DEFAULT_FILTER_UNCHANGED, scene.isFilterUnchanged());
   }

   @Test
   public void testARowWrittenBeforeTheColumnGetsTheDefault() {
      SceneDefinition scene = dao(true).buildEntity(row(true, null));
      assertEquals(SceneDefinition.DEFAULT_FILTER_UNCHANGED, scene.isFilterUnchanged());
   }

   @Test
   public void testAStoredSettingIsRead() {
      SceneDefinition scene = dao(true).buildEntity(row(true, false));
      assertFalse(scene.isFilterUnchanged());
   }
}

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
package com.iris.modelmanager.changelog.deserializer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.iris.modelmanager.changelog.CQLCommand;
import com.iris.modelmanager.changelog.ChangeLog;
import com.iris.modelmanager.changelog.ChangeLogSet;
import com.iris.modelmanager.changelog.ChangeSet;

public class TestChangeLogDeserializer_Valid extends BaseChangeLogDeserializerTestCase {

   @Test
   public void testMinimalValidFile() throws Exception {
      ChangeLogSet set = deserializer.deserialize("valid-minimal.xml");
      assertEquals(1, set.getChangeLogs().size());
      assertMinimalExpected(set.getChangeLogs().get(0));
   }

   @Test
   public void testCompleteValidFile() throws Exception {
      ChangeLogSet set = deserializer.deserialize("valid-complete.xml");
      assertEquals(1, set.getChangeLogs().size());
      assertCompleteExpected(set.getChangeLogs().get(0));
   }

   @Test
   public void testImports() throws Exception {
      ChangeLogSet set = deserializer.deserialize("valid-import.xml");
      assertEquals(2, set.getChangeLogs().size());
      assertCompleteExpected(set.getChangeLogs().get(0));
      assertMinimalExpected(set.getChangeLogs().get(1));
   }

   private void assertMinimalExpected(ChangeLog changeLog) {
      assertEquals("valid-minimal.xml", changeLog.getSource());
      assertEquals("1.0.1", changeLog.getVersion());
      assertEquals(1, changeLog.getChangeSets().size());
      ChangeSet changeSet = changeLog.getChangeSets().get(0);
      assertEquals("rob", changeSet.getAuthor());
      assertEquals("changeset3", changeSet.getIdentifier());
      assertEquals(0, changeSet.getCommands().size());
      assertNull(changeSet.getChecksum());
      assertEquals("1.0.1", changeSet.getVersion());
      assertEquals("valid-minimal.xml", changeSet.getSource());
   }

   private void assertCompleteExpected(ChangeLog changeLog) {
      assertEquals("valid-complete.xml", changeLog.getSource());
      assertEquals("1.0.0", changeLog.getVersion());
      assertEquals(2, changeLog.getChangeSets().size());
      ChangeSet changeSet = changeLog.getChangeSets().get(0);
      assertEquals("rob", changeSet.getAuthor());
      assertEquals("changeset1", changeSet.getIdentifier());
      assertEquals("Some description" ,changeSet.getDescription());
      assertEquals("ITWO-0", changeSet.getTracking());
      assertEquals(2, changeSet.getCommands().size());
      assertTrue(((CQLCommand) changeSet.getCommands().get(0)).getUpdateCql().contains("foobar"));
      assertTrue(((CQLCommand) changeSet.getCommands().get(1)).getUpdateCql().contains("foobaz"));
      assertTrue(((CQLCommand) changeSet.getCommands().get(0)).getRollbackCql().contains("foobar"));
      assertTrue(((CQLCommand) changeSet.getCommands().get(1)).getRollbackCql().contains("foobaz"));
      assertNotNull(changeSet.getChecksum());
      assertEquals("1.0.0", changeSet.getVersion());
      assertEquals("valid-complete.xml", changeSet.getSource());

      changeSet = changeLog.getChangeSets().get(1);
      assertEquals("rob", changeSet.getAuthor());
      assertEquals("changeset2", changeSet.getIdentifier());
      assertEquals("Another description" ,changeSet.getDescription());
      assertEquals("ITWO-1", changeSet.getTracking());
      assertEquals(2, changeSet.getCommands().size());
      assertTrue(((CQLCommand) changeSet.getCommands().get(0)).getUpdateCql().contains("foobar2"));
      assertTrue(((CQLCommand) changeSet.getCommands().get(1)).getUpdateCql().contains("foobaz2"));
      assertTrue(((CQLCommand) changeSet.getCommands().get(0)).getRollbackCql().contains("foobar2"));
      assertTrue(((CQLCommand) changeSet.getCommands().get(1)).getRollbackCql().contains("foobaz2"));
      assertNotNull(changeSet.getChecksum());
      assertEquals("1.0.0", changeSet.getVersion());
      assertEquals("valid-complete.xml", changeSet.getSource());
   }
}


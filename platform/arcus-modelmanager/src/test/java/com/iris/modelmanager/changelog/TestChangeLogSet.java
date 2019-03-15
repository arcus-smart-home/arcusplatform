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
package com.iris.modelmanager.changelog;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.iris.modelmanager.changelog.checksum.ChecksumInvalidException;
import com.iris.modelmanager.changelog.checksum.ChecksumUtil;
import com.iris.modelmanager.version.Version;

public class TestChangeLogSet {

   private ChangeLog cl1;
   private ChangeLog cl2;
   private ChangeSet cs1;
   private ChangeSet cs2;
   private ChangeSet cs3;
   private ChangeSet cs4;

   @Before
   public void setUp() {
      cl1 = new ChangeLog();
      cl1.setSource("source1.xml");
      cl1.setVersion("1.0.0");

      cs1 = new ChangeSet();
      cs1.setAuthor("rob");
      cs1.setIdentifier("changeset1");
      cs1.setTimestamp(new Date(System.currentTimeMillis()));
      cs1.setTracking("ITWO-0");
      cs1.setDescription("changeset1");
      cs1.addCommand(new CQLCommand("CREATE TABLE IF NOT EXISTS foobar (id uuid)", "DROP TABLE IF EXISTS foobar"));
      ChecksumUtil.updateChecksum(cs1);
      cl1.addChangeSet(cs1);

      cs2 = new ChangeSet();
      cs2.setAuthor("rob");
      cs2.setIdentifier("changeset2");
      cs2.setTimestamp(new Date(System.currentTimeMillis() + 10));
      cs2.setTracking("ITWO-1");
      cs2.setDescription("changeset2");
      cs2.addCommand(new CQLCommand("CREATE TABLE IF NOT EXISTS foobar2 (id uuid)", "DROP TABLE IF EXISTS foobar2"));
      ChecksumUtil.updateChecksum(cs2);
      cl1.addChangeSet(cs2);

      cl2 = new ChangeLog();
      cl2.setSource("source2.xml");
      cl2.setVersion("2.0.0");

      cs3 = new ChangeSet();
      cs3.setAuthor("rob");
      cs3.setIdentifier("changeset3");
      cs3.setTimestamp(new Date(System.currentTimeMillis() + 20));
      cs3.setTracking("ITWO-2");
      cs3.setDescription("changeset3");
      cl2.addChangeSet(cs3);

      cs4 = new ChangeSet();
      cs4.setAuthor("rob");
      cs4.setIdentifier("changeset4");
      cs4.setTimestamp(new Date(System.currentTimeMillis() + 30));
      cs4.setTracking("ITWO-3");
      cs4.setDescription("changeset4");
      cl2.addChangeSet(cs4);
   }

   @Test
   public void testDifferenceWhenModelIsEmpty() {
      ChangeLogSet set1 = new ChangeLogSet(Arrays.asList(cl1, cl2));
      ChangeLogSet set2 = new ChangeLogSet(Collections.<ChangeLog>emptyList());

      List<ChangeLog> differences = set1.diff(set2);
      assertEquals(2, differences.size());
   }

   @Test
   public void testDifferenceWithNewChangeLog() {
      ChangeLogSet set1 = new ChangeLogSet(Arrays.asList(cl1));
      ChangeLogSet set2 = new ChangeLogSet(Arrays.asList(copy(cl1), cl2));

      List<ChangeLog> differences = set1.diff(set2);
      assertEquals(1, differences.size());
      assertEquals(cl2.getSource(), differences.get(0).getSource());
   }

   @Test
   public void testDifferenceInChangeSets() throws Exception {
      ChangeLogSet set1 = new ChangeLogSet(Arrays.asList(cl1));

      ChangeLog cl1Copy = new ChangeLog();
      cl1Copy.setSource(cl1.getSource());
      cl1Copy.setVersion(cl1.getVersion());
      for(ChangeSet cs : cl1.getChangeSets()) {
         cl1Copy.addChangeSet(cs);
      }

      ChangeSet cs = new ChangeSet();
      cs.setAuthor("rob");
      cs.setIdentifier("changeset5");
      cl1Copy.addChangeSet(cs);

      ChangeLogSet set2 = new ChangeLogSet(Arrays.asList(cl1Copy));

      List<ChangeLog> differences = set1.diff(set2);
      assertEquals(1, differences.size());
      assertEquals(1, differences.get(0).getChangeSets().size());
   }

   @Test
   public void testNoDifferences() throws Exception {
      ChangeLogSet set1 = new ChangeLogSet(Arrays.asList(cl1));
      ChangeLogSet set2 = new ChangeLogSet(Arrays.asList(copy(cl1)));

      List<ChangeLog> differences = set1.diff(set2);
      assertEquals(0, differences.size());
   }

   @Test
   public void testPruneChangesNullVersion() {
      ChangeLogSet set1 = new ChangeLogSet(Arrays.asList(cl1, cl2));
      List<ChangeLog> changes = set1.prune(null);
      assertEquals(2, changes.size());
   }

   @Test
   public void testPruneChangesBefore2() {
      ChangeLogSet set1 = new ChangeLogSet(Arrays.asList(cl1, cl2));
      List<ChangeLog> changes = set1.prune(Version.valueOf("1.0.0"));
      assertEquals(1, changes.size());
      assertEquals("2.0.0", changes.get(0).getVersion());
   }

   @Test
   public void testVerifyChecksums() throws ChecksumInvalidException {
      ChangeLogSet set1 = new ChangeLogSet(Arrays.asList(cl1));
      ChangeLogSet set2 = new ChangeLogSet(Arrays.asList(cl1, cl2));

      // shouldn't throw
      set1.verify(set2);
   }

   @Test(expected=ChecksumInvalidException.class)
   public void testVerifyChecksumsFails() throws Exception {

      ChangeSet cs1Copy = copy(cs1);
      cs1Copy.addCommand(new CQLCommand("foo", "foo"));

      ChangeLog copy = copy(cl1);
      copy.removeChangeSet(cs1Copy.getIdentifier());
      copy.addChangeSet(cs1Copy);

      ChecksumUtil.updateChecksum(cs1Copy);

      ChangeLogSet set1 = new ChangeLogSet(Arrays.asList(cl1));
      ChangeLogSet set2 = new ChangeLogSet(Arrays.asList(copy, cl2));
      set1.verify(set2);
   }

   @Test
   public void testCreateFromChangeSetsMatches() {
      ChangeLogSet set1 = new ChangeLogSet(Arrays.asList(cl1, cl2));
      ChangeLogSet set2 = ChangeLogSet.fromChangesets(Arrays.asList(cs4, cs3, cs2, cs1));

      List<ChangeLog> diff = set1.diff(set2);
      assertEquals(0, diff.size());
   }

   private ChangeLog copy(ChangeLog cl) {
      ChangeLog copy = new ChangeLog();
      copy.setSource(cl.getSource());
      copy.setVersion(cl.getVersion());
      for(ChangeSet cs : cl.getChangeSets()) {
         copy.addChangeSet(cs);
      }
      return copy;
   }

   private ChangeSet copy(ChangeSet cs) {
      ChangeSet copy = new ChangeSet();
      copy.setAuthor(cs.getAuthor());
      copy.setChecksum(cs.getChecksum());
      copy.setDescription(cs.getDescription());
      copy.setIdentifier(cs.getIdentifier());
      copy.setSource(cs.getSource());
      copy.setTimestamp(cs.getTimestamp());
      copy.setTracking(cs.getTracking());
      copy.setVersion(cs.getVersion());
      for(Command command : cs.getCommands()) {
         copy.addCommand(command);
      }
      return copy;
   }
}


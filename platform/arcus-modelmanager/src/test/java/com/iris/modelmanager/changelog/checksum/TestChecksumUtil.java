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
package com.iris.modelmanager.changelog.checksum;

import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.iris.modelmanager.changelog.CQLCommand;
import com.iris.modelmanager.changelog.ChangeSet;

public class TestChecksumUtil {

   @Test
   public void testChecksumMatch() throws Exception {
      ChangeSet cs = new ChangeSet();
      cs.addCommand(new CQLCommand("CREATE TABLE IF NOT EXISTS tableOne (id uuid PRIMARY KEY, col1 text", "DROP TABLE IF EXISTS tableOne"));

      ChecksumUtil.updateChecksum(cs);

      ChecksumUtil.verifyChecksums(cs, cs);
      // shouldn't throw
   }

   @Test(expected=ChecksumInvalidException.class)
   public void testChecksumDoesntMatchUpdateCQL() throws Exception {
      ChangeSet cs = new ChangeSet();
      cs.addCommand(new CQLCommand("CREATE TABLE IF NOT EXISTS tableOne (id uuid PRIMARY KEY, col1 text", "DROP TABLE IF EXISTS tableOne"));

      ChecksumUtil.updateChecksum(cs);

      ChangeSet cs2 = new ChangeSet();
      cs2.addCommand(new CQLCommand("CREATE TABLE IF NOT EXISTS tableOne (id uuid PRIMARY KEY, col1 text, col2 text", "DROP TABLE IF EXISTS tableOne"));


      ChecksumUtil.verifyChecksums(cs, cs2);
   }

   @Test(expected=ChecksumInvalidException.class)
   public void testChecksumDoesntMatchRollbackCQL() throws Exception {
      ChangeSet cs = new ChangeSet();
      cs.addCommand(new CQLCommand("CREATE TABLE IF NOT EXISTS tableOne (id uuid PRIMARY KEY, col1 text", "DROP TABLE IF EXISTS tableOne"));

      ChecksumUtil.updateChecksum(cs);

      ChangeSet cs2 = new ChangeSet();
      cs2.addCommand(new CQLCommand("CREATE TABLE IF NOT EXISTS tableOne (id uuid PRIMARY KEY, col1 text", "DROP TABLE IF EXISTS tableTwo"));


      ChecksumUtil.verifyChecksums(cs, cs2);
   }

   @Test
   public void testChecksumNullIfNoCommands() {
      ChangeSet cs = new ChangeSet();

      ChecksumUtil.updateChecksum(cs);

      assertNull(cs.getChecksum());
   }
}


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
package com.iris.core.dao.cassandra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Test;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.iris.core.dao.cassandra.RecordingCqlSession.Bound;
import com.iris.security.passkey.Passkey;

/**
 * Every service with a PersonDAO builds this DAO at startup, whether or not passkeys
 * are on, so it must not need the passkey tables until it's used.
 */
public class PasskeyDAOImplTest {
   private static final int STATEMENTS = 12;

   @Test
   public void testStartsWithoutTouchingCassandra() {
      CqlSession session = EasyMock.createMock(CqlSession.class);
      EasyMock.replay(session);
      new PasskeyDAOImpl(session);
      EasyMock.verify(session);
   }

   @Test
   public void testPreparesOnFirstUseAndRetriesAFailure() {
      UUID personId = UUID.randomUUID();
      PreparedStatement prepared = EasyMock.createMock(PreparedStatement.class);
      BoundStatement bound = EasyMock.createMock(BoundStatement.class);
      ResultSet result = EasyMock.createMock(ResultSet.class);
      EasyMock.expect(prepared.bind(personId)).andReturn(bound).times(2);
      EasyMock.expect(result.all()).andReturn(Collections.emptyList()).times(2);

      CqlSession session = EasyMock.createMock(CqlSession.class);
      EasyMock.expect(session.prepare(EasyMock.anyString()))
            .andThrow(new IllegalStateException("unconfigured table passkey"))
            .andReturn(prepared).times(STATEMENTS);
      EasyMock.expect(session.execute(bound)).andReturn(result).times(2);
      EasyMock.replay(prepared, bound, result, session);

      PasskeyDAOImpl dao = new PasskeyDAOImpl(session);
      try {
         dao.findByPerson(personId);
         fail("used statements that failed to prepare");
      }
      catch (IllegalStateException expected) {
      }
      // once the tables exist the next call prepares everything, and later calls reuse it
      assertTrue(dao.findByPerson(personId).isEmpty());
      assertTrue(dao.findByPerson(personId).isEmpty());
      EasyMock.verify(prepared, result, session);
   }

   @Test
   public void testUsageIsOnlyRecordedForAPasskeyThatStillExists() {
      RecordingCqlSession cassandra = new RecordingCqlSession();
      PasskeyDAOImpl dao = new PasskeyDAOImpl(cassandra.session);
      UUID personId = UUID.randomUUID();
      Instant now = Instant.now();

      assertTrue(dao.updateUsage(personId, "credential", 3, now));
      Bound update = cassandra.executed.get(0).get(0);
      // conditional, so a login racing a delete can't write part of the deleted row back
      assertTrue(update.query, update.query.startsWith("UPDATE passkey SET ") && update.query.endsWith(" IF EXISTS"));
      assertEquals(Arrays.asList(3L, now, personId, "credential"), update.values);

      // the passkey was deleted first
      cassandra.results = bound -> RecordingCqlSession.result(false, Collections.emptyList());
      assertFalse(dao.updateUsage(personId, "credential", 4, now));
   }

   @Test
   public void testSaveIsConditional() {
      RecordingCqlSession cassandra = new RecordingCqlSession();
      Passkey passkey = new Passkey();
      passkey.setPersonId(UUID.randomUUID());
      passkey.setCredentialId("credential");

      PasskeyDAOImpl dao = new PasskeyDAOImpl(cassandra.session);
      assertTrue(dao.save(passkey));

      // like every write to the passkey table, so Cassandra orders them all
      assertEquals(cassandra.executed.toString(), 2, cassandra.executed.size());
      Bound row = cassandra.executed.get(0).get(0);
      assertTrue(row.query, row.query.startsWith("INSERT INTO passkey (") && row.query.endsWith(" IF NOT EXISTS"));
      assertEquals(passkey.getPersonId(), row.values.get(0));
      Bound lookup = cassandra.executed.get(1).get(0);
      assertEquals("INSERT INTO passkey_by_credential (credentialId, personId) VALUES (?, ?)", lookup.query);
      assertEquals(Arrays.asList("credential", passkey.getPersonId()), lookup.values);

      // a row is already stored: the lookup row isn't written, so that row's key stays unusable
      cassandra.executed.clear();
      cassandra.results = bound -> RecordingCqlSession.result(false, Collections.emptyList());
      assertFalse(dao.save(passkey));
      assertEquals(cassandra.executed.toString(), 1, cassandra.executed.size());
   }

   @Test
   public void testDeleteIsConditional() {
      RecordingCqlSession cassandra = new RecordingCqlSession();
      PasskeyDAOImpl dao = new PasskeyDAOImpl(cassandra.session);
      UUID personId = UUID.randomUUID();

      assertTrue(dao.delete(personId, "credential"));

      assertEquals(cassandra.executed.toString(), 2, cassandra.executed.size());
      assertDeleted(cassandra.executed.get(0), cassandra.executed.get(1), personId, "credential");

      // another request deleted it first
      cassandra.results = bound -> RecordingCqlSession.result(false, Collections.emptyList());
      assertFalse(dao.delete(personId, "credential"));
   }

   @Test
   public void testDeleteForPersonRemovesEveryPasskeyAndItsLookup() {
      RecordingCqlSession cassandra = new RecordingCqlSession();
      List<Row> rows = Arrays.asList(row("first"), row("second"));
      cassandra.results = bound -> RecordingCqlSession.result(true, bound.query.startsWith("SELECT ") ? rows : Collections.emptyList());
      UUID personId = UUID.randomUUID();

      new PasskeyDAOImpl(cassandra.session).deleteForPerson(personId);

      assertEquals(cassandra.executed.toString(), 5, cassandra.executed.size());
      Bound read = cassandra.executed.get(0).get(0);
      // only the ids, not the keys
      assertEquals("SELECT credentialId FROM passkey WHERE personId = ?", read.query);
      assertEquals(Collections.singletonList(personId), read.values);
      // a row at a time, each conditional like delete
      assertDeleted(cassandra.executed.get(1), cassandra.executed.get(2), personId, "first");
      assertDeleted(cassandra.executed.get(3), cassandra.executed.get(4), personId, "second");
   }

   private static void assertDeleted(List<Bound> passkeyStatement, List<Bound> lookupStatement, UUID personId, String credentialId) {
      assertEquals(1, passkeyStatement.size());
      assertEquals("DELETE FROM passkey WHERE personId = ? AND credentialId = ? IF EXISTS", passkeyStatement.get(0).query);
      assertEquals(Arrays.asList(personId, credentialId), passkeyStatement.get(0).values);
      assertEquals(1, lookupStatement.size());
      assertEquals("DELETE FROM passkey_by_credential WHERE credentialId = ?", lookupStatement.get(0).query);
      assertEquals(Collections.singletonList(credentialId), lookupStatement.get(0).values);
   }

   private static Row row(String credentialId) {
      Row row = EasyMock.createNiceMock(Row.class);
      EasyMock.expect(row.getString("credentialId")).andReturn(credentialId).anyTimes();
      EasyMock.replay(row);
      return row;
   }
}

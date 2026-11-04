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

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.datastax.oss.driver.api.core.DriverTimeoutException;
import com.datastax.oss.driver.api.core.servererrors.InvalidQueryException;
import com.iris.core.dao.PasskeyDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.messages.model.Person;
import com.iris.security.crypto.AES;

/**
 * Every path that deletes a person (removal from their last place, DeleteLogin, closing
 * the account) goes through PersonDAO.delete, so that's where their passkeys go too.
 * Those paths remove the person's grants first, so the passkeys must never stop the
 * person from being deleted.
 */
public class PersonDAOImplTest {
   private RecordingCqlSession cassandra;
   private PasskeyDAO passkeyDao;
   private PersonDAOImpl dao;
   private Person person;

   @Before
   public void setUp() {
      cassandra = new RecordingCqlSession();
      passkeyDao = EasyMock.createMock(PasskeyDAO.class);
      dao = new PersonDAOImpl(cassandra.session, EasyMock.createNiceMock(AES.class), EasyMock.createNiceMock(PersonPlaceAssocDAO.class), passkeyDao);

      person = new Person();
      person.setId(UUID.randomUUID());
      person.setEmail("person@example.com");
      person.setHasLogin(true);
   }

   @Test
   public void testDeletingAPersonDeletesTheirPasskeys() {
      passkeyDao.deleteForPerson(person.getId());
      EasyMock.expectLastCall().andAnswer(() -> {
         assertTrue("passkeys deleted before the person", personDeleted());
         return null;
      });
      EasyMock.replay(passkeyDao);

      dao.delete(person);

      EasyMock.verify(passkeyDao);
   }

   @Test
   public void testPeopleCanBeDeletedWithoutThePasskeyTables() {
      passkeyDao.deleteForPerson(person.getId());
      EasyMock.expectLastCall().andThrow(new InvalidQueryException(null, "unconfigured table passkey"));
      EasyMock.replay(passkeyDao);

      dao.delete(person);

      EasyMock.verify(passkeyDao);
      assertTrue(cassandra.executed.toString(), personDeleted());
   }

   @Test
   public void testOtherPasskeyFailuresDontStopTheDelete() {
      passkeyDao.deleteForPerson(person.getId());
      EasyMock.expectLastCall().andThrow(new DriverTimeoutException("Query timed out after PT12S"));
      EasyMock.replay(passkeyDao);

      dao.delete(person);

      EasyMock.verify(passkeyDao);
      assertTrue(cassandra.executed.toString(), personDeleted());
   }

   @Test
   public void testPasskeysAreDeletedWhateverTheLoginFlagSays() {
      // registration and login check the flag, but an old row could disagree with it
      person.setHasLogin(false);
      passkeyDao.deleteForPerson(person.getId());
      EasyMock.expectLastCall();
      EasyMock.replay(passkeyDao);

      dao.delete(person);

      EasyMock.verify(passkeyDao);
      assertTrue(cassandra.executed.toString(), personDeleted());
   }

   private boolean personDeleted() {
      return cassandra.executed.stream()
            .flatMap(List::stream)
            .anyMatch(bound -> bound.query.startsWith("DELETE FROM person ") && bound.values.contains(person.getId()));
   }
}

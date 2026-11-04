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
package com.iris.client.server.passkey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.iris.client.server.passkey.PasskeyService.PasskeyException;
import com.iris.core.dao.PersonDAO;
import com.iris.messages.model.Person;
import com.iris.security.passkey.PasskeyToken;
import com.iris.security.principal.DefaultPrincipalResolver;
import com.iris.security.principal.Principal;

public class PasskeyRealmTest {
   private PasskeyService passkeyService;
   private PersonDAO personDao;
   private PasskeyRealm realm;
   private Person person;

   @Before
   public void setUp() {
      passkeyService = EasyMock.createMock(PasskeyService.class);
      personDao = EasyMock.createMock(PersonDAO.class);
      realm = new PasskeyRealm(passkeyService, personDao, new DefaultPrincipalResolver());

      person = new Person();
      person.setId(UUID.randomUUID());
      person.setEmail("person@example.com");
      person.setHasLogin(true);
   }

   @Test
   public void testSupportsOnlyPasskeyTokens() {
      assertTrue(realm.supports(new PasskeyToken("request", "{}")));
      assertFalse(realm.supports(new UsernamePasswordToken("person@example.com", "password")));
   }

   @Test
   public void testVerifiedPasskeyLogsInItsOwner() throws Exception {
      EasyMock.expect(passkeyService.finishLogin("request", "{}")).andReturn(person.getId());
      EasyMock.expect(personDao.findById(person.getId())).andReturn(person);
      EasyMock.replay(passkeyService, personDao);

      AuthenticationInfo info = realm.getAuthenticationInfo(new PasskeyToken("request", "{}"));
      Principal principal = (Principal) info.getPrincipals().getPrimaryPrincipal();
      assertEquals(person.getId(), principal.getUserId());
      assertEquals(person.getEmail(), principal.getUsername());
      EasyMock.verify(passkeyService, personDao);
   }

   @Test
   public void testFailedVerificationIsRejected() throws Exception {
      EasyMock.expect(passkeyService.finishLogin("request", "{}")).andThrow(new PasskeyException("Passkey login failed"));
      EasyMock.replay(passkeyService, personDao);

      assertRejected();
   }

   @Test
   public void testPersonWithoutLoginIsRejected() throws Exception {
      person.setHasLogin(false);
      EasyMock.expect(passkeyService.finishLogin("request", "{}")).andReturn(person.getId());
      EasyMock.expect(personDao.findById(person.getId())).andReturn(person);
      EasyMock.replay(passkeyService, personDao);

      assertRejected();
   }

   @Test
   public void testRemovedPersonIsRejected() throws Exception {
      EasyMock.expect(passkeyService.finishLogin("request", "{}")).andReturn(person.getId());
      EasyMock.expect(personDao.findById(person.getId())).andReturn(null);
      EasyMock.replay(passkeyService, personDao);

      assertRejected();
   }

   private void assertRejected() {
      try {
         realm.getAuthenticationInfo(new PasskeyToken("request", "{}"));
         fail("passkey login should have been rejected");
      }
      catch (IncorrectCredentialsException expected) {
      }
   }
}

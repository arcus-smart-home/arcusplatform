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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.easymock.EasyMock;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iris.client.server.passkey.PasskeyService.PasskeyException;
import com.iris.client.server.passkey.PasskeyService.PendingRequest;
import com.iris.client.server.passkey.PasskeyService.TooManyPasskeysException;
import com.iris.core.dao.PersonDAO;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.core.notification.Notifications;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.NotificationCapability.NotifyRequest;
import com.iris.messages.model.Person;
import com.iris.security.passkey.Passkey;
import com.yubico.webauthn.data.ByteArray;

/**
 * Runs real WebAuthn ceremonies against a software authenticator, so the options
 * stored between steps, signatures and user handles are all checked for real.
 */
public class PasskeyServiceTest {
   private static final String RP_ID = "example.com";
   private static final String ORIGIN = "https://example.com";

   private InMemoryPasskeyDAO passkeyDao;
   private PersonDAO personDao;
   private InMemoryPlatformMessageBus bus;
   private PasskeyService service;
   private Person person;
   private SoftwareAuthenticator authenticator;

   @Before
   public void setUp() throws Exception {
      person = new Person();
      person.setId(UUID.randomUUID());
      person.setEmail("person@example.com");
      person.setFirstName("Pat");
      person.setLastName("Person");
      person.setHasLogin(true);

      personDao = EasyMock.createNiceMock(PersonDAO.class);
      EasyMock.expect(personDao.findByEmail(person.getEmail())).andReturn(person).anyTimes();
      EasyMock.expect(personDao.findById(person.getId())).andReturn(person).anyTimes();
      EasyMock.replay(personDao);

      passkeyDao = new InMemoryPasskeyDAO();
      bus = new InMemoryPlatformMessageBus();
      service = new PasskeyService(config(RP_ID, ""), passkeyDao, personDao, bus);
      authenticator = new SoftwareAuthenticator(ORIGIN);
   }

   @Test
   public void testRegisterThenLogin() throws Exception {
      Passkey passkey = register("Laptop");
      assertEquals(authenticator.getCredentialId(), passkey.getCredentialId());
      assertEquals("Laptop", passkey.getName());
      assertEquals(1, service.list(person.getId()).size());
      assertNotice(Notifications.PasskeyAdded.KEY, Notifications.PasskeyAdded.PARAM_NAME, "Laptop");

      PendingRequest login = service.startLogin();
      assertEquals(person.getId(), service.finishLogin(login.getRequestId(), authenticator.get(login.getOptions())));
      assertNull("logging in sent a notice", bus.poll());

      Passkey stored = passkeyDao.findByCredentialId(passkey.getCredentialId());
      assertEquals(1, stored.getSignCount());
      assertNotNull(stored.getLastUsed());
   }

   @Test
   public void testNoticeUsesTheStoredName() throws Exception {
      register("  Phone ");
      assertNotice(Notifications.PasskeyAdded.KEY, Notifications.PasskeyAdded.PARAM_NAME, "Phone");
   }

   @Test
   public void testTamperedSignatureIsRejected() throws Exception {
      register("Laptop");
      PendingRequest login = service.startLogin();
      assertLoginFails(login.getRequestId(), authenticator.get(login.getOptions(), ORIGIN, true));
   }

   @Test
   public void testWrongOriginIsRejected() throws Exception {
      register("Laptop");
      PendingRequest login = service.startLogin();
      assertLoginFails(login.getRequestId(), authenticator.get(login.getOptions(), "https://attacker.example", false));
   }

   @Test
   public void testLoginRequestIsSingleUse() throws Exception {
      register("Laptop");
      PendingRequest login = service.startLogin();
      String response = authenticator.get(login.getOptions());
      service.finishLogin(login.getRequestId(), response);
      assertLoginFails(login.getRequestId(), response);
   }

   @Test
   public void testUnknownPasskeyIsRejected() throws Exception {
      // the authenticator has a credential the server never registered
      register("Laptop");
      passkeyDao.passkeys.clear();
      PendingRequest login = service.startLogin();
      assertLoginFails(login.getRequestId(), authenticator.get(login.getOptions()));
   }

   @Test
   public void testRegistrationRequestBelongsToItsPerson() throws Exception {
      PendingRequest registration = service.startRegistration(person);
      String response = authenticator.create(registration.getOptions());
      Person other = new Person();
      other.setId(UUID.randomUUID());
      other.setEmail("other@example.com");
      try {
         service.finishRegistration(other, registration.getRequestId(), response, "Laptop");
         fail("registered a passkey from another person's request");
      }
      catch (PasskeyException expected) {
      }
      assertTrue(passkeyDao.passkeys.isEmpty());
      assertNull("a failed registration sent a notice", bus.poll());
   }

   @Test
   public void testRequestsCantCrossCeremonies() throws Exception {
      register("Laptop");
      PendingRequest registration = service.startRegistration(person);
      PendingRequest login = service.startLogin();
      // a registration request can't complete a login, and a login request can't register
      assertLoginFails(registration.getRequestId(), authenticator.get(login.getOptions()));
      try {
         service.finishRegistration(person, login.getRequestId(), authenticator.create(registration.getOptions()), "Phone");
         fail("registered with a login request");
      }
      catch (PasskeyException expected) {
      }
   }

   @Test
   public void testSamePasskeyCantBeRegisteredTwice() throws Exception {
      register("Laptop");
      assertNotice(Notifications.PasskeyAdded.KEY, Notifications.PasskeyAdded.PARAM_NAME, "Laptop");
      PendingRequest registration = service.startRegistration(person);
      try {
         service.finishRegistration(person, registration.getRequestId(), authenticator.create(registration.getOptions()), "Again");
         fail("registered the same credential twice");
      }
      catch (PasskeyException expected) {
      }
      assertNull("a failed registration sent a notice", bus.poll());
   }

   @Test
   public void testDeleteOnlyOwnPasskeys() throws Exception {
      Passkey passkey = register("Laptop");
      assertNotice(Notifications.PasskeyAdded.KEY, Notifications.PasskeyAdded.PARAM_NAME, "Laptop");
      assertFalse(service.delete(UUID.randomUUID(), passkey.getCredentialId()));
      assertEquals(1, passkeyDao.passkeys.size());
      assertNull("deleting someone else's passkey sent a notice", bus.poll());
      assertTrue(service.delete(person.getId(), passkey.getCredentialId()));
      assertTrue(passkeyDao.passkeys.isEmpty());
      assertNotice(Notifications.PasskeyRemoved.KEY, Notifications.PasskeyRemoved.PARAM_NAME, "Laptop");
      assertFalse(service.delete(person.getId(), passkey.getCredentialId()));
      assertNull("deleting a passkey twice sent a second notice", bus.poll());
   }

   @Test
   public void testLoginFailsIfThePasskeyIsRemovedMeanwhile() throws Exception {
      InMemoryPasskeyDAO racing = new InMemoryPasskeyDAO() {
         @Override
         public boolean updateUsage(UUID personId, String credentialId, long signCount, Instant lastUsed) {
            // the person deletes the passkey after the login looked it up
            delete(personId, credentialId);
            return super.updateUsage(personId, credentialId, signCount, lastUsed);
         }
      };
      service = new PasskeyService(config(RP_ID, ""), racing, personDao, bus);
      register("Laptop");
      PendingRequest login = service.startLogin();
      assertLoginFails(login.getRequestId(), authenticator.get(login.getOptions()));
      assertTrue(racing.passkeys.isEmpty());
   }

   @Test
   public void testPasskeyLimit() throws Exception {
      for (int i = 0; i < PasskeyService.MAX_PASSKEYS; i++) {
         store(i);
      }
      try {
         service.startRegistration(person);
         fail("started a registration past the limit");
      }
      catch (TooManyPasskeysException expected) {
      }

      // another person isn't affected
      Person other = new Person();
      other.setId(UUID.randomUUID());
      other.setEmail("other@example.com");
      other.setHasLogin(true);
      assertNotNull(service.startRegistration(other));
   }

   @Test
   public void testPasskeyLimitIsCheckedAgainWhenFinishing() throws Exception {
      for (int i = 0; i < PasskeyService.MAX_PASSKEYS - 1; i++) {
         store(i);
      }
      PendingRequest registration = service.startRegistration(person);
      String response = authenticator.create(registration.getOptions());
      // another registration finished in the meantime
      store(PasskeyService.MAX_PASSKEYS);
      try {
         service.finishRegistration(person, registration.getRequestId(), response, "Laptop");
         fail("registered a passkey past the limit");
      }
      catch (TooManyPasskeysException expected) {
      }
      assertEquals(PasskeyService.MAX_PASSKEYS, service.list(person.getId()).size());
      assertNull("a refused registration sent a notice", bus.poll());
   }

   @Test
   public void testOptionsTellTheBrowserToGiveUpBeforeTheRequestExpires() throws Exception {
      // the 300 second default, less 30 seconds for the browser starting later
      assertEquals(270_000, timeout(service.startLogin()));
      assertEquals(270_000, timeout(service.startRegistration(person)));
   }

   @Test
   public void testRegistrationFailsIfTheRowIsAlreadyStored() throws Exception {
      InMemoryPasskeyDAO stored = new InMemoryPasskeyDAO() {
         @Override
         public boolean save(Passkey passkey) {
            // an earlier save stored the row but not its lookup row
            return false;
         }
      };
      service = new PasskeyService(config(RP_ID, ""), stored, personDao, bus);
      PendingRequest registration = service.startRegistration(person);
      try {
         service.finishRegistration(person, registration.getRequestId(), authenticator.create(registration.getOptions()), "Laptop");
         fail("reported a passkey that wasn't stored");
      }
      catch (PasskeyException expected) {
      }
      assertNull("a registration that stored nothing sent a notice", bus.poll());
   }

   @Test
   public void testDeletingTwiceAtOnceSendsOneNotice() throws Exception {
      InMemoryPasskeyDAO racing = new InMemoryPasskeyDAO() {
         @Override
         public boolean delete(UUID personId, String credentialId) {
            // another request deletes it after this one looked it up
            super.delete(personId, credentialId);
            return super.delete(personId, credentialId);
         }
      };
      service = new PasskeyService(config(RP_ID, ""), racing, personDao, bus);
      Passkey passkey = register("Laptop");
      assertNotice(Notifications.PasskeyAdded.KEY, Notifications.PasskeyAdded.PARAM_NAME, "Laptop");

      assertFalse(service.delete(person.getId(), passkey.getCredentialId()));
      assertNull("the request that lost sent a notice too", bus.poll());
   }

   @Test
   public void testRequestTtlIsKeptInRange() throws Exception {
      PasskeyConfig config = config(RP_ID, "");
      config.setRequestTtlSecs(86400);
      assertEquals(3600, config.getRequestTtlSecs());
      config.setRequestTtlSecs(-5);
      assertEquals(1, config.getRequestTtlSecs());
      config.setRequestTtlSecs(0);
      assertEquals(1, config.getRequestTtlSecs());
      // WebAuthn timeouts must be positive, so 0 would otherwise break every request
      assertEquals(900, timeout(new PasskeyService(config, passkeyDao, personDao, bus).startLogin()));
   }

   @Test
   public void testDisabledWithoutRpId() throws Exception {
      PasskeyService disabled = new PasskeyService(config("", ""), passkeyDao, personDao, bus);
      assertFalse(disabled.isEnabled());
      try {
         disabled.startLogin();
         fail("started a login with passkeys disabled");
      }
      catch (PasskeyException expected) {
      }
   }

   @Test
   public void testNormalizeName() {
      assertEquals("Passkey", PasskeyService.normalizeName(null));
      assertEquals("Passkey", PasskeyService.normalizeName(" \n\t "));
      assertEquals("Phone", PasskeyService.normalizeName(" Phone "));
      // line breaks and runs of spaces become one space, so a name never starts a new
      // line in the emails
      assertEquals("Work Phone", PasskeyService.normalizeName("Work\nPhone"));
      assertEquals("Work Phone", PasskeyService.normalizeName("Work\t\r\n Phone"));
      assertEquals("Work Phone", PasskeyService.normalizeName("Work\u2028Phone"));
      // as do control characters and every kind of space
      assertEquals("Work Phone", PasskeyService.normalizeName("Work\u0000\u00A0\u3000Phone"));
      // formatting characters are drawn as nothing, so they're dropped: what reverses the
      // text after it, zero-width spaces, soft hyphens, byte order marks, tag text
      assertEquals("Phonegpj.exe", PasskeyService.normalizeName("Phone\u202Egpj.exe"));
      assertEquals("Phone", PasskeyService.normalizeName("Ph\u200Bo\u00ADne\uFEFF"));
      assertEquals("Laptop", PasskeyService.normalizeName("Laptop" + text(0xE0068, 0xE0069, 0xE007F)));
      assertEquals("Passkey", PasskeyService.normalizeName("\u202E\u200B\u2066"));
      // except the joiners that emoji and scripts such as Persian are made with
      String family = text(0x1F468, 0x200D, 0x1F469, 0x200D, 0x1F467);
      assertEquals(family + " Phone", PasskeyService.normalizeName(family + " Phone"));
      String persian = text(0x0645, 0x06CC, 0x200C, 0x062E, 0x0648, 0x0627, 0x0647, 0x0645);
      assertEquals(persian, PasskeyService.normalizeName(persian));
      // half of a character, from malformed input, is dropped
      assertEquals("Phone", PasskeyService.normalizeName("Phone\uD83D"));
      // the limit counts code points, not UTF-16 units, and a cut never leaves a space
      assertEquals(PasskeyService.MAX_NAME_LENGTH, PasskeyService.normalizeName(StringUtils.repeat('x', 100)).length());
      String x63 = StringUtils.repeat('x', 63);
      String grin = text(0x1F600);
      assertEquals(x63 + grin, PasskeyService.normalizeName(x63 + grin + grin));
      assertEquals(x63, PasskeyService.normalizeName(x63 + " yy"));
   }

   @Test
   public void testNormalizeNameKeepsEmojiWhole() {
      // extended grapheme clusters arrived in BreakIterator with JDK 20
      Assume.assumeTrue(Runtime.version().feature() >= 20);
      String x63 = StringUtils.repeat('x', 63);
      assertEquals(x63, PasskeyService.normalizeName(x63 + text(0x1F44D, 0x1F3FD)));
      assertEquals(x63, PasskeyService.normalizeName(x63 + text(0x1F468, 0x200D, 0x1F469, 0x200D, 0x1F467)));
   }

   @Test
   public void testListShowsStoredNamesTheWayEmailsDo() throws Exception {
      Passkey old = new Passkey();
      old.setPersonId(person.getId());
      old.setCredentialId(new ByteArray(new byte[] { 9 }).getBase64Url());
      old.setName("Work\nPhone");
      passkeyDao.save(old);
      assertEquals("Work Phone", service.list(person.getId()).get(0).getName());
   }

   @Test
   public void testOnlyPeopleWithALoginCanRegister() throws Exception {
      // PasskeyRealm refuses them at login, so a passkey would never work
      person.setHasLogin(false);
      try {
         service.startRegistration(person);
         fail("started a registration for a person without a login");
      }
      catch (PasskeyException expected) {
      }
   }

   @Test
   public void testUserHandleRoundTrip() {
      UUID id = UUID.randomUUID();
      assertEquals(id, PasskeyCredentialRepository.personId(PasskeyCredentialRepository.userHandle(id)).get());
   }

   private Passkey register(String name) throws Exception {
      PendingRequest registration = service.startRegistration(person);
      return service.finishRegistration(person, registration.getRequestId(), authenticator.create(registration.getOptions()), name);
   }

   private void assertLoginFails(String requestId, String response) {
      try {
         service.finishLogin(requestId, response);
         fail("login should have failed");
      }
      catch (PasskeyException expected) {
      }
   }

   /** Stores a passkey for the person without a ceremony. */
   private void store(int i) {
      Passkey passkey = new Passkey();
      passkey.setPersonId(person.getId());
      passkey.setCredentialId(new ByteArray(new byte[] { 1, (byte) i }).getBase64Url());
      passkey.setUserHandle(PasskeyCredentialRepository.userHandle(person.getId()).getBase64Url());
      passkey.setName("Stored " + i);
      passkeyDao.save(passkey);
   }

   private static long timeout(PendingRequest request) throws Exception {
      return new ObjectMapper().readTree(request.getOptions()).get("publicKey").get("timeout").asLong();
   }

   private static String text(int... codePoints) {
      return new String(codePoints, 0, codePoints.length);
   }

   /** The next message on the bus is an email notice to the person. */
   private void assertNotice(String key, String nameParam, String passkeyName) {
      PlatformMessage message = bus.poll();
      assertNotNull("no notice sent", message);
      MessageBody body = message.getValue();
      assertEquals(NotifyRequest.NAME, body.getMessageType());
      assertEquals(person.getId().toString(), NotifyRequest.getPersonId(body));
      assertEquals(key, NotifyRequest.getMsgKey(body));
      assertEquals(NotifyRequest.PRIORITY_LOW, NotifyRequest.getPriority(body));
      assertEquals(passkeyName, NotifyRequest.getMsgParams(body).get(nameParam));
   }

   private static PasskeyConfig config(String rpId, String origins) {
      PasskeyConfig config = new PasskeyConfig();
      config.setRpId(rpId);
      config.setOrigins(origins);
      return config;
   }
}

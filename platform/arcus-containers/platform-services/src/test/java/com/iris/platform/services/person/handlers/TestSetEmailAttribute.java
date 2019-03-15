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
package com.iris.platform.services.person.handlers;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.iris.capability.attribute.transform.AttributeMapTransformModule;
import com.iris.capability.registry.CapabilityRegistryModule;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Person;
import com.iris.platform.services.BillingTestCase;
import com.iris.security.Login;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Ignore
@Mocks({PersonPlaceAssocDAO.class})
@Modules({InMemoryMessageModule.class, AttributeMapTransformModule.class, CapabilityRegistryModule.class})
public class TestSetEmailAttribute extends BillingTestCase {
   private static final String NEW_EMAIL = "new@email.com";
   private static final String NEW_FIRSTNAME = "Jane";
   private static final String NEW_LASTNAME = "Cranston";
   private static final String COR_ID = "e73d8787-e62e-40fa-96d0-e08d2f15aeb9";
   
   @Inject
   private PersonSetAttributesHandler handler;
   @Inject
   private PersonPlaceAssocDAO personPlaceAssocDao;
   
   @Override
   public void setUp() throws Exception {
      super.setUp();
      initData();
   }
   
   @Test
   public void testUpdateEmailAddress() throws Exception {
      // Test the scenario where only the email is changed.
      
      // Have the handler handle the message.
      handleMsg(person, NEW_EMAIL, null, null);
      // Check to make sure the person has changed.
      verifyPerson(person.getId(), NEW_EMAIL, FakePerson.FIRSTNAME, FakePerson.LASTNAME);
      // Check to make sure the login has changed.
      //verifyLogin(NEW_EMAIL);
      // Check to make sure the billing information has changed.
      verifyBilling(NEW_EMAIL);
      // Check to make sure notifications were sent.
      verifyNotification(platformBus.take(), NotificationCapability.NotifyRequest.PRIORITY_LOW);
      verifyNotification(platformBus.take(), NotificationCapability.NotifyRequest.PRIORITY_MEDIUM);
      verifyValueChangeEvent(platformBus.take(), person.getAddress());
      verifyNoMoreMsgs();
   }
   
   @Test
   public void testUpdateEmailAddressUnchanged() throws Exception {
      // Test the case where the email is set to the same value.
      handleMsg(person, FakePerson.EMAIL, null, null);
      // Nothing should have been called.
      Assert.assertEquals("No calls should have been made to PersonDAO", 0, ((Faker)personDao).numberOfCalls());
      Assert.assertEquals("No calls should have been made to Billing Client", 0, ((Faker)client).numberOfCalls());
      // No notifications should have been sent.
      Assert.assertNull("No messaages should have been sent", platformBus.poll());
   }
   
   @Test 
   public void testUpdatePersonButNotEmail() throws Exception {
      // Test the case where attributes other than email (and any other attributes that invoke notifications) are set.
      handleMsg(person, null, NEW_FIRSTNAME, NEW_LASTNAME);
      
      // The only call should have been to save the person.
      Assert.assertEquals("One call should have been made to PersonDAO", 1, ((Faker)personDao).numberOfCalls());
      Assert.assertEquals("The one call to PersonDAO should have been 'save'", "save", ((Faker)personDao).getCalls().get(0));
      Assert.assertEquals("No calls should have been made to Billing Client", 0, ((Faker)client).numberOfCalls());
      // No notifications should have been sent.
      verifyValueChangeEvent(platformBus.take(), person.getAddress());
      verifyNoMoreMsgs();
      
      //Verify the person record has been changed.
      verifyPerson(person.getId(), FakePerson.EMAIL, NEW_FIRSTNAME, NEW_LASTNAME);
   }
   
   @Test
   public void testUpdatePersonAndEmail() throws Exception {
	  person.setEmailVerificationToken("token123");
	  person.setEmailVerified(new Date());
      //Capture<Person> personRef = EasyMock.newCapture();
      //EasyMock.expect(EasyMock.capture(personRef)).andAnswer(() -> personRef.getValue());
      EasyMock.expect(personDao.updatePersonAndEmail(person, person.getEmail())).andReturn(person);
      EasyMock.expect(personPlaceAssocDao.findPlaceIdsByPerson(person.getId())).andReturn(ImmutableSet.<UUID>of(firstPlace.getId()));
      personDao.setUpdateFlag(person.getId(), true);
      EasyMock.expectLastCall().anyTimes();
      personDao.setUpdateFlag(person.getId(), false);
      EasyMock.expectLastCall().anyTimes();
      replay();
      
      // Have the handler handle the message.
      handleMsg(person, NEW_EMAIL, NEW_FIRSTNAME, NEW_LASTNAME);
      // Check to make sure the person has changed.
      verifyPerson(person.getId(), NEW_EMAIL, NEW_FIRSTNAME, NEW_LASTNAME);
      
      //verify emailVerification related attributes are cleared
      assertNull(person.getEmailVerificationToken());
      assertNull(person.getEmailVerified());
      // Check to make sure the login has changed.
      //verifyLogin(NEW_EMAIL);
      // Check to make sure the billing information has changed.
      verifyBilling(NEW_EMAIL);
      // Check to make sure notifications were sent.
      verifyNotification(platformBus.take(), NotificationCapability.NotifyRequest.PRIORITY_LOW);
      verifyNotification(platformBus.take(), NotificationCapability.NotifyRequest.PRIORITY_MEDIUM);
      verifyValueChangeEvent(platformBus.take(), person.getAddress());
      verifyNoMoreMsgs();
   }
   
   private void verifyNotification(PlatformMessage msg, String priority) {
      Assert.assertNotNull(msg);
      Assert.assertEquals(Addresses.NOTIFICATION, msg.getDestination().getRepresentation());
      Assert.assertEquals(Addresses.PERSON, msg.getSource().getRepresentation());
      
      MessageBody body = msg.getValue();
      Assert.assertNotNull(msg);
      Assert.assertEquals(body.getMessageType(), NotificationCapability.NotifyRequest.NAME);
      Assert.assertEquals(person.getId().toString(), NotificationCapability.NotifyRequest.getPersonId(body));
      Assert.assertEquals("email.changed", NotificationCapability.NotifyRequest.getMsgKey(body));
      Assert.assertEquals(priority, NotificationCapability.NotifyRequest.getPriority(body));
      Map<String, String> params = NotificationCapability.NotifyRequest.getMsgParams(body);
      Assert.assertEquals(FakePerson.EMAIL, params.get("oldemail"));
   }
   
   private void verifyBilling(String newEmail) {
      com.iris.billing.client.model.Account account = billingAccount.getAccount();
      Assert.assertEquals("Email for billing account should have changed", newEmail, account.getEmail());
   }
   
   private void verifyLogin(String newEmail) {
      // Not much of a test as a lot of this logic is in the DAO which is being faked.
      //Login oldLogin = personDao.findLogin(FakePerson.EMAIL);
      //Assert.assertNull("The old login should no longer exist.", oldLogin);
      Login newLogin = personDao.findLogin(newEmail);
      Assert.assertNotNull("The new login should exist.", newLogin);
      Assert.assertEquals("The password should be the same", FakeLogin.PASSWORD, newLogin.getPassword());
      Assert.assertEquals("The password salt is the same", FakeLogin.PASSWORD_SALT, newLogin.getPasswordSalt());
   }
   
   private void verifyPerson(UUID personId, String newEmail, String firstName, String lastName) {
      Person checkPerson = personDao.findById(person.getId());
      Assert.assertEquals(newEmail, checkPerson.getEmail());
      Assert.assertEquals(checkPerson.getFirstName(), firstName);
      Assert.assertEquals(checkPerson.getLastName(), lastName);
   }
   
   private void handleMsg(Person person, String newEmail, String newFirstName, String newLastName) {
      Map<String, Object> attrs = new HashMap<>();
      if (newEmail != null) {
         attrs.put(PersonCapability.ATTR_EMAIL, newEmail);
      }
      if (newFirstName != null) {
         attrs.put(PersonCapability.ATTR_FIRSTNAME, newFirstName);
      }
      if (newLastName != null) {
         attrs.put(PersonCapability.ATTR_LASTNAME, newLastName);
      }
      MessageBody request = MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, attrs);
      
      PlatformMessage msg = PlatformMessage.create(request, 
            Fixtures.createClientAddress(), 
            Address.fromString(person.getAddress()), 
            COR_ID);
      
      handler.handleRequest(person, msg);
   }
   
}


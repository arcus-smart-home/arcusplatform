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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.iris.capability.attribute.transform.AttributeMapTransformModule;
import com.iris.capability.registry.CapabilityRegistryModule;
import com.iris.capability.util.PhoneNumbers;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.notification.Notifications;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Person;
import com.iris.messages.model.Account.AccountState;
import com.iris.platform.services.BillingTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Mocks({PersonPlaceAssocDAO.class})
@Modules({InMemoryMessageModule.class, AttributeMapTransformModule.class, CapabilityRegistryModule.class})
public class TestSetAttributes extends BillingTestCase {
   private static final String NEW_MOBILE_NUMBER = "4815162342";
   private static final String NEW_FIRSTNAME = "Jane";
   private static final String NEW_LASTNAME = "Cranston";
   private static final String COR_ID = "e73d8787-e62e-40fa-96d0-e08d2f15aeb9";

   @Inject
   private PersonPlaceAssocDAO personPlaceAssocDao;

   
   
   @Inject
   private PersonSetAttributesHandler handler;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      initData();
   }

   @Test
   public void testUpdateMobileNumber() throws Exception {
      UUID secondPlaceId = UUID.randomUUID();
      EasyMock.expect(personDao.update(person)).andReturn(person);
      EasyMock.expect(personPlaceAssocDao.findPlaceIdsByPerson(person.getId())).andReturn(ImmutableSet.<UUID>of(firstPlace.getId(), secondPlaceId));
      replay();
      handleMsg(person, NEW_MOBILE_NUMBER, null, null);

      verifyPerson(person.getId(), NEW_MOBILE_NUMBER, FakePerson.FIRSTNAME, FakePerson.LASTNAME);
      verifyMobileNumberNotification(platformBus.take(), person.getId());
      verifyValueChangeEvent(platformBus.take(), person.getAddress(), firstPlace.getId().toString());
      verifyValueChangeEvent(platformBus.take(), person.getAddress(), secondPlaceId.toString());  //two value change event should be sent.
      verifyNoMoreMsgs();
      verify();
   }

   @Test
   public void testUpdateOtherAttributes() throws Exception {
      EasyMock.expect(personDao.update(person)).andReturn(person);
      EasyMock.expect(personPlaceAssocDao.findPlaceIdsByPerson(person.getId())).andReturn(ImmutableSet.<UUID>of(firstPlace.getId()));
      replay();
     
      handleMsg(person, null, NEW_FIRSTNAME, NEW_LASTNAME);
      verifyPerson(person.getId(), FakePerson.MOBILENUMBER, NEW_FIRSTNAME, NEW_LASTNAME);
      // Should be no notifications.
      verifyValueChangeEvent(platformBus.take(), person.getAddress(), firstPlace.getId().toString());
      verifyNoMoreMsgs();
      
      verify();
   }

   @Test
   public void testUpdateMobileNumberAndOthers() throws Exception {
      EasyMock.expect(personDao.update(person)).andReturn(person);
      EasyMock.expect(personPlaceAssocDao.findPlaceIdsByPerson(person.getId())).andReturn(ImmutableSet.<UUID>of(firstPlace.getId()));
      replay();

      handleMsg(person, NEW_MOBILE_NUMBER, NEW_FIRSTNAME, NEW_LASTNAME);
      verifyPerson(person.getId(), NEW_MOBILE_NUMBER, NEW_FIRSTNAME, NEW_LASTNAME);
      verifyMobileNumberNotification(platformBus.take(), person.getId());
      verifyValueChangeEvent(platformBus.take(), person.getAddress(), firstPlace.getId().toString());
      verifyNoMoreMsgs();
      Assert.assertNull(platformBus.poll());
      
      verify();
   }

   @Test
   public void testUpdatePinAndMobileWithUnchangedData() throws Exception {
      replay();
      
      handleMsg(person, FakePerson.MOBILENUMBER, null, null);
      verifyPerson(person.getId(), FakePerson.MOBILENUMBER, FakePerson.FIRSTNAME, FakePerson.LASTNAME);
      Assert.assertNull(platformBus.poll());
      
      verify();
   }
   
   
   @Test
   public void testUpdateEmail() throws Exception {
   	account.setState(AccountState.COMPLETE);
   	String newEmail = "test111@gmail.com";
   	setupUpdateEmail(newEmail);
   	replay();
   	
   	Map<String, Object> attrs = new HashMap<>();
   	attrs.put(PersonCapability.ATTR_EMAIL, newEmail);
   	createAndSendRequest(person, attrs);
   	
   	verifyEmailUpdateNotification(platformBus.take(), person.getId(), true);	//email
   	verifyEmailUpdateNotification(platformBus.take(), person.getId(), false);	//text
      verifyValueChangeEvent(platformBus.take(), person.getAddress(), firstPlace.getId().toString());
   	
   }
   
   @Test
   public void testUpdateEmailAccountNotComplete() throws Exception {
   	account.setState(AccountState.SIGN_UP_1);
   	String newEmail = "test111@gmail.com";
   	setupUpdateEmail(newEmail);
   	replay();
   	
   	Map<String, Object> attrs = new HashMap<>();
   	attrs.put(PersonCapability.ATTR_EMAIL, newEmail);
   	createAndSendRequest(person, attrs);
   	//No email notification
      verifyValueChangeEvent(platformBus.take(), person.getAddress(), firstPlace.getId().toString());
   }

   private void setupUpdateEmail(String newEmail) {

   	personDao.setUpdateFlag(person.getId(), true);
   	EasyMock.expectLastCall();
   	EasyMock.expect(personDao.updatePersonAndEmail(person, person.getEmail())).andAnswer(new IAnswer<Person>() {

			@Override
			public Person answer() throws Throwable {
				Person newPerson = person.copy();
				newPerson.setEmail(newEmail);
				return newPerson;
			}
		});

   	personDao.setUpdateFlag(person.getId(), false);
   	EasyMock.expectLastCall();
   	EasyMock.expect(personPlaceAssocDao.findPlaceIdsByPerson(person.getId())).andReturn(ImmutableSet.<UUID>of(firstPlace.getId()));		
	}

	private void verifyPerson(UUID personId, String newMobileNumber, String firstName, String lastName) {
      Person checkPerson = personDao.findById(person.getId());
      Assert.assertEquals(PhoneNumbers.fromString(newMobileNumber), PhoneNumbers.fromString(checkPerson.getMobileNumber()));
      Assert.assertEquals(firstName, checkPerson.getFirstName());
      Assert.assertEquals(lastName, checkPerson.getLastName());
   }

   private void handleMsg(Person person, String newNumber, String newFirstName, String newLastName) {
      Map<String, Object> attrs = new HashMap<>();
      if (newNumber != null) {
         attrs.put(PersonCapability.ATTR_MOBILENUMBER, newNumber);
      }
      if (newFirstName != null) {
         attrs.put(PersonCapability.ATTR_FIRSTNAME, newFirstName);
      }
      if (newLastName != null) {
         attrs.put(PersonCapability.ATTR_LASTNAME, newLastName);
      }
      
      createAndSendRequest(person, attrs);
   }
   
   private void createAndSendRequest(Person person, Map<String, Object> attrs) {
   	MessageBody request = MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, attrs);

      PlatformMessage msg = PlatformMessage.create(request,
            Fixtures.createClientAddress(),
            Address.fromString(person.getAddress()),
            COR_ID);

      handler.handleRequest(person, msg);
   }

   private void verifyMobileNumberNotification(PlatformMessage msg, UUID personId) {

      MessageBody body = assertNotification(msg, personId, true);
      
      Assert.assertEquals(Notifications.MobileNumberChanged.KEY, NotificationCapability.NotifyRequest.getMsgKey(body));
      Map<String, String> params = NotificationCapability.NotifyRequest.getMsgParams(body);
      Assert.assertEquals(FakePerson.MOBILENUMBER, params.get(Notifications.MobileNumberChanged.PARAM_OLDMOBILENUMBER));
   }
   
   private void verifyEmailUpdateNotification(PlatformMessage msg, UUID personId, boolean isEmail) {
   	MessageBody body = assertNotification(msg, personId, isEmail);
   	Assert.assertEquals(Notifications.EmailChanged.KEY, NotificationCapability.NotifyRequest.getMsgKey(body));
      Map<String, String> params = NotificationCapability.NotifyRequest.getMsgParams(body);
      Assert.assertEquals(FakePerson.EMAIL, params.get(Notifications.EmailChanged.PARAM_OLD_EMAIL));
   }
   
   private MessageBody assertNotification(PlatformMessage msg, UUID personId, boolean isEmail) {
   	Assert.assertNotNull(msg);
      Assert.assertEquals(Addresses.NOTIFICATION, msg.getDestination().getRepresentation());
      Assert.assertEquals(Addresses.PERSON, msg.getSource().getRepresentation());

      MessageBody body = msg.getValue();
      Assert.assertNotNull(msg);
      Assert.assertEquals(body.getMessageType(), NotificationCapability.NotifyRequest.NAME);
      Assert.assertEquals(personId.toString(), NotificationCapability.NotifyRequest.getPersonId(body));
      if(isEmail) {
         Assert.assertEquals(NotificationCapability.NotifyRequest.PRIORITY_LOW, NotificationCapability.NotifyRequest.getPriority(body));      	
      }else{
         Assert.assertEquals(NotificationCapability.NotifyRequest.PRIORITY_MEDIUM, NotificationCapability.NotifyRequest.getPriority(body));      	
      	
      }
      return body;
   }
}


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
package com.iris.core.platform.handlers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.iris.capability.attribute.transform.AttributeMapTransformModule;
import com.iris.capability.registry.CapabilityRegistryModule;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.AuthorizationGrantDAO;
import com.iris.core.dao.EmptyResourceBundle;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.ResourceBundleDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.PlatformServiceAddress;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.NotificationCapability.NotifyRequest;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.model.Account;
import com.iris.messages.model.Account.AccountState;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.service.AccountService;
import com.iris.messages.service.AccountService.CreateAccountRequest;
import com.iris.messages.service.AccountService.CreateAccountRequest.Builder;
import com.iris.platform.location.LocationServiceModule;
import com.iris.platform.location.TimezonesModule;
import com.iris.security.authz.AuthorizationGrant;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@RunWith(Parameterized.class)
@Mocks({AccountDAO.class, PlatformMessageBus.class, PersonDAO.class, PlaceDAO.class, AuthorizationGrantDAO.class})
@Modules({ CapabilityRegistryModule.class, AttributeMapTransformModule.class, TimezonesModule.class, LocationServiceModule.class } )
public class TestCreateAccountHandler extends IrisMockTestCase {

   private static final Address clientAddress = Address.clientAddress("test", "test");

   @Inject 
   private AccountDAO accountDaoMock;
   @Inject 
   private PersonDAO personDao;
   @Inject 
   private PlaceDAO placeDao;
   @Inject
   private AuthorizationGrantDAO authDao;
   @Inject 
   private PlatformMessageBus bus;
   @Inject
   private CreateAccountHandler handler;
   
   private String email;
   private String password;   
   private boolean optin;
   private boolean isPubic;
   private Map<String, Object> currentPerson;
   private Map<String, Object> currentPlace;
   private boolean isError;
   
   private static enum ModelName {
      Null,
      Person1,
      Person2,
      Person3,
      Place1,
      Place2,
      Place3,
      Place4
   }
   
   
   
   private static Account account1 = createAccount(AccountState.SIGN_UP_1);
   private static Map<String, Object> person1 = createPersonAttributes("dan", "yao", "414-111-2222", null);
   private static Map<String, Object> person2 = createPersonAttributes("erika", "patrow", "312-111-2222", 
            ImmutableMap.<String, Object>of(PersonCapability.ATTR_HASPIN, true));
   private static Map<String, Object> person3 = createPersonAttributes("dan", "yao", "414-111-2222", ImmutableMap.<String, Object>of(Capability.ATTR_TAGS, ImmutableSet.<String>of("test1", "test2")));
   private static Map<String, Object> place1 = createPlaceAttributes("60025", "My Place", ImmutableMap.<String, Object>of(PlaceCapability.ATTR_TZID, "America/Chicago"));
   private static Map<String, Object> place2 = createPlaceAttributes("60601", "Patrow's Place", 
            ImmutableMap.<String, Object>of(PlaceCapability.ATTR_NAME, "Test Place",
               PlaceCapability.ATTR_ZIPCODE, "60610",
               PlaceCapability.ATTR_ZIPPLUS4, "1234",
               PlaceCapability.ATTR_TZID, "America/Chicago",
               PlaceCapability.ATTR_STATE, "IL"));
   private static Map<String, Object> place3 = createPlaceAttributes("60025", "Patrow's Place", ImmutableMap.<String, Object>of(
         PlaceCapability.ATTR_STREETADDRESS1, "3511 countryside ln",
         PlaceCapability.ATTR_CITY, "Glenview",
         PlaceCapability.ATTR_STATE, "IL"
         ));
   private static Map<String, Object> place4 = createPlaceAttributes("60601", "Patrow's Place", 
      ImmutableMap.<String, Object>of(PlaceCapability.ATTR_NAME, "Test Place",
         PlaceCapability.ATTR_ZIPCODE, "60610",
         PlaceCapability.ATTR_ZIPPLUS4, "1234",
         PlaceCapability.ATTR_TZID, "America/Chicago",
         Capability.ATTR_TAGS, ImmutableSet.<String>of("test1", "test2"))); //ITWO-13435 - accept a place with base:tags
   private static Map<ModelName, Map<String, Object>> modelMap = ImmutableMap.<ModelName, Map<String, Object>>builder()
      .put(ModelName.Person1, person1)
      .put(ModelName.Person2, person2)
      .put(ModelName.Person3, person3)
      .put(ModelName.Place1, place1)
      .put(ModelName.Place2, place2)
      .put(ModelName.Place3, place3)
      .put(ModelName.Place4, place4).build();


   @Parameters(name="email[{0}],password[{1}],optin[{2}],public[{3}],person[{4}],place[{5}], error[{6}]")
   public static List<Object []> files() {
      return Arrays.<Object[]>asList(
            new Object [] { "dyao1@gmail.com", "test",  true, true, ModelName.Person1, ModelName.Place1, false},
            new Object [] { "dyao1@gmail.com", "test",  true, true, ModelName.Person1, ModelName.Place2, false},
            new Object [] { "test1@gmail.com", "test",  true, true, ModelName.Null, ModelName.Null, false},
            new Object [] { "test2@gmail.com", "test",  true, true, ModelName.Person1, ModelName.Place2, false},
            new Object [] { "test3@gmail.com", "test",  true, true, ModelName.Person1, ModelName.Place3, true},
            new Object [] { "test3@gmail.com", "test",  true, true, ModelName.Person2, ModelName.Place1, true},
            new Object [] { "test2@gmail.com", "test",  true, true, ModelName.Person1, ModelName.Place4, false},
            new Object [] { "dyao1@gmail.com", "test",  true, true, ModelName.Person3, ModelName.Place1, false}
      );
   }
   
   public TestCreateAccountHandler(String email, String password, boolean optin, boolean isPubic, ModelName personName, ModelName placeName, boolean isError) {
      this.email = email;
      this.password = password;
      this.optin = optin;
      this.isPubic = isPubic;
      this.currentPerson = modelMap.get(personName);
      this.currentPlace = modelMap.get(placeName);
      this.isError = isError;
   }
   
   

   @Override
   protected void configure(Binder binder)
   {
      super.configure(binder);
      binder.bind(ResourceBundleDAO.class).to(EmptyResourceBundle.class);
   }
   
   private static Map<String, Object> createPersonAttributes(String firstName, String lastName, String mobilePhone, Map<String, Object> additionalAttributes) {
      Map<String, Object> attributes = new HashMap<String, Object>();
      attributes.put(PersonCapability.ATTR_FIRSTNAME, firstName);
      attributes.put(PersonCapability.ATTR_LASTNAME, lastName);
      //attributes.put(PersonCapability.ATTR_EMAIL, email);
      attributes.put(PersonCapability.ATTR_MOBILENUMBER, mobilePhone);
      if(additionalAttributes!= null && !additionalAttributes.isEmpty()) {
         attributes.putAll(additionalAttributes);
      }
      return attributes;
   }

   private static Map<String, Object> createPlaceAttributes(String zipcode, String name, Map<String, Object> additionalAttributes)
   {
      Map<String, Object> attributes = new HashMap<String, Object>();
      attributes.put(PlaceCapability.ATTR_ZIPCODE, zipcode);
      attributes.put(PlaceCapability.ATTR_NAME, name);
      if(additionalAttributes!= null && !additionalAttributes.isEmpty()) {
         attributes.putAll(additionalAttributes);
      }
      return attributes;
   }

   @Test
   public void testCreateAccount_1() {
      PlatformMessage request = createCreateAccountRequest(email, password, Boolean.toString(optin), Boolean.toString(isPubic), currentPerson, currentPlace);
      Capture<Person> personCaptured = Capture.newInstance();      
      Capture<String> passwordCaptured = Capture.newInstance();
      Capture<Account> accountCaptured = Capture.newInstance();  
      Capture<Place> placeCaptured = Capture.newInstance(); 
      if(!isError) {       
         EasyMock.expect(personDao.create(EasyMock.capture(personCaptured), EasyMock.capture(passwordCaptured))).andAnswer(new IAnswer<Person>()
         {
            @Override
            public Person answer() throws Throwable
            {
               return personCaptured.getValue();
            }
         });
         
         
         EasyMock.expect(accountDaoMock.create(EasyMock.capture(accountCaptured))).andAnswer(new IAnswer<Account>()
         {
            @Override
            public Account answer() throws Throwable
            {
               return accountCaptured.getValue();
            }
         });
         
          
         EasyMock.expect(placeDao.create(EasyMock.capture(placeCaptured))).andAnswer(new IAnswer<Place>()
         {
            @Override
            public Place answer() throws Throwable
            {
               return placeCaptured.getValue();
            }
         });
         
         authDao.save(EasyMock.anyObject(AuthorizationGrant.class));
         EasyMock.expectLastCall();
         
         Capture<PlatformMessage> msgCaptured = Capture.newInstance(CaptureType.ALL);
         EasyMock.expect(bus.send(EasyMock.capture(msgCaptured))).andAnswer(
            () -> {
               //assertNotificationSent(msgCaptured.getValue());
               return Futures.immediateFuture(null);
            }
         ).anyTimes();
      }
      
      replay();
      try{
         handler.handleStaticRequest(request); 
         
         Person personCreated = personCaptured.getValue();
         Account accountCreated = accountCaptured.getValue();
         Place placeCreated = placeCaptured.getValue();
         assertEquals(email, personCreated.getEmail());
         assertEquals(password, passwordCaptured.getValue());
         assertEquals(accountCreated.getId(), placeCreated.getAccount());
         assertEquals(personCreated.getId(), accountCreated.getOwner());
         assertEquals(ImmutableSet.<UUID>of(placeCreated.getId()), accountCreated.getPlaceIDs());
         assertEquals(ServiceLevel.BASIC, placeCreated.getServiceLevel());
         assertTrue(placeCreated.isPrimary());
         if(isError) {
            fail("Should have failed");
         }
         
         
      }catch(ErrorEventException e) {
         if(!isError) {
            e.printStackTrace();
            fail(e.getMessage());
         }
         //assertEquals(AccountActivateHandler.ERROR_INVALID_STATE, e.getCode());
      }
      verify();
   }
   
  /*
   @Test
   public void testActivateState() {
      Person owner = createPerson();
      Account account = createAccount(curAccountState);
      account.setOwner(owner.getId());
      owner.setAccountId(account.getId());
      Place place1 = createPlace(account.getId());
      Place place2 = createPlace(account.getId());
      account.setPlaceIDs(ImmutableSet.<UUID>of(place1.getId(), place2.getId()));
      
      EasyMock.expect(personDao.findById(owner.getId())).andReturn(owner).anyTimes();
      Capture<Account> accountCaptured = null;
      if(accountSaved) {
         accountCaptured = Capture.newInstance();
         EasyMock.expect(accountDaoMock.save(EasyMock.capture(accountCaptured))).andReturn(account);
      }
      final Capture<PlatformMessage> msgCaptured = Capture.newInstance(CaptureType.ALL);
      int expectedMessageSent = notificationSent?3:2;
      if(accountSaved) {
         EasyMock.expect(bus.send(EasyMock.capture(msgCaptured))).andAnswer(
            () -> {
               //assertNotificationSent(msgCaptured.getValue());
               return Futures.immediateFuture(null);
            }
         ).times(expectedMessageSent);
      }
      replay();
      
      handler.handleRequest(account, createCreateAccountRequest(account));      
      if(accountSaved) {
         Account accountSaved = accountCaptured.getValue();
         assertEquals(Account.AccountState.COMPLETE, accountSaved.getState());
      }
      int index = 0;
      if(notificationSent) {
         PlatformMessage msgSent = msgCaptured.getValues().get(0);
         index++;
         assertEquals(NotifyRequest.NAME, msgSent.getMessageType());
      }
      verify();
      
      if(accountSaved) {
         PlatformMessage valueChangeMsg1 = msgCaptured.getValues().get(index++);
         assertEquals(Capability.EVENT_VALUE_CHANGE, valueChangeMsg1.getMessageType());
         assertEquals(place1.getId().toString(), valueChangeMsg1.getPlaceId());
         
         PlatformMessage valueChangeMsg2 = msgCaptured.getValues().get(index++);
         assertEquals(Capability.EVENT_VALUE_CHANGE, valueChangeMsg2.getMessageType());
         assertEquals(place2.getId().toString(), valueChangeMsg2.getPlaceId());
      }
   }
   */
   
   private void assertNotificationSent(PlatformMessage value)
   {
      assertEquals(NotifyRequest.NAME, value.getMessageType());
      
   }


   private static Account createAccount(String state) {
      Account account = Fixtures.createAccount();
      account.setId(UUID.randomUUID());
      account.setOwner(UUID.randomUUID());
      account.setState(state);
      return account;
   }
   
   private PlatformMessage createCreateAccountRequest(String email, String password, String optin, String isPublic, Map<String, Object> owner, Map<String, Object> place) {
      Builder builder = CreateAccountRequest.builder()
            .withEmail(email)
            .withPassword(password)
            .withOptin(optin)
            .withIsPublic(isPublic);
      if(place != null) {
         builder.withPlace(place);
      }
      if(owner != null) {
         builder.withPerson(owner);
      }
           
      return PlatformMessage.buildMessage(builder.build(), clientAddress, PlatformServiceAddress.platformService(AccountService.NAMESPACE))
            .isRequestMessage(true)
            .withCorrelationId("correlationid")           
            .create();
   }
}


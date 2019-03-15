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
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.capability.attribute.transform.AttributeMapTransformModule;
import com.iris.core.dao.PersonDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Person;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Modules({InMemoryMessageModule.class, AttributeMapTransformModule.class})
@Mocks(PersonDAO.class)
public class TestAcceptPolicyHandler extends IrisMockTestCase {

   private static final Date ACCEPTANCE_DATE = new Date();
   private static final Date REJECTED_DATE = new Date(0);

   @Inject
   private PersonDAO mockPersonDao;
   
   // unit under test
   @Inject
   private RejectPolicyHandler handler;
   
   private Person person;
   private Address clientAddress;
   private Address personAddress;
   
   @Before
   public void stageFixtures() throws Exception {
      person = Fixtures.createPerson();
      person.setId(UUID.randomUUID());
      person.setTermsAgreed(ACCEPTANCE_DATE);
      person.setPrivacyPolicyAgreed(ACCEPTANCE_DATE);
      
      clientAddress = Fixtures.createClientAddress();
      personAddress = Address.fromString(person.getAddress());
   }

   @Test
   public void testRejectPolicyPrivacy() throws Exception {
      EasyMock
         .expect(mockPersonDao.update(person))
         .andReturn(person);
      replay();
      
      MessageBody request = 
            PersonCapability.RejectPolicyRequest.builder()
               .withType(PersonCapability.RejectPolicyRequest.TYPE_PRIVACY)
               .build();

      MessageBody response = handler.handleRequest(person, request(request));
      
      assertEquals(MessageBody.emptyMessage(), response);
      assertEquals(REJECTED_DATE, person.getPrivacyPolicyAgreed());
      assertEquals(ACCEPTANCE_DATE, person.getTermsAgreed());
      verify();
   }

   @Test
   public void testRejectPolicyTerms() throws Exception {
      EasyMock
         .expect(mockPersonDao.update(person))
         .andReturn(person);
      replay();
      
      MessageBody request = 
            PersonCapability.RejectPolicyRequest.builder()
               .withType(PersonCapability.RejectPolicyRequest.TYPE_TERMS)
               .build();

      MessageBody response = handler.handleRequest(person, request(request));
      
      assertEquals(MessageBody.emptyMessage(), response);
      assertEquals(ACCEPTANCE_DATE, person.getPrivacyPolicyAgreed());
      assertEquals(REJECTED_DATE, person.getTermsAgreed());
      verify();
   }
   
   @Test
   public void testMissingParam() throws Exception {
      replay();
      
      MessageBody request = 
            PersonCapability.RejectPolicyRequest.builder()
               .build();

      try {
         handler.handleRequest(person, request(request));
         fail();
      }
      catch(ErrorEventException e) {
         assertEquals(Errors.CODE_MISSING_PARAM, e.getCode());
      }
      
      verify();
   }
   
   @Test
   public void testWithMissingActor() throws Exception {
      replay();
      
      MessageBody request = 
            PersonCapability.RejectPolicyRequest.builder()
               .build();
      PlatformMessage message =
            PlatformMessage
               .buildRequest(request, clientAddress, personAddress)
               // no actor set
               .create();

      try {
         handler.handleRequest(person, message);
         fail();
      }
      catch(ErrorEventException e) {
         assertEquals(Errors.CODE_INVALID_REQUEST, e.getCode());
      }
      
      verify();
      
   }
   
   @Test
   public void testWithWrongActor() throws Exception {
      replay();
      
      MessageBody request = 
            PersonCapability.RejectPolicyRequest.builder()
               .build();
      PlatformMessage message =
            PlatformMessage
               .buildRequest(request, clientAddress, personAddress)
               .withActor(clientAddress)
               .create();

      try {
         handler.handleRequest(person, message);
         fail();
      }
      catch(ErrorEventException e) {
         assertEquals(Errors.CODE_INVALID_REQUEST, e.getCode());
      }
      
      verify();
      
   }
   
   @Test
   @Ignore // TODO not the most correct error code
   public void testInvalidParam() throws Exception {
      replay();
      
      MessageBody request = 
            PersonCapability.RejectPolicyRequest.builder()
               .withType("SomeBogusStuff")
               .build();

      try {
         handler.handleRequest(person, request(request));
         fail();
      }
      catch(ErrorEventException e) {
         assertEquals(Errors.CODE_INVALID_PARAM, e.getCode());
      }
      
      verify();
   }
   
   private PlatformMessage request(MessageBody request) {
      return 
            PlatformMessage
               .buildRequest(request, clientAddress, personAddress)
               .withActor(personAddress)
               .create();
   }
}


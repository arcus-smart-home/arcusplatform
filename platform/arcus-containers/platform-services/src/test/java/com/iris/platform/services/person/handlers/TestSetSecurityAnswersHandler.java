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

import static com.iris.platform.services.person.handlers.SetSecurityAnswersHandler.BLANK_ANSWER_CODE;
import static com.iris.platform.services.person.handlers.SetSecurityAnswersHandler.BLANK_QUESTION_CODE;
import static com.iris.platform.services.person.handlers.SetSecurityAnswersHandler.INVALID_QUESTION_CODE;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import java.util.ListResourceBundle;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.iris.core.dao.PersonDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PersonCapability.SetSecurityAnswersRequest;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Person;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

@Mocks({PersonDAO.class, PlatformMessageBus.class})
public class TestSetSecurityAnswersHandler extends IrisMockTestCase
{

   @Inject
   private PersonDAO mockPersonDao;
   @Inject
   private SetSecurityAnswersHandler componentUnderTest;
   
   @Inject
   private PlatformMessageBus bus;

   private Person person;
   private Address clientAddress;
   private Address personAddress;

   @Provides @Named(SetSecurityAnswersHandler.NAME_RESOURCE_BUNDLE)
   public ResourceBundle stubSecurityQuestionBundle() {
      return new ListResourceBundle()
      {
         @Override
         protected Object[][] getContents()
         {
            return new Object[][] {
               { "questionA", "Mother's maiden name?" },
               { "questionB", "Father's middle name?" },
               { "questionC", "First car?" },
            };
         }
      };
   }
   
   @Before
   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      person = Fixtures.createPerson();
      person.setId(UUID.randomUUID());

      clientAddress = Fixtures.createClientAddress();
      personAddress = Address.fromString(person.getAddress());
   }

   @Test
   public void testNoQuestions()
   {
      test(new String[] {null, null, null, null, null, null}, BLANK_QUESTION_CODE, false);
   }

   @Test
   public void testQ1()
   {
      test(new String[] {"questionA", "answerA", null, null, null, null}, null, true);
   }

   @Test
   public void testQ1Q2()
   {
      test(new String[] {"questionA", "answerA", "questionB", "answerB", null, null}, null, true);
   }

   @Test
   public void testQ1Q3()
   {
      test(new String[] {"questionA", "answerA", null, null, "questionC", "answerC"}, null, true);
   }

   @Test
   public void testQ1Q2Q3()
   {
      test(new String[] {"questionA", "answerA", "questionB", "answerB", "questionC", "answerC"}, null, false);  //count did not change
   }

   @Test
   public void testInvalidQuestion()
   {
      test(new String[] {"questionA", "answerA", "questionB", "answerB", "questionD", "answerD"}, INVALID_QUESTION_CODE, false);
   }

   @Test
   public void testQuestionWithNoAnswer()
   {
      test(new String[] {"questionA", null, "questionB", "answerB", "questionC", "answerC"}, BLANK_ANSWER_CODE, false);
   }

   @Test
   public void testAnswerWithNoQuestion()
   {
      test(new String[] {"questionA", "answerA", null, "answerB", "questionC", "answerC"}, BLANK_QUESTION_CODE, false);
   }

   private void test(String[] inputQuestionsAndAnswers, String expectedErrorCode, boolean sendValueChange)
   {
      if (expectedErrorCode == null)
      {
         expect(mockPersonDao.update(person)).andReturn(person);
      }
      Capture<PlatformMessage> msgCaptured = null;
      if(sendValueChange) {
      	msgCaptured = Capture.newInstance(CaptureType.ALL);
      	EasyMock.expect(bus.send(EasyMock.capture(msgCaptured))).andAnswer(
            () -> {
               return Futures.immediateFuture(null);
            }
         );
      }

      replay();

      MessageBody request = SetSecurityAnswersRequest.builder()
         .withSecurityQuestion1(inputQuestionsAndAnswers[0])
         .withSecurityAnswer1(inputQuestionsAndAnswers[1])
         .withSecurityQuestion2(inputQuestionsAndAnswers[2])
         .withSecurityAnswer2(inputQuestionsAndAnswers[3])
         .withSecurityQuestion3(inputQuestionsAndAnswers[4])
         .withSecurityAnswer3(inputQuestionsAndAnswers[5])
         .build();

      try
      {
      	int expectedAnswerCount = 0;
         MessageBody response = componentUnderTest.handleRequest(person, buildRequestMessage(request));

         if (expectedErrorCode == null)
         {
            Map<String, String> securityAnswers = person.getSecurityAnswers();
   
            for (int i = 0; i < 6; i += 2)
            {
               String question = inputQuestionsAndAnswers[i];
               String answer = inputQuestionsAndAnswers[i + 1];
   
               if (!isBlank(question))
               {
                  assertThat(securityAnswers, hasEntry(question, answer));
                  expectedAnswerCount++;
               }
               else
               {
                  assertThat(securityAnswers, not(hasKey(question)));
               }
            }
   
            assertThat(response, equalTo(MessageBody.emptyMessage()));
            assertEquals(expectedAnswerCount, person.getSecurityAnswerCount());
            if(sendValueChange) {
            	PlatformMessage msg = msgCaptured.getValue();
            	assertEquals(Capability.EVENT_VALUE_CHANGE, msg.getMessageType());
            	assertTrue(msg.getValue().getAttributes().containsKey(PersonCapability.ATTR_SECURITYANSWERCOUNT));
            	assertEquals(expectedAnswerCount, msg.getValue().getAttributes().get(PersonCapability.ATTR_SECURITYANSWERCOUNT));
            }
         }
         else
         {
            fail();
         }
      }
      catch (ErrorEventException e)
      {
         if (expectedErrorCode == null)
         {
            fail();
         }
         else
         {
            assertThat(e.getCode(), equalTo(expectedErrorCode));
         }
      }
   }

   private PlatformMessage buildRequestMessage(MessageBody request)
   {
      return PlatformMessage.buildRequest(request, clientAddress, personAddress).withActor(personAddress).create();
   }

   @After
   @Override
   public void tearDown() throws Exception
   {
      verify();
      reset();

      super.tearDown();
   }
}


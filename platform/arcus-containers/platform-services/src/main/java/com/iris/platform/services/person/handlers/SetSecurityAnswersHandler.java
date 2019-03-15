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

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.Utils;
import com.iris.core.dao.PersonDAO;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.model.Person;

@Singleton
public class SetSecurityAnswersHandler implements ContextualRequestMessageHandler<Person> {

   public static final String MESSAGE_TYPE = "person:SetSecurityAnswers";
   private static final String QUESTION_PREFIX = "securityQuestion";
   private static final String ANSWER_PREFIX = "securityAnswer";

   public static final String INVALID_QUESTION_CODE = "security.question.invalid";
   public static final String INVALID_QUESTION_MSG = "%s id '%s' does not identify a valid security question";
   public static final String BLANK_QUESTION_CODE = "security.question.blank";
   public static final String BLANK_QUESTION_MSG = "Key for %s must not be blank";
   public static final String BLANK_ANSWER_CODE = "security.answer.blank";
   public static final String BLANK_ANSWER_MSG = "Answer for %s must not be blank";

   public static final String NAME_RESOURCE_BUNDLE = "security_question";
   
   private final PersonDAO personDao;
   private final ResourceBundle securityQuestionBundle;
   private final PlatformMessageBus platformBus;
   
   @Inject
   public SetSecurityAnswersHandler(
         PersonDAO personDao,
         @Named(NAME_RESOURCE_BUNDLE) ResourceBundle securityQuestionBundle,
         PlatformMessageBus platformBus
   ) {
      this.personDao = personDao;
      this.securityQuestionBundle = securityQuestionBundle;
      this.platformBus = platformBus;
   }

   @Override
   public String getMessageType() {
      return MESSAGE_TYPE;
   }

   @Override
   public MessageBody handleRequest(Person context, PlatformMessage msg) {
      Utils.assertNotNull(context, "The person is required");
      int oldAnswerCount = context.getSecurityAnswerCount();

      Map<String, String> securityAnswers = new HashMap<>();

      MessageBody request = msg.getValue();

      setSecurityAnswer(1, true,  request, securityAnswers);
      setSecurityAnswer(2, false, request, securityAnswers);
      setSecurityAnswer(3, false, request, securityAnswers);

      context.setSecurityAnswers(securityAnswers);
      personDao.update(context);
      
      sendSecurityAnswerCountValueChangeIfNecessary(oldAnswerCount, context, msg);

      return MessageBody.emptyMessage();
   }

   private void sendSecurityAnswerCountValueChangeIfNecessary(int oldAnswerCount, Person person, PlatformMessage request) {
		if(oldAnswerCount != person.getSecurityAnswerCount()) {
			MessageBody body = MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, ImmutableMap.<String, Object>of(PersonCapability.ATTR_SECURITYANSWERCOUNT, person.getSecurityAnswerCount()));
		   PlatformMessage msg = PlatformMessage.buildBroadcast(body, request.getDestination())
	            .withPlaceId(request.getPlaceId())
	            .withPopulation(request.getPopulation())
	            .create();
		   platformBus.send(msg);
		}
		
	}

	private void setSecurityAnswer(int questionIndex, boolean required, MessageBody request,
      Map<String, String> securityAnswers)
   {
      String questionKey = QUESTION_PREFIX + questionIndex;
      String questionId = (String) request.getAttributes().get(questionKey);
      String answer = (String) request.getAttributes().get(ANSWER_PREFIX + questionIndex);

      if (isBlank(questionId))
      {
         if (!isBlank(answer) || required)
         {
            throw new ErrorEventException(BLANK_QUESTION_CODE, format(BLANK_QUESTION_MSG, questionKey));
         }
      }
      else
      {
         if (isBlank(answer))
         {
            throw new ErrorEventException(BLANK_ANSWER_CODE, format(BLANK_ANSWER_MSG, questionKey));
         }

         if (!securityQuestionBundle.containsKey(questionId))
         {
            throw new ErrorEventException(INVALID_QUESTION_CODE, format(INVALID_QUESTION_MSG, questionKey, questionId));
         }

         securityAnswers.put(questionId, answer);
      }
   }
}


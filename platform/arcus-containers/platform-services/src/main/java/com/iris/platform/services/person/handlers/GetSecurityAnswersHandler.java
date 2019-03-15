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

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
import com.iris.Utils;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.model.Person;

@Singleton
public class GetSecurityAnswersHandler implements ContextualRequestMessageHandler<Person> {

   public static final String MESSAGE_TYPE = "person:GetSecurityAnswers";

   private static final String RETURN_ATTR = "securityAnswers";

   @Override
   public String getMessageType() {
      return MESSAGE_TYPE;
   }

   @Override
   public MessageBody handleRequest(Person context, PlatformMessage msg) {
      Utils.assertNotNull(context, "The person is required");

      Map<String,String> securityAnswers = context.getSecurityAnswers();
      securityAnswers = securityAnswers == null ? Collections.<String,String>emptyMap() : securityAnswers;

      return MessageBody.buildResponse(msg.getValue(), ImmutableMap.of(RETURN_ATTR, securityAnswers));
   }
}


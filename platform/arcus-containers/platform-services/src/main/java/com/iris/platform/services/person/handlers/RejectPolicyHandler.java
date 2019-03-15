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

import com.google.common.base.Objects;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.PersonDAO;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.errors.NotFoundException;
import com.iris.messages.model.Person;

@Singleton
public class RejectPolicyHandler implements ContextualRequestMessageHandler<Person> {

   private final PersonDAO personDao;

   @Inject
   public RejectPolicyHandler(
         PersonDAO personDao,
         BeanAttributesTransformer<Person> personTransformer,
         PlatformMessageBus bus
   ) {
      this.personDao = personDao;
   }

   @Override
   public String getMessageType() {
      return PersonCapability.RejectPolicyRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Person context, PlatformMessage msg) {
      if(context == null) {
         throw new NotFoundException(msg.getDestination());
      }
      Errors.assertValidRequest(
            Objects.equal(msg.getActor(), msg.getDestination()),
            "Only the same user can accept new policy terms"
      );
      
      MessageBody body = msg.getValue();
      validate(body);

      String policyType = PersonCapability.RejectPolicyRequest.getType(body);
      if (PersonCapability.RejectPolicyRequest.TYPE_PRIVACY.equals(policyType)) {
         context.setPrivacyPolicyAgreed(new Date(0));
      }
      else if (PersonCapability.RejectPolicyRequest.TYPE_TERMS.equals(policyType)) {
         context.setTermsAgreed(new Date(0));
      }
      else {
         throw new ErrorEventException(Errors.CODE_INVALID_REQUEST, "policy type " + policyType + " is not valid.");
      }
      personDao.update(context);
      return PersonCapability.AcceptPolicyResponse.instance();
   }

   private void validate(MessageBody body) {
      Errors.assertRequiredParam(
            PersonCapability.RejectPolicyRequest.getType(body),
            PersonCapability.RejectPolicyRequest.ATTR_TYPE
      );
   }

}


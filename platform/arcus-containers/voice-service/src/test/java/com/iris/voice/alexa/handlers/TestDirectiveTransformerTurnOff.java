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
package com.iris.voice.alexa.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.iris.alexa.AlexaInterfaces;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.service.AlexaService;
import com.iris.voice.alexa.AlexaConfig;

public class TestDirectiveTransformerTurnOff {

   private Model switchModel;
   private AlexaConfig config;
   private MessageBody request;

   @Before
   public void setup() {
      switchModel = new SimpleModel();
      switchModel.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(SwitchCapability.NAMESPACE));

      config = new AlexaConfig();

      request = AlexaService.ExecuteRequest.builder()
         .withDirective(AlexaInterfaces.PowerController.REQUEST_TURNOFF)
         .build();
   }

   @Test
   public void testTurnOffSwitchOn() {
      switchModel.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);

      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(request).txfmRequest(request, switchModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(Capability.CMD_SET_ATTRIBUTES, body.getMessageType());
      assertEquals(SwitchCapability.STATE_OFF, body.getAttributes().get(SwitchCapability.ATTR_STATE));
   }

   @Test
   public void testTurnOffSwitchOff() {
      switchModel.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF);

      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(request).txfmRequest(request, switchModel, config);
      assertFalse(optBody.isPresent());
   }

}


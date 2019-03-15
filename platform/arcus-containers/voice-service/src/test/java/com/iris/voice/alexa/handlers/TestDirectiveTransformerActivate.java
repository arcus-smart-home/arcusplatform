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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.alexa.AlexaInterfaces;
import com.iris.alexa.error.AlexaErrors;
import com.iris.alexa.error.AlexaException;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.service.AlexaService;
import com.iris.messages.type.Action;
import com.iris.messages.type.AlexaCause;
import com.iris.voice.alexa.AlexaConfig;

public class TestDirectiveTransformerActivate {

   private Model sceneModel;
   private AlexaConfig config;
   private MessageBody activate;

   @Before
   public void setup() {
      Action a = new Action();
      a.setTemplate("switches");
      a.setContext(ImmutableMap.of(Address.platformDriverAddress(UUID.randomUUID()).getRepresentation(), ImmutableMap.of("switch", "ON")));
      sceneModel = new SimpleModel();
      sceneModel.setAttribute(SceneCapability.ATTR_NAME, "test");
      sceneModel.setAttribute(SceneCapability.ATTR_ENABLED, true);
      sceneModel.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(SceneCapability.NAMESPACE));
      sceneModel.setAttribute(SceneCapability.ATTR_ACTIONS, ImmutableList.of(a.toMap()));

      config = new AlexaConfig();

      activate = AlexaService.ExecuteRequest.builder()
         .withDirective(AlexaInterfaces.SceneController.REQUEST_ACTIVATE)
         .build();
   }

   @Test
   public void testActivate() {
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(activate).txfmRequest(activate, sceneModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(SceneCapability.FireRequest.NAME, body.getMessageType());
   }

   @Test
   public void testActivateForbidden() {
      Action a = new Action();
      a.setTemplate("doorlocks");
      a.setContext(ImmutableMap.of(Address.platformDriverAddress(UUID.randomUUID()).getRepresentation(), ImmutableMap.of("lockstate", "UNLOCKED")));

      sceneModel.setAttribute(SceneCapability.ATTR_ACTIONS, ImmutableList.of(a.toMap()));

      try {
         DirectiveTransformer.transformerFor(activate).txfmRequest(activate, sceneModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_INVALID_VALUE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testResponse() {
      Map<String, Object> resp = DirectiveTransformer.transformerFor(activate).txfmResponse(sceneModel, null, config);
      assertNotNull(resp.get("cause"));
      assertNotNull(resp.get("timestamp"));
      Map<String, Object> cause = (Map<String, Object>) resp.get("cause");
      AlexaCause c = new AlexaCause(cause);
      assertEquals("VOICE_INTERACTION", c.getType());
   }
}


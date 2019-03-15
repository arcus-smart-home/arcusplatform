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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.alexa.AlexaInterfaces;
import com.iris.alexa.error.AlexaErrors;
import com.iris.alexa.error.AlexaException;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.FanCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.service.AlexaService;
import com.iris.voice.alexa.AlexaConfig;

public class TestDirectiveTransformerAdjustPercentage {

   private Model fanModel;
   private AlexaConfig config;

   @Before
   public void setup() {
      fanModel = new SimpleModel();
      fanModel.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(SwitchCapability.NAMESPACE, FanCapability.NAMESPACE));

      config = new AlexaConfig();
   }

   @Test
   public void testAdjustMissingArgumentArgsNull() {
      try {
         MessageBody req = request(null);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, fanModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_INVALID_DIRECTIVE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testAdjustMissingArgumentDeltaNull() {
      try {
         MessageBody req = builder().withArguments(ImmutableMap.of()).build();
         DirectiveTransformer.transformerFor(req).txfmRequest(req, fanModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_INVALID_DIRECTIVE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testAdjustOutOfRangeAbove() {
      try {
         MessageBody req = request(101);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, fanModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_VALUE_OUT_OF_RANGE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testAdjustOutOfRangeBelow() {
      try {
         MessageBody req = request(-101);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, fanModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_VALUE_OUT_OF_RANGE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testAdjustCurrentStateUnknown() {
      try {
         MessageBody req = request(25);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, fanModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_INTERNAL_ERROR, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testDecreaseOff() {
      fanModel.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF);
      fanModel.setAttribute(FanCapability.ATTR_SPEED, 2);

      MessageBody req = request(-25);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, fanModel, config);
      assertFalse(optBody.isPresent());
   }

   @Test
   public void testIncreaseOff() {
      fanModel.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF);
      fanModel.setAttribute(FanCapability.ATTR_SPEED, 2);

      MessageBody req = request(25);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, fanModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(Capability.CMD_SET_ATTRIBUTES, body.getMessageType());
      assertEquals(2, body.getAttributes().size());
      assertEquals(SwitchCapability.STATE_ON, SwitchCapability.getState(body));
      assertEquals(1, FanCapability.getSpeed(body).intValue());
   }

   @Test
   public void testDecreaseTurnsOff() {
      fanModel.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);
      fanModel.setAttribute(FanCapability.ATTR_SPEED, 1);

      MessageBody req = request(-25);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, fanModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(Capability.CMD_SET_ATTRIBUTES, body.getMessageType());
      assertEquals(1, body.getAttributes().size());
      assertEquals(SwitchCapability.STATE_OFF, SwitchCapability.getState(body));
   }

   @Test
   public void testDecrease() {
      fanModel.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);
      fanModel.setAttribute(FanCapability.ATTR_SPEED, 2);

      MessageBody req = request(-25);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, fanModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(Capability.CMD_SET_ATTRIBUTES, body.getMessageType());
      assertEquals(1, body.getAttributes().size());
      assertEquals(1, FanCapability.getSpeed(body).intValue());
   }

   @Test
   public void testIncreaseNoOpAtMaxSpeed() {
      fanModel.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);
      fanModel.setAttribute(FanCapability.ATTR_SPEED, 3);

      MessageBody req = request(25);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, fanModel, config);
      assertFalse(optBody.isPresent());
   }

   @Test
   public void testIncrease() {
      fanModel.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);
      fanModel.setAttribute(FanCapability.ATTR_SPEED, 2);

      MessageBody req = request(25);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, fanModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(Capability.CMD_SET_ATTRIBUTES, body.getMessageType());
      assertEquals(1, body.getAttributes().size());
      assertEquals(3, FanCapability.getSpeed(body).intValue());
   }

   private MessageBody request(Integer delta) {
      AlexaService.ExecuteRequest.Builder builder = builder();

      if(delta != null) {
         builder.withArguments(ImmutableMap.of(AlexaInterfaces.PercentageController.ARG_PERCENTAGEDELTA, delta));
      }

      return builder.build();
   }

   private AlexaService.ExecuteRequest.Builder builder() {
      return AlexaService.ExecuteRequest.builder()
         .withDirective(AlexaInterfaces.PercentageController.REQUEST_ADJUSTPERCENTAGE);
   }

}


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
import static org.junit.Assert.assertNull;
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
import com.iris.messages.capability.DimmerCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.service.AlexaService;
import com.iris.voice.alexa.AlexaConfig;

public class TestDirectiveTransformerSetBrightness {

   private Model dimmerModel;
   private AlexaConfig config;

   @Before
   public void setup() {
      dimmerModel = new SimpleModel();
      dimmerModel.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(SwitchCapability.NAMESPACE, DimmerCapability.NAMESPACE));

      config = new AlexaConfig();
   }

   @Test
   public void testSetMissingArgumentArgsNull() {
      try {
         MessageBody req = request(null);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, dimmerModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_INVALID_DIRECTIVE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testSetMissingArgumentBrightnessNull() {
      try {
         MessageBody req = builder().withArguments(ImmutableMap.of()).build();
         DirectiveTransformer.transformerFor(req).txfmRequest(req, dimmerModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_INVALID_DIRECTIVE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testSetOutOfRangeAbove() {
      try {
         MessageBody req = request(101);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, dimmerModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_VALUE_OUT_OF_RANGE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testSetOutOfRangeBelow() {
      try {
         MessageBody req = request(-1);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, dimmerModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_VALUE_OUT_OF_RANGE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testSetCurrentStateUnknown() {
      try {
         MessageBody req = request(25);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, dimmerModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_INTERNAL_ERROR, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testSetTo0NoopOff() {
      dimmerModel.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF);
      dimmerModel.setAttribute(DimmerCapability.ATTR_BRIGHTNESS, 100);

      MessageBody req = request(0);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, dimmerModel, config);
      assertFalse(optBody.isPresent());
   }

   @Test
   public void testSetTurnsOn() {
      dimmerModel.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF);
      dimmerModel.setAttribute(DimmerCapability.ATTR_BRIGHTNESS, 100);

      MessageBody req = request(25);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, dimmerModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(Capability.CMD_SET_ATTRIBUTES, body.getMessageType());
      assertEquals(SwitchCapability.STATE_ON, SwitchCapability.getState(body));
      assertEquals(25, DimmerCapability.getBrightness(body).intValue());
   }

   @Test
   public void testSetTo0TurnsOff() {
      dimmerModel.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);
      dimmerModel.setAttribute(DimmerCapability.ATTR_BRIGHTNESS, 25);

      MessageBody req = request(0);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, dimmerModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(Capability.CMD_SET_ATTRIBUTES, body.getMessageType());
      assertEquals(SwitchCapability.STATE_OFF, SwitchCapability.getState(body));
      assertNull(body.getAttributes().get(DimmerCapability.ATTR_BRIGHTNESS));
   }

   @Test
   public void testSetToSameValueNoop() {
      dimmerModel.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);
      dimmerModel.setAttribute(DimmerCapability.ATTR_BRIGHTNESS, 10);

      MessageBody req = request(10);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, dimmerModel, config);
      assertFalse(optBody.isPresent());
   }

   private MessageBody request(Integer value) {
      AlexaService.ExecuteRequest.Builder builder = builder();

      if(value != null) {
         builder.withArguments(ImmutableMap.of(AlexaInterfaces.BrightnessController.PROP_BRIGHTNESS, value));
      }

      return builder.build();
   }

   private AlexaService.ExecuteRequest.Builder builder() {
      return AlexaService.ExecuteRequest.builder()
         .withDirective(AlexaInterfaces.BrightnessController.REQUEST_SETBRIGHTNESS);
   }

}


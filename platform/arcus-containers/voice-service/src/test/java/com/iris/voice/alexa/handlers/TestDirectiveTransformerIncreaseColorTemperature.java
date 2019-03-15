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

import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.iris.alexa.AlexaInterfaces;
import com.iris.alexa.error.AlexaErrors;
import com.iris.alexa.error.AlexaException;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ColorCapability;
import com.iris.messages.capability.ColorTemperatureCapability;
import com.iris.messages.capability.DimmerCapability;
import com.iris.messages.capability.LightCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.service.AlexaService;
import com.iris.voice.alexa.AlexaConfig;

public class TestDirectiveTransformerIncreaseColorTemperature {

   private Model colorTempModel;
   private AlexaConfig config;

   @Before
   public void setup() {
      colorTempModel = new SimpleModel();
      colorTempModel.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(SwitchCapability.NAMESPACE, DimmerCapability.NAMESPACE, LightCapability.NAMESPACE, ColorCapability.NAMESPACE, ColorTemperatureCapability.NAMESPACE));
      colorTempModel.setAttribute(ColorTemperatureCapability.ATTR_MINCOLORTEMP, 2700);
      colorTempModel.setAttribute(ColorTemperatureCapability.ATTR_MAXCOLORTEMP, 6500);
      colorTempModel.setAttribute(LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_COLORTEMP);
      colorTempModel.setAttribute(ColorTemperatureCapability.ATTR_COLORTEMP, 2700);
      colorTempModel.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);

      config = new AlexaConfig();
   }

   @Test
   public void testIncreaseColorMode() {
      colorTempModel.setAttribute(LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_COLOR);
      try {
         MessageBody req = request();
         DirectiveTransformer.transformerFor(req).txfmRequest(req, colorTempModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_NOT_SUPPORTED_IN_CURRENT_MODE, ae.getErrorMessage().getAttributes().get("type"));
         Map<String, Object> payload = AlexaService.AlexaErrorEvent.getPayload(ae.getErrorMessage());
         assertEquals("COLOR", payload.get("currentDeviceMode"));
      }
   }

   @Test
   public void testIncreaseNormalMode() {
      colorTempModel.setAttribute(LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_NORMAL);
      try {
         MessageBody req = request();
         DirectiveTransformer.transformerFor(req).txfmRequest(req, colorTempModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_NOT_SUPPORTED_IN_CURRENT_MODE, ae.getErrorMessage().getAttributes().get("type"));
         Map<String, Object> payload = AlexaService.AlexaErrorEvent.getPayload(ae.getErrorMessage());
         assertEquals("OTHER", payload.get("currentDeviceMode"));
      }
   }

   @Test
   public void testIncreaseCurrentStateUnknown() {
      colorTempModel.setAttribute(ColorTemperatureCapability.ATTR_COLORTEMP, null);
      try {
         MessageBody req = request();
         DirectiveTransformer.transformerFor(req).txfmRequest(req, colorTempModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_INTERNAL_ERROR, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testIncreaseMinUnknown() {
      colorTempModel.setAttribute(ColorTemperatureCapability.ATTR_MINCOLORTEMP, null);
      try {
         MessageBody req = request();
         DirectiveTransformer.transformerFor(req).txfmRequest(req, colorTempModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_INTERNAL_ERROR, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testIncreaseMaxUnknown() {
      colorTempModel.setAttribute(ColorTemperatureCapability.ATTR_MAXCOLORTEMP, null);
      try {
         MessageBody req = request();
         DirectiveTransformer.transformerFor(req).txfmRequest(req, colorTempModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_INTERNAL_ERROR, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testIncreaseAboveMaxNoop() {
      colorTempModel.setAttribute(ColorTemperatureCapability.ATTR_COLORTEMP, 6500);
      MessageBody req = request();
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, colorTempModel, config);
      assertFalse(optBody.isPresent());
   }

   @Test
   public void testIncrease() {
      colorTempModel.setAttribute(ColorTemperatureCapability.ATTR_COLORTEMP, 3000);
      MessageBody req = request();
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, colorTempModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(1, body.getAttributes().size());
      assertEquals(3500, ColorTemperatureCapability.getColortemp(body).intValue());
   }

   @Test
   public void testIncreaseTurnsOn() {
      colorTempModel.setAttribute(ColorTemperatureCapability.ATTR_COLORTEMP, 3000);
      colorTempModel.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF);
      MessageBody req = request();
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, colorTempModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(2, body.getAttributes().size());
      assertEquals(3500, ColorTemperatureCapability.getColortemp(body).intValue());
      assertEquals(SwitchCapability.STATE_ON, SwitchCapability.getState(body));
   }

   private MessageBody request() {
      return AlexaService.ExecuteRequest.builder()
         .withDirective(AlexaInterfaces.ColorTemperatureController.REQUEST_INCREASECOLORTEMPERATURE)
         .build();
   }

}


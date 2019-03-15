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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.alexa.AlexaInterfaces;
import com.iris.alexa.error.AlexaErrors;
import com.iris.alexa.error.AlexaException;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ThermostatCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.service.AlexaService;
import com.iris.messages.type.AlexaTemperature;
import com.iris.voice.alexa.AlexaConfig;

public class TestDirectiveTransformerSetTargetTemperatureV2 {

   private Model thermostatModel;
   private AlexaConfig config;

   @Before
   public void setup() {
      thermostatModel = new SimpleModel();
      thermostatModel.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(ThermostatCapability.NAMESPACE));
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_OFF);
      thermostatModel.setAttribute(ThermostatCapability.ATTR_COOLSETPOINT, 21.11);
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HEATSETPOINT, 19.44);

      config = new AlexaConfig();
   }

   @Test
   public void testSetMissingArgumentArgsNull() {
      try {
         MessageBody req = request(null);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_INVALID_DIRECTIVE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testSetMissingArgumentValueNull() {
      try {
         MessageBody req = builder().withArguments(ImmutableMap.of()).build();
         DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_INVALID_DIRECTIVE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testSet() {
      MessageBody req = request(21.11);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(ThermostatCapability.SetIdealTemperatureRequest.NAME, body.getMessageType());
      assertEquals(21.11, ThermostatCapability.SetIdealTemperatureRequest.getTemperature(body).doubleValue(), .01);
   }

   private MessageBody request(Double value) {
      AlexaService.ExecuteRequest.Builder builder = builder();

      if(value != null) {
         builder.withArguments(ImmutableMap.of("targetTemperature", value));
      }

      return builder.build();
   }

   private AlexaService.ExecuteRequest.Builder builder() {
      return AlexaService.ExecuteRequest.builder()
         .withDirective("SetTargetTemperatureRequest");
   }

}


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

public class TestDirectiveTransformerAdjustTargetTemperature {

   private Model thermostatModel;
   private AlexaConfig config;

   @Before
   public void setup() {
      thermostatModel = new SimpleModel();
      thermostatModel.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(ThermostatCapability.NAMESPACE));
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_OFF);
      thermostatModel.setAttribute(ThermostatCapability.ATTR_MINSETPOINT, 1.67);
      thermostatModel.setAttribute(ThermostatCapability.ATTR_MAXSETPOINT, 35.00);
      thermostatModel.setAttribute(ThermostatCapability.ATTR_COOLSETPOINT, 21.11);
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HEATSETPOINT, 19.44);

      config = new AlexaConfig();
   }

   @Test
   public void testAdjustMissingArgumentArgsNull() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_HEAT);
      try {
         MessageBody req = request(null);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_INVALID_DIRECTIVE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testAdjustMissingArgumentArgsDeltaMissing() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_HEAT);
      try {
         MessageBody req = builder().withArguments(ImmutableMap.of()).build();
         DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_INVALID_DIRECTIVE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testAdjustOff() {
      try {
         AlexaTemperature temp = new AlexaTemperature();
         temp.setScale(AlexaTemperature.SCALE_FAHRENHEIT);
         temp.setValue(3.0);
         MessageBody req = request(temp);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_THERMOSTAT_IS_OFF, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testAdjustEco() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_ECO);
      try {
         AlexaTemperature temp = new AlexaTemperature();
         temp.setScale(AlexaTemperature.SCALE_FAHRENHEIT);
         temp.setValue(3.0);
         MessageBody req = request(temp);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_NOT_SUPPORTED_IN_CURRENT_MODE, ae.getErrorMessage().getAttributes().get("type"));
         Map<String, Object> payload = AlexaService.AlexaErrorEvent.getPayload(ae.getErrorMessage());
         assertEquals("OTHER", payload.get("currentDeviceMode"));
      }
   }

   @Test
   public void testSetNoCoolCurrentState() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_AUTO);
      thermostatModel.setAttribute(ThermostatCapability.ATTR_COOLSETPOINT, null);
      try {
         AlexaTemperature temp = new AlexaTemperature();
         temp.setScale(AlexaTemperature.SCALE_FAHRENHEIT);
         temp.setValue(3.0);
         MessageBody req = request(temp);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_INTERNAL_ERROR, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testSetNoHeatCurrentState() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_AUTO);
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HEATSETPOINT, null);
      try {
         AlexaTemperature temp = new AlexaTemperature();
         temp.setScale(AlexaTemperature.SCALE_FAHRENHEIT);
         temp.setValue(3.0);
         MessageBody req = request(temp);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_INTERNAL_ERROR, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testDecreaseGoesBelowCool() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_COOL);
      thermostatModel.setAttribute(ThermostatCapability.ATTR_COOLSETPOINT, 2.00);
      try {
         AlexaTemperature temp = new AlexaTemperature();
         temp.setScale(AlexaTemperature.SCALE_FAHRENHEIT);
         temp.setValue(-3.0);
         MessageBody req = request(temp);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_TEMPERATURE_VALUE_OUT_OF_RANGE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testDecreaseGoesBelowHeat() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_HEAT);
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HEATSETPOINT, 2.00);
      try {
         AlexaTemperature temp = new AlexaTemperature();
         temp.setScale(AlexaTemperature.SCALE_FAHRENHEIT);
         temp.setValue(-3.0);
         MessageBody req = request(temp);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_TEMPERATURE_VALUE_OUT_OF_RANGE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testDecreaseGoesBelowAuto() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_AUTO);
      thermostatModel.setAttribute(ThermostatCapability.ATTR_COOLSETPOINT, 3.67);
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HEATSETPOINT, 2.00);
      try {
         AlexaTemperature temp = new AlexaTemperature();
         temp.setScale(AlexaTemperature.SCALE_FAHRENHEIT);
         temp.setValue(-3.0);
         MessageBody req = request(temp);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_TEMPERATURE_VALUE_OUT_OF_RANGE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testIncreaseGoesAboveCool() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_COOL);
      thermostatModel.setAttribute(ThermostatCapability.ATTR_COOLSETPOINT, 34.00);
      try {
         AlexaTemperature temp = new AlexaTemperature();
         temp.setScale(AlexaTemperature.SCALE_FAHRENHEIT);
         temp.setValue(3.0);
         MessageBody req = request(temp);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_TEMPERATURE_VALUE_OUT_OF_RANGE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testIncreaseGoesAboveHeat() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_HEAT);
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HEATSETPOINT, 34.00);
      try {
         AlexaTemperature temp = new AlexaTemperature();
         temp.setScale(AlexaTemperature.SCALE_FAHRENHEIT);
         temp.setValue(3.0);
         MessageBody req = request(temp);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_TEMPERATURE_VALUE_OUT_OF_RANGE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testIncreaseGoesAboveAuto() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_AUTO);
      thermostatModel.setAttribute(ThermostatCapability.ATTR_COOLSETPOINT, 34.00);
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HEATSETPOINT, 32.33);
      try {
         AlexaTemperature temp = new AlexaTemperature();
         temp.setScale(AlexaTemperature.SCALE_FAHRENHEIT);
         temp.setValue(3.0);
         MessageBody req = request(temp);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_TEMPERATURE_VALUE_OUT_OF_RANGE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testDecreaseCool() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_COOL);
      thermostatModel.setAttribute(ThermostatCapability.ATTR_COOLSETPOINT, 20.00);
      AlexaTemperature temp = new AlexaTemperature();
      temp.setScale(AlexaTemperature.SCALE_CELSIUS);
      temp.setValue(-3.0);
      MessageBody req = request(temp);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(Capability.CMD_SET_ATTRIBUTES, body.getMessageType());
      assertEquals(1, body.getAttributes().size());
      assertEquals(17.00, ThermostatCapability.getCoolsetpoint(body).doubleValue(), .01);
   }

   @Test
   public void testIncreaseCool() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_COOL);
      thermostatModel.setAttribute(ThermostatCapability.ATTR_COOLSETPOINT, 20.00);
      AlexaTemperature temp = new AlexaTemperature();
      temp.setScale(AlexaTemperature.SCALE_CELSIUS);
      temp.setValue(3.0);
      MessageBody req = request(temp);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(Capability.CMD_SET_ATTRIBUTES, body.getMessageType());
      assertEquals(1, body.getAttributes().size());
      assertEquals(23.00, ThermostatCapability.getCoolsetpoint(body).doubleValue(), .01);
   }

   @Test
   public void testDecreaseHeat() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_HEAT);
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HEATSETPOINT, 20.00);
      AlexaTemperature temp = new AlexaTemperature();
      temp.setScale(AlexaTemperature.SCALE_CELSIUS);
      temp.setValue(-3.0);
      MessageBody req = request(temp);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(Capability.CMD_SET_ATTRIBUTES, body.getMessageType());
      assertEquals(1, body.getAttributes().size());
      assertEquals(17.00, ThermostatCapability.getHeatsetpoint(body).doubleValue(), .01);
   }

   @Test
   public void testIncreaseHeat() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_HEAT);
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HEATSETPOINT, 20.00);
      AlexaTemperature temp = new AlexaTemperature();
      temp.setScale(AlexaTemperature.SCALE_CELSIUS);
      temp.setValue(3.0);
      MessageBody req = request(temp);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(Capability.CMD_SET_ATTRIBUTES, body.getMessageType());
      assertEquals(1, body.getAttributes().size());
      assertEquals(23.00, ThermostatCapability.getHeatsetpoint(body).doubleValue(), .01);
   }

   @Test
   public void testDecreaseAuto() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_AUTO);
      AlexaTemperature temp = new AlexaTemperature();
      temp.setScale(AlexaTemperature.SCALE_CELSIUS);
      temp.setValue(-3.0);
      MessageBody req = request(temp);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(Capability.CMD_SET_ATTRIBUTES, body.getMessageType());
      assertEquals(2, body.getAttributes().size());
      assertEquals(16.44, ThermostatCapability.getHeatsetpoint(body).doubleValue(), .01);
      assertEquals(18.11, ThermostatCapability.getCoolsetpoint(body).doubleValue(), .01);
   }

   @Test
   public void testIncreaseAuto() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_AUTO);
      AlexaTemperature temp = new AlexaTemperature();
      temp.setScale(AlexaTemperature.SCALE_CELSIUS);
      temp.setValue(3.0);
      MessageBody req = request(temp);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(Capability.CMD_SET_ATTRIBUTES, body.getMessageType());
      assertEquals(2, body.getAttributes().size());
      assertEquals(22.44, ThermostatCapability.getHeatsetpoint(body).doubleValue(), .01);
      assertEquals(24.11, ThermostatCapability.getCoolsetpoint(body).doubleValue(), .01);
   }

   private MessageBody request(AlexaTemperature delta) {
      AlexaService.ExecuteRequest.Builder builder = builder();

      if(delta != null) {
         builder.withArguments(ImmutableMap.of("targetSetpointDelta", delta.toMap()));
      }

      return builder.build();
   }

   private AlexaService.ExecuteRequest.Builder builder() {
      return AlexaService.ExecuteRequest.builder()
         .withDirective(AlexaInterfaces.ThermostatController.REQUEST_ADJUSTTARGETTEMPERATURE);
   }

}


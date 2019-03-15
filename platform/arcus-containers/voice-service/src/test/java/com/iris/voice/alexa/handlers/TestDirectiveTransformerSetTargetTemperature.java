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

public class TestDirectiveTransformerSetTargetTemperature {

   private Model thermostatModel;
   private AlexaConfig config;

   @Before
   public void setup() {
      thermostatModel = new SimpleModel();
      thermostatModel.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(ThermostatCapability.NAMESPACE));
      thermostatModel.setAttribute(ThermostatCapability.ATTR_MINSETPOINT, 1.67);
      thermostatModel.setAttribute(ThermostatCapability.ATTR_MAXSETPOINT, 35.00);
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_OFF);
      thermostatModel.setAttribute(ThermostatCapability.ATTR_COOLSETPOINT, 21.11);
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HEATSETPOINT, 19.44);

      config = new AlexaConfig();
   }

   @Test
   public void testSetMissingArgumentArgsNull() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_HEAT);
      try {
         MessageBody req = request(null, null, null);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_INVALID_DIRECTIVE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testSetOff() {
      try {
         MessageBody req = request(null, null, null);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_THERMOSTAT_IS_OFF, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testSetEco() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_ECO);
      try {
         MessageBody req = request(null, null, null);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_NOT_SUPPORTED_IN_CURRENT_MODE, ae.getErrorMessage().getAttributes().get("type"));
         Map<String, Object> payload = AlexaService.AlexaErrorEvent.getPayload(ae.getErrorMessage());
         assertEquals("OTHER", payload.get("currentDeviceMode"));
      }
   }

   @Test
   public void testSetBelowMin() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_COOL);
      try {
         AlexaTemperature temp = new AlexaTemperature();
         temp.setScale(AlexaTemperature.SCALE_CELSIUS);
         temp.setValue(1.66);
         MessageBody req = request(temp, null, null);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_TEMPERATURE_VALUE_OUT_OF_RANGE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testSetAboveMax() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_COOL);
      try {
         AlexaTemperature temp = new AlexaTemperature();
         temp.setScale(AlexaTemperature.SCALE_CELSIUS);
         temp.setValue(35.1);
         MessageBody req = request(temp, null, null);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_TEMPERATURE_VALUE_OUT_OF_RANGE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testSetNoCoolCurrentState() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_AUTO);
      thermostatModel.setAttribute(ThermostatCapability.ATTR_COOLSETPOINT, null);
      try {
         AlexaTemperature temp = new AlexaTemperature();
         temp.setScale(AlexaTemperature.SCALE_CELSIUS);
         temp.setValue(20.00);
         MessageBody req = request(temp, null, null);
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
         temp.setScale(AlexaTemperature.SCALE_CELSIUS);
         temp.setValue(20.00);
         MessageBody req = request(temp, null, null);
         DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_INTERNAL_ERROR, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testTargetCoolNoChange() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_COOL);
      AlexaTemperature temp = new AlexaTemperature();
      temp.setScale(AlexaTemperature.SCALE_CELSIUS);
      temp.setValue(21.11);
      MessageBody req = request(temp, null, null);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      assertFalse(optBody.isPresent());
   }

   @Test
   public void testTargetCool() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_COOL);
      AlexaTemperature temp = new AlexaTemperature();
      temp.setScale(AlexaTemperature.SCALE_CELSIUS);
      temp.setValue(22.00);
      MessageBody req = request(temp, null, null);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(Capability.CMD_SET_ATTRIBUTES, body.getMessageType());
      assertEquals(1, body.getAttributes().size());
      assertEquals(22.00, ThermostatCapability.getCoolsetpoint(body).doubleValue(), .01);
   }

   @Test
   public void testTargetCoolF() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_COOL);
      AlexaTemperature temp = new AlexaTemperature();
      temp.setScale(AlexaTemperature.SCALE_FAHRENHEIT);
      temp.setValue(72.0);
      MessageBody req = request(temp, null, null);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(Capability.CMD_SET_ATTRIBUTES, body.getMessageType());
      assertEquals(1, body.getAttributes().size());
      assertEquals(22.22, ThermostatCapability.getCoolsetpoint(body).doubleValue(), .01);
   }

   @Test
   public void testTargetHeatNoChange() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_HEAT);
      AlexaTemperature temp = new AlexaTemperature();
      temp.setScale(AlexaTemperature.SCALE_CELSIUS);
      temp.setValue(19.44);
      MessageBody req = request(temp, null, null);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      assertFalse(optBody.isPresent());
   }

   @Test
   public void testTargetHeat() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_HEAT);
      AlexaTemperature temp = new AlexaTemperature();
      temp.setScale(AlexaTemperature.SCALE_CELSIUS);
      temp.setValue(19.00);
      MessageBody req = request(temp, null, null);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(Capability.CMD_SET_ATTRIBUTES, body.getMessageType());
      assertEquals(1, body.getAttributes().size());
      assertEquals(19.00, ThermostatCapability.getHeatsetpoint(body).doubleValue(), .01);
   }

   @Test
   public void testTargetHeatF() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_HEAT);
      AlexaTemperature temp = new AlexaTemperature();
      temp.setScale(AlexaTemperature.SCALE_FAHRENHEIT);
      temp.setValue(65.0);
      MessageBody req = request(temp, null, null);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(Capability.CMD_SET_ATTRIBUTES, body.getMessageType());
      assertEquals(1, body.getAttributes().size());
      assertEquals(18.33, ThermostatCapability.getHeatsetpoint(body).doubleValue(), .01);
   }

   @Test
   public void testTargetAutoNoChange() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_AUTO);
      AlexaTemperature temp = new AlexaTemperature();
      temp.setScale(AlexaTemperature.SCALE_CELSIUS);
      temp.setValue(20.275);
      MessageBody req = request(temp, null, null);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      assertFalse(optBody.isPresent());
   }

   @Test
   public void testTargetAuto() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_AUTO);
      AlexaTemperature temp = new AlexaTemperature();
      temp.setScale(AlexaTemperature.SCALE_CELSIUS);
      temp.setValue(20.00);
      MessageBody req = request(temp, null, null);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(Capability.CMD_SET_ATTRIBUTES, body.getMessageType());
      assertEquals(2, body.getAttributes().size());
      assertEquals(20.835, ThermostatCapability.getCoolsetpoint(body).doubleValue(), .01);
      assertEquals(19.165, ThermostatCapability.getHeatsetpoint(body).doubleValue(), .01);
   }

   @Test
   public void testTargetAutoF() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_AUTO);
      AlexaTemperature temp = new AlexaTemperature();
      temp.setScale(AlexaTemperature.SCALE_FAHRENHEIT);
      temp.setValue(72.0);
      MessageBody req = request(temp, null, null);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(Capability.CMD_SET_ATTRIBUTES, body.getMessageType());
      assertEquals(2, body.getAttributes().size());
      assertEquals(23.055, ThermostatCapability.getCoolsetpoint(body).doubleValue(), .01);
      assertEquals(21.385, ThermostatCapability.getHeatsetpoint(body).doubleValue(), .01);
   }

   @Test
   public void testTargetUpperSnap() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_AUTO);
      AlexaTemperature temp = new AlexaTemperature();
      temp.setScale(AlexaTemperature.SCALE_CELSIUS);
      temp.setValue(34.2);
      MessageBody req = request(temp, null, null);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(Capability.CMD_SET_ATTRIBUTES, body.getMessageType());
      assertEquals(2, body.getAttributes().size());
      assertEquals(35.00, ThermostatCapability.getCoolsetpoint(body).doubleValue(), .01);
      assertEquals(33.33, ThermostatCapability.getHeatsetpoint(body).doubleValue(), .01);
   }

   @Test
   public void testTargetLowerSnap() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_AUTO);
      AlexaTemperature temp = new AlexaTemperature();
      temp.setScale(AlexaTemperature.SCALE_CELSIUS);
      temp.setValue(2.4);
      MessageBody req = request(temp, null, null);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(Capability.CMD_SET_ATTRIBUTES, body.getMessageType());
      assertEquals(2, body.getAttributes().size());
      assertEquals(3.34, ThermostatCapability.getCoolsetpoint(body).doubleValue(), .01);
      assertEquals(1.67, ThermostatCapability.getHeatsetpoint(body).doubleValue(), .01);
   }

   @Test
   public void testDualNotInAutoThrows() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_COOL);
      AlexaTemperature lower = new AlexaTemperature();
      lower.setValue(19.00);
      lower.setScale(AlexaTemperature.SCALE_CELSIUS);

      AlexaTemperature upper = new AlexaTemperature();
      upper.setValue(21.00);
      upper.setScale(AlexaTemperature.SCALE_CELSIUS);

      MessageBody req = request(null, lower, upper);

      try {
         DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_DUAL_SETPOINTS_UNSUPPORTED, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testDualMissingLowerThrows() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_AUTO);
      AlexaTemperature upper = new AlexaTemperature();
      upper.setValue(21.00);
      upper.setScale(AlexaTemperature.SCALE_CELSIUS);

      MessageBody req = request(null, null, upper);

      try {
         DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_INVALID_DIRECTIVE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testDualMissingUpperThrows() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_AUTO);

      AlexaTemperature lower = new AlexaTemperature();
      lower.setValue(19.00);
      lower.setScale(AlexaTemperature.SCALE_CELSIUS);

      MessageBody req = request(null, lower, null);

      try {
         DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_INVALID_DIRECTIVE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testDualGapTooSmall() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_AUTO);

      AlexaTemperature lower = new AlexaTemperature();
      lower.setValue(19.00);
      lower.setScale(AlexaTemperature.SCALE_CELSIUS);

      AlexaTemperature upper = new AlexaTemperature();
      upper.setValue(20.60);
      upper.setScale(AlexaTemperature.SCALE_CELSIUS);

      MessageBody req = request(null, lower, upper);

      try {
         DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      } catch(AlexaException ae) {
         assertEquals(AlexaErrors.TYPE_REQUESTED_SETPOINTS_TOO_CLOSE, ae.getErrorMessage().getAttributes().get("type"));
      }
   }

   @Test
   public void testDual() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_AUTO);
      AlexaTemperature lower = new AlexaTemperature();
      lower.setValue(19.00);
      lower.setScale(AlexaTemperature.SCALE_CELSIUS);

      AlexaTemperature upper = new AlexaTemperature();
      upper.setValue(21.00);
      upper.setScale(AlexaTemperature.SCALE_CELSIUS);

      MessageBody req = request(null, lower, upper);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(Capability.CMD_SET_ATTRIBUTES, body.getMessageType());
      assertEquals(2, body.getAttributes().size());
      assertEquals(21.00, ThermostatCapability.getCoolsetpoint(body).doubleValue(), .01);
      assertEquals(19.00, ThermostatCapability.getHeatsetpoint(body).doubleValue(), .01);
   }

   @Test
   public void testDualF() {
      thermostatModel.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_AUTO);
      AlexaTemperature lower = new AlexaTemperature();
      lower.setValue(70.00);
      lower.setScale(AlexaTemperature.SCALE_FAHRENHEIT);

      AlexaTemperature upper = new AlexaTemperature();
      upper.setValue(73.00);
      upper.setScale(AlexaTemperature.SCALE_FAHRENHEIT);

      MessageBody req = request(null, lower, upper);
      Optional<MessageBody> optBody = DirectiveTransformer.transformerFor(req).txfmRequest(req, thermostatModel, config);
      assertTrue(optBody.isPresent());
      MessageBody body = optBody.get();
      assertEquals(Capability.CMD_SET_ATTRIBUTES, body.getMessageType());
      assertEquals(2, body.getAttributes().size());
      assertEquals(22.78, ThermostatCapability.getCoolsetpoint(body).doubleValue(), .01);
      assertEquals(21.11, ThermostatCapability.getHeatsetpoint(body).doubleValue(), .01);
   }

   private MessageBody request(AlexaTemperature target, AlexaTemperature lower, AlexaTemperature upper) {
      AlexaService.ExecuteRequest.Builder builder = builder();

      if(target != null || lower != null || upper != null) {
         ImmutableMap.Builder<String, Object> args = ImmutableMap.builder();
         if(target != null) {
            args.put("targetSetpoint", target.toMap());
         }
         if(lower != null) {
            args.put("lowerSetpoint", lower.toMap());
         }
         if(upper != null) {
            args.put("upperSetpoint", upper.toMap());
         }
         builder.withArguments(args.build());
      }

      return builder.build();
   }

   private AlexaService.ExecuteRequest.Builder builder() {
      return AlexaService.ExecuteRequest.builder()
         .withDirective(AlexaInterfaces.ThermostatController.REQUEST_SETTARGETTEMPERATURE);
   }

}


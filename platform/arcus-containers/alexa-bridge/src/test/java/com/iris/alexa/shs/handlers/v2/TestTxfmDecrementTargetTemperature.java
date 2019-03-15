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
package com.iris.alexa.shs.handlers.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.alexa.message.AlexaMessage;
import com.iris.alexa.message.Header;
import com.iris.alexa.message.v2.Appliance;
import com.iris.alexa.message.v2.DoubleValue;
import com.iris.alexa.message.v2.error.ErrorPayloadException;
import com.iris.alexa.message.v2.error.UnwillingToSetValueError;
import com.iris.alexa.message.v2.error.ValueOutOfRangeError;
import com.iris.alexa.message.v2.request.DecrementTargetTemperatureRequest;
import com.iris.alexa.message.v2.response.DecrementTargetTemperatureConfirmation;
import com.iris.alexa.message.v2.response.TemperatureConfirmationPayload;
import com.iris.alexa.shs.ShsAssertions;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.ThermostatCapability;
import com.iris.messages.service.AlexaService;
import com.iris.util.IrisUUID;

public class TestTxfmDecrementTargetTemperature {

   private AlexaMessage decTargetTemp;
   private Appliance app;

   @Before
   public void setup() {
      Header h = Header.v2(IrisUUID.randomUUID().toString(), "DecrementTargetTemperatureRequest", "Alexa.ConnectedHome.Control");

      app = new Appliance();
      app.setApplianceId(Address.platformDriverAddress(IrisUUID.randomUUID()).getRepresentation());

      DecrementTargetTemperatureRequest payload = new DecrementTargetTemperatureRequest();
      payload.setDeltaTemperature(new DoubleValue(2.00));
      payload.setAccessToken("token");
      payload.setAppliance(app);

      decTargetTemp = new AlexaMessage(h, payload);
   }

   @Test
   public void testDecrementTargetTemperature() {
      PlatformMessage platMsg = TxfmTestUtil.txfmReq(decTargetTemp);

      ShsAssertions.assertExecuteRequest(
         platMsg,
         app.getApplianceId(),
         "DecrementTargetTemperatureRequest",
         ImmutableMap.of("deltaTemperature", 2.00),
         null,
         false
      );
   }

   @Test
   public void testSuccess() {
      MessageBody body = ThermostatCapability.DecrementIdealTemperatureResponse.builder()
         .withHvacmode(ThermostatCapability.HVACMODE_COOL)
         .withIdealTempSet(21.11)
         .withPrevIdealTemp(20.00)
         .withMaxSetPoint(35.00)
         .withMinSetPoint(1.67)
         .withResult(true)
         .build();

      MessageBody resp = AlexaService.ExecuteResponse.builder()
         .withPayload(body.getAttributes())
         .build();

      AlexaMessage response = TxfmTestUtil.txfmResponse(decTargetTemp, resp);
      ShsAssertions.assertCommonResponseHeader(decTargetTemp, response, "DecrementTargetTemperatureConfirmation", "2");
      assertTrue(response.getPayload() instanceof DecrementTargetTemperatureConfirmation);
      DecrementTargetTemperatureConfirmation confirm = (DecrementTargetTemperatureConfirmation) response.getPayload();
      TemperatureConfirmationPayload.PreviousState previousState = confirm.getPreviousState();
      assertEquals(ThermostatCapability.HVACMODE_COOL, previousState.getMode().getValue());
      assertEquals(20.00, previousState.getTargetTemperature().getValue(), .001);
      assertEquals(ThermostatCapability.HVACMODE_COOL, confirm.getTemperatureMode().getValue());
      assertEquals(21.11, confirm.getTargetTemperature().getValue(), .001);
   }

   @Test
   public void testOutOfRange() {
      MessageBody body = ThermostatCapability.DecrementIdealTemperatureResponse.builder()
         .withHvacmode(ThermostatCapability.HVACMODE_COOL)
         .withIdealTempSet(21.11)
         .withPrevIdealTemp(20.00)
         .withMaxSetPoint(35.00)
         .withMinSetPoint(1.67)
         .withResult(false)
         .build();

      MessageBody resp = AlexaService.ExecuteResponse.builder()
         .withPayload(body.getAttributes())
         .build();

      try {
         TxfmTestUtil.txfmResponse(decTargetTemp, resp);
      } catch(ErrorPayloadException epe) {
         assertTrue(epe.getPayload() instanceof ValueOutOfRangeError);
         ValueOutOfRangeError e = (ValueOutOfRangeError) epe.getPayload();
         assertEquals(1.67, e.getMinimumValue(), .001);
         assertEquals(35.00, e.getMaximumValue(), .001);
      }
   }

   @Test
   public void testThermostatOff() {
      MessageBody body = ThermostatCapability.DecrementIdealTemperatureResponse.builder()
         .withHvacmode(ThermostatCapability.HVACMODE_OFF)
         .withIdealTempSet(21.11)
         .withPrevIdealTemp(20.00)
         .withMaxSetPoint(35.00)
         .withMinSetPoint(1.67)
         .withResult(false)
         .build();

      MessageBody resp = AlexaService.ExecuteResponse.builder()
         .withPayload(body.getAttributes())
         .build();

      try {
         TxfmTestUtil.txfmResponse(decTargetTemp, resp);
      } catch(ErrorPayloadException epe) {
         assertTrue(epe.getPayload() instanceof UnwillingToSetValueError);
         UnwillingToSetValueError e = (UnwillingToSetValueError) epe.getPayload();
         assertEquals("ThermostatIsOff", e.getErrorInfo().getCode());
      }
   }
}


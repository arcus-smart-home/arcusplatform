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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.alexa.AlexaInterfaces;
import com.iris.alexa.message.AlexaMessage;
import com.iris.alexa.message.Header;
import com.iris.alexa.message.v2.Appliance;
import com.iris.alexa.message.v2.error.DriverInternalError;
import com.iris.alexa.message.v2.error.ErrorPayloadException;
import com.iris.alexa.message.v2.request.GetTargetTemperatureRequest;
import com.iris.alexa.message.v2.response.GetTargetTemperatureResponse;
import com.iris.alexa.shs.ShsAssertions;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.ThermostatCapability;
import com.iris.messages.service.AlexaService;
import com.iris.messages.type.AlexaPropertyReport;
import com.iris.messages.type.AlexaTemperature;
import com.iris.util.IrisUUID;

public class TestTxfmGetTargetTemperature {

   private AlexaMessage getTargetTemp;
   private Appliance app;

   @Before
   public void setup() {
      Header h = Header.v2(IrisUUID.randomUUID().toString(), "GetTargetTemperatureRequest", "Alexa.ConnectedHome.Query");

      app = new Appliance();
      app.setApplianceId(Address.platformDriverAddress(IrisUUID.randomUUID()).getRepresentation());

      GetTargetTemperatureRequest payload = new GetTargetTemperatureRequest();
      payload.setAccessToken("token");
      payload.setAppliance(app);

      getTargetTemp = new AlexaMessage(h, payload);
   }

   @Test
   public void testGetTargetTemperature() {
      PlatformMessage platMsg = TxfmTestUtil.txfmReq(getTargetTemp);

      ShsAssertions.assertExecuteRequest(
         platMsg,
         app.getApplianceId(),
         AlexaInterfaces.REQUEST_REPORTSTATE,
         ImmutableMap.of(),
         null,
         false
      );
   }

   @Test
   public void testSuccessResponseOff() {
      AlexaPropertyReport mode = new AlexaPropertyReport();
      mode.setValue(ThermostatCapability.HVACMODE_OFF);
      mode.setUncertaintyInMilliseconds(0L);
      mode.setTimeOfSample(ZonedDateTime.now(ZoneOffset.UTC).toString());
      mode.setName(AlexaInterfaces.ThermostatController.PROP_THERMOSTATMODE);
      mode.setNamespace(AlexaInterfaces.ThermostatController.NAMESPACE);

      MessageBody resp = AlexaService.ExecuteResponse.builder()
         .withProperties(ImmutableList.of(mode.toMap()))
         .build();

      AlexaMessage response = TxfmTestUtil.txfmResponse(getTargetTemp, resp);
      ShsAssertions.assertCommonResponseHeader(getTargetTemp, response, "GetTargetTemperatureResponse", "2");
      assertTrue(response.getPayload() instanceof GetTargetTemperatureResponse);
      GetTargetTemperatureResponse confirmation = (GetTargetTemperatureResponse) response.getPayload();
      assertNull(confirmation.getCoolingTargetTemperature());
      assertNull(confirmation.getHeatingTargetTemperature());
      assertNull(confirmation.getTargetTemperature());
      assertEquals(ThermostatCapability.HVACMODE_OFF, confirmation.getTemperatureMode().getValue());
   }

   @Test
   public void testSuccessResponseCool() {
      AlexaPropertyReport mode = new AlexaPropertyReport();
      mode.setValue(ThermostatCapability.HVACMODE_COOL);
      mode.setUncertaintyInMilliseconds(0L);
      mode.setTimeOfSample(ZonedDateTime.now(ZoneOffset.UTC).toString());
      mode.setName(AlexaInterfaces.ThermostatController.PROP_THERMOSTATMODE);
      mode.setNamespace(AlexaInterfaces.ThermostatController.NAMESPACE);

      AlexaTemperature temp = new AlexaTemperature();
      temp.setValue(21.11);
      temp.setScale(AlexaTemperature.SCALE_CELSIUS);

      AlexaPropertyReport target = new AlexaPropertyReport();
      target.setValue(temp.toMap());
      target.setUncertaintyInMilliseconds(0L);
      target.setTimeOfSample(ZonedDateTime.now(ZoneOffset.UTC).toString());
      target.setName(AlexaInterfaces.ThermostatController.PROP_TARGETSETPOINT);
      target.setNamespace(AlexaInterfaces.ThermostatController.NAMESPACE);

      MessageBody resp = AlexaService.ExecuteResponse.builder()
         .withProperties(ImmutableList.of(mode.toMap(), target.toMap()))
         .build();

      AlexaMessage response = TxfmTestUtil.txfmResponse(getTargetTemp, resp);
      ShsAssertions.assertCommonResponseHeader(getTargetTemp, response, "GetTargetTemperatureResponse", "2");
      assertTrue(response.getPayload() instanceof GetTargetTemperatureResponse);
      GetTargetTemperatureResponse confirmation = (GetTargetTemperatureResponse) response.getPayload();
      assertNull(confirmation.getHeatingTargetTemperature());
      assertNull(confirmation.getTargetTemperature());
      assertEquals(ThermostatCapability.HVACMODE_COOL, confirmation.getTemperatureMode().getValue());
      assertEquals(21.11, confirmation.getCoolingTargetTemperature().getValue(), .001);
   }

   @Test
   public void testSuccessResponseHeat() {
      AlexaPropertyReport mode = new AlexaPropertyReport();
      mode.setValue(ThermostatCapability.HVACMODE_HEAT);
      mode.setUncertaintyInMilliseconds(0L);
      mode.setTimeOfSample(ZonedDateTime.now(ZoneOffset.UTC).toString());
      mode.setName(AlexaInterfaces.ThermostatController.PROP_THERMOSTATMODE);
      mode.setNamespace(AlexaInterfaces.ThermostatController.NAMESPACE);

      AlexaTemperature temp = new AlexaTemperature();
      temp.setValue(21.11);
      temp.setScale(AlexaTemperature.SCALE_CELSIUS);

      AlexaPropertyReport target = new AlexaPropertyReport();
      target.setValue(temp.toMap());
      target.setUncertaintyInMilliseconds(0L);
      target.setTimeOfSample(ZonedDateTime.now(ZoneOffset.UTC).toString());
      target.setName(AlexaInterfaces.ThermostatController.PROP_TARGETSETPOINT);
      target.setNamespace(AlexaInterfaces.ThermostatController.NAMESPACE);

      MessageBody resp = AlexaService.ExecuteResponse.builder()
         .withProperties(ImmutableList.of(mode.toMap(), target.toMap()))
         .build();

      AlexaMessage response = TxfmTestUtil.txfmResponse(getTargetTemp, resp);
      ShsAssertions.assertCommonResponseHeader(getTargetTemp, response, "GetTargetTemperatureResponse", "2");
      assertTrue(response.getPayload() instanceof GetTargetTemperatureResponse);
      GetTargetTemperatureResponse confirmation = (GetTargetTemperatureResponse) response.getPayload();
      assertNull(confirmation.getCoolingTargetTemperature());
      assertNull(confirmation.getTargetTemperature());
      assertEquals(ThermostatCapability.HVACMODE_HEAT, confirmation.getTemperatureMode().getValue());
      assertEquals(21.11, confirmation.getHeatingTargetTemperature().getValue(), .001);
   }

   @Test
   public void testSuccessResponseAuto() {
      AlexaPropertyReport mode = new AlexaPropertyReport();
      mode.setValue(ThermostatCapability.HVACMODE_AUTO);
      mode.setUncertaintyInMilliseconds(0L);
      mode.setTimeOfSample(ZonedDateTime.now(ZoneOffset.UTC).toString());
      mode.setName(AlexaInterfaces.ThermostatController.PROP_THERMOSTATMODE);
      mode.setNamespace(AlexaInterfaces.ThermostatController.NAMESPACE);

      AlexaTemperature upper = new AlexaTemperature();
      upper.setValue(21.11);
      upper.setScale(AlexaTemperature.SCALE_CELSIUS);

      AlexaPropertyReport upperSp = new AlexaPropertyReport();
      upperSp.setValue(upper.toMap());
      upperSp.setUncertaintyInMilliseconds(0L);
      upperSp.setTimeOfSample(ZonedDateTime.now(ZoneOffset.UTC).toString());
      upperSp.setName(AlexaInterfaces.ThermostatController.PROP_UPPERSETPOINT);
      upperSp.setNamespace(AlexaInterfaces.ThermostatController.NAMESPACE);

      AlexaTemperature lower = new AlexaTemperature();
      lower.setValue(19.00);
      lower.setScale(AlexaTemperature.SCALE_CELSIUS);

      AlexaPropertyReport lowerSp = new AlexaPropertyReport();
      lowerSp.setValue(lower.toMap());
      lowerSp.setUncertaintyInMilliseconds(0L);
      lowerSp.setTimeOfSample(ZonedDateTime.now(ZoneOffset.UTC).toString());
      lowerSp.setName(AlexaInterfaces.ThermostatController.PROP_LOWERSETPOINT);
      lowerSp.setNamespace(AlexaInterfaces.ThermostatController.NAMESPACE);

      MessageBody resp = AlexaService.ExecuteResponse.builder()
         .withProperties(ImmutableList.of(mode.toMap(), upperSp.toMap(), lowerSp.toMap()))
         .build();

      AlexaMessage response = TxfmTestUtil.txfmResponse(getTargetTemp, resp);
      ShsAssertions.assertCommonResponseHeader(getTargetTemp, response, "GetTargetTemperatureResponse", "2");
      assertTrue(response.getPayload() instanceof GetTargetTemperatureResponse);
      GetTargetTemperatureResponse confirmation = (GetTargetTemperatureResponse) response.getPayload();
      assertNull(confirmation.getTargetTemperature());
      assertEquals(ThermostatCapability.HVACMODE_AUTO, confirmation.getTemperatureMode().getValue());
      assertEquals(21.11, confirmation.getCoolingTargetTemperature().getValue(), .001);
      assertEquals(19.00, confirmation.getHeatingTargetTemperature().getValue(), .001);
   }

   @Test
   public void testNullPropertiesResponse() {
      try {
         TxfmTestUtil.txfmResponse(getTargetTemp, AlexaService.ExecuteResponse.builder().build());
      } catch(ErrorPayloadException epe) {
         assertTrue(epe.getPayload() instanceof DriverInternalError);
         DriverInternalError die = (DriverInternalError) epe.getPayload();
         assertEquals("Alexa.ConnectedHome.Control", die.getNamespace());
      }
   }

   @Test
   public void testEmptyPropertyResponse() {
      MessageBody resp = AlexaService.ExecuteResponse.builder()
         .withProperties(ImmutableList.of())
         .build();
      try {
         TxfmTestUtil.txfmResponse(getTargetTemp, resp);
      } catch(ErrorPayloadException epe) {
         assertTrue(epe.getPayload() instanceof DriverInternalError);
         DriverInternalError die = (DriverInternalError) epe.getPayload();
         assertEquals("Alexa.ConnectedHome.Control", die.getNamespace());
      }
   }

   @Test
   public void testFailCoolMissingTargetSp() {
      AlexaPropertyReport mode = new AlexaPropertyReport();
      mode.setValue(ThermostatCapability.HVACMODE_COOL);
      mode.setUncertaintyInMilliseconds(0L);
      mode.setTimeOfSample(ZonedDateTime.now(ZoneOffset.UTC).toString());
      mode.setName(AlexaInterfaces.ThermostatController.PROP_THERMOSTATMODE);
      mode.setNamespace(AlexaInterfaces.ThermostatController.NAMESPACE);

      MessageBody resp = AlexaService.ExecuteResponse.builder()
         .withProperties(ImmutableList.of(mode.toMap()))
         .build();

      try {
         TxfmTestUtil.txfmResponse(getTargetTemp, resp);
      } catch(ErrorPayloadException epe) {
         assertTrue(epe.getPayload() instanceof DriverInternalError);
         DriverInternalError die = (DriverInternalError) epe.getPayload();
         assertEquals("Alexa.ConnectedHome.Control", die.getNamespace());
      }
   }

   @Test
   public void testFailHeatMissingTargetSp() {
      AlexaPropertyReport mode = new AlexaPropertyReport();
      mode.setValue(ThermostatCapability.HVACMODE_HEAT);
      mode.setUncertaintyInMilliseconds(0L);
      mode.setTimeOfSample(ZonedDateTime.now(ZoneOffset.UTC).toString());
      mode.setName(AlexaInterfaces.ThermostatController.PROP_THERMOSTATMODE);
      mode.setNamespace(AlexaInterfaces.ThermostatController.NAMESPACE);

      MessageBody resp = AlexaService.ExecuteResponse.builder()
         .withProperties(ImmutableList.of(mode.toMap()))
         .build();

      try {
         TxfmTestUtil.txfmResponse(getTargetTemp, resp);
      } catch(ErrorPayloadException epe) {
         assertTrue(epe.getPayload() instanceof DriverInternalError);
         DriverInternalError die = (DriverInternalError) epe.getPayload();
         assertEquals("Alexa.ConnectedHome.Control", die.getNamespace());
      }
   }

   @Test
   public void testFailAutoMissingLowerSp() {
      AlexaPropertyReport mode = new AlexaPropertyReport();
      mode.setValue(ThermostatCapability.HVACMODE_AUTO);
      mode.setUncertaintyInMilliseconds(0L);
      mode.setTimeOfSample(ZonedDateTime.now(ZoneOffset.UTC).toString());
      mode.setName(AlexaInterfaces.ThermostatController.PROP_THERMOSTATMODE);
      mode.setNamespace(AlexaInterfaces.ThermostatController.NAMESPACE);

      AlexaTemperature upper = new AlexaTemperature();
      upper.setValue(21.11);
      upper.setScale(AlexaTemperature.SCALE_CELSIUS);

      AlexaPropertyReport upperSp = new AlexaPropertyReport();
      upperSp.setValue(upper.toMap());
      upperSp.setUncertaintyInMilliseconds(0L);
      upperSp.setTimeOfSample(ZonedDateTime.now(ZoneOffset.UTC).toString());
      upperSp.setName(AlexaInterfaces.ThermostatController.PROP_UPPERSETPOINT);
      upperSp.setNamespace(AlexaInterfaces.ThermostatController.NAMESPACE);

      MessageBody resp = AlexaService.ExecuteResponse.builder()
         .withProperties(ImmutableList.of(mode.toMap(), upperSp.toMap()))
         .build();

      try {
         TxfmTestUtil.txfmResponse(getTargetTemp, resp);
      } catch(ErrorPayloadException epe) {
         assertTrue(epe.getPayload() instanceof DriverInternalError);
         DriverInternalError die = (DriverInternalError) epe.getPayload();
         assertEquals("Alexa.ConnectedHome.Control", die.getNamespace());
      }
   }

   @Test
   public void testFailAutoMissingUpperSp() {
      AlexaPropertyReport mode = new AlexaPropertyReport();
      mode.setValue(ThermostatCapability.HVACMODE_AUTO);
      mode.setUncertaintyInMilliseconds(0L);
      mode.setTimeOfSample(ZonedDateTime.now(ZoneOffset.UTC).toString());
      mode.setName(AlexaInterfaces.ThermostatController.PROP_THERMOSTATMODE);
      mode.setNamespace(AlexaInterfaces.ThermostatController.NAMESPACE);

      AlexaTemperature lower = new AlexaTemperature();
      lower.setValue(19.00);
      lower.setScale(AlexaTemperature.SCALE_CELSIUS);

      AlexaPropertyReport lowerSp = new AlexaPropertyReport();
      lowerSp.setValue(lower.toMap());
      lowerSp.setUncertaintyInMilliseconds(0L);
      lowerSp.setTimeOfSample(ZonedDateTime.now(ZoneOffset.UTC).toString());
      lowerSp.setName(AlexaInterfaces.ThermostatController.PROP_LOWERSETPOINT);
      lowerSp.setNamespace(AlexaInterfaces.ThermostatController.NAMESPACE);


      MessageBody resp = AlexaService.ExecuteResponse.builder()
         .withProperties(ImmutableList.of(mode.toMap(), lowerSp.toMap()))
         .build();

      try {
         TxfmTestUtil.txfmResponse(getTargetTemp, resp);
      } catch(ErrorPayloadException epe) {
         assertTrue(epe.getPayload() instanceof DriverInternalError);
         DriverInternalError die = (DriverInternalError) epe.getPayload();
         assertEquals("Alexa.ConnectedHome.Control", die.getNamespace());
      }
   }
}


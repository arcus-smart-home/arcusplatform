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
import com.iris.alexa.message.v2.request.GetTemperatureReadingRequest;
import com.iris.alexa.message.v2.response.GetTemperatureReadingResponse;
import com.iris.alexa.shs.ShsAssertions;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.service.AlexaService;
import com.iris.messages.type.AlexaPropertyReport;
import com.iris.messages.type.AlexaTemperature;
import com.iris.util.IrisUUID;

public class TestTxfmGetTemperatureReading {

   private AlexaMessage getTemp;
   private Appliance app;

   @Before
   public void setup() {
      Header h = Header.v2(IrisUUID.randomUUID().toString(), "GetTemperatureReadingRequest", "Alexa.ConnectedHome.Query");

      app = new Appliance();
      app.setApplianceId(Address.platformDriverAddress(IrisUUID.randomUUID()).getRepresentation());

      GetTemperatureReadingRequest payload = new GetTemperatureReadingRequest();
      payload.setAccessToken("token");
      payload.setAppliance(app);

      getTemp = new AlexaMessage(h, payload);
   }

   @Test
   public void testGetTemperatureReading() {
      PlatformMessage platMsg = TxfmTestUtil.txfmReq(getTemp);

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
   public void testSuccessResponse() {
      AlexaTemperature temp = new AlexaTemperature();
      temp.setValue(21.11);
      temp.setScale(AlexaTemperature.SCALE_CELSIUS);

      AlexaPropertyReport temperature = new AlexaPropertyReport();
      temperature.setValue(temp.toMap());
      temperature.setUncertaintyInMilliseconds(0L);
      temperature.setTimeOfSample(ZonedDateTime.now(ZoneOffset.UTC).toString());
      temperature.setName(AlexaInterfaces.TemperatureSensor.PROP_TEMPERATURE);
      temperature.setNamespace(AlexaInterfaces.TemperatureSensor.NAMESPACE);

      MessageBody resp = AlexaService.ExecuteResponse.builder()
         .withProperties(ImmutableList.of(temperature.toMap()))
         .build();

      AlexaMessage response = TxfmTestUtil.txfmResponse(getTemp, resp);
      ShsAssertions.assertCommonResponseHeader(getTemp, response, "GetTemperatureReadingResponse", "2");
      assertTrue(response.getPayload() instanceof GetTemperatureReadingResponse);
      GetTemperatureReadingResponse confirmation = (GetTemperatureReadingResponse) response.getPayload();
      assertEquals(21.11, confirmation.getTemperatureReading().getValue(), .001);
   }

   @Test
   public void testNullPropertiesResponse() {
      try {
         TxfmTestUtil.txfmResponse(getTemp, AlexaService.ExecuteResponse.builder().build());
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
         TxfmTestUtil.txfmResponse(getTemp, resp);
      } catch(ErrorPayloadException epe) {
         assertTrue(epe.getPayload() instanceof DriverInternalError);
         DriverInternalError die = (DriverInternalError) epe.getPayload();
         assertEquals("Alexa.ConnectedHome.Control", die.getNamespace());
      }
   }
}


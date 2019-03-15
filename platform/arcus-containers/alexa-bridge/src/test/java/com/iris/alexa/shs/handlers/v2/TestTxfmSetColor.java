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
import com.iris.alexa.message.v2.Color;
import com.iris.alexa.message.v2.error.DriverInternalError;
import com.iris.alexa.message.v2.error.ErrorPayloadException;
import com.iris.alexa.message.v2.request.SetColorRequest;
import com.iris.alexa.message.v2.response.SetColorConfirmation;
import com.iris.alexa.shs.ShsAssertions;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.service.AlexaService;
import com.iris.messages.type.AlexaColor;
import com.iris.messages.type.AlexaPropertyReport;
import com.iris.util.IrisUUID;

public class TestTxfmSetColor {

   private AlexaMessage setColor;
   private Appliance app;

   @Before
   public void setup() {
      Header h = Header.v2(IrisUUID.randomUUID().toString(), "SetColorRequest", "Alexa.ConnectedHome.Control");

      app = new Appliance();
      app.setApplianceId(Address.platformDriverAddress(IrisUUID.randomUUID()).getRepresentation());

      SetColorRequest payload = new SetColorRequest();
      Color c = new Color();
      c.setHue(0.0);
      c.setSaturation(1.00);
      c.setBrightness(1.00);
      payload.setColor(c);
      payload.setAccessToken("token");
      payload.setAppliance(app);

      setColor = new AlexaMessage(h, payload);
   }

   @Test
   public void testSetColor() {
      PlatformMessage platMsg = TxfmTestUtil.txfmReq(setColor);

      AlexaColor color = new AlexaColor();
      color.setBrightness(1.0);
      color.setHue(0.0);
      color.setSaturation(1.0);

      ShsAssertions.assertExecuteRequest(
         platMsg,
         app.getApplianceId(),
         AlexaInterfaces.ColorController.REQUEST_SETCOLOR,
         ImmutableMap.of(AlexaInterfaces.ColorController.PROP_COLOR, color.toMap()),
         null,
         false
      );
   }

   @Test
   public void testSuccessResponse() {
      AlexaColor color = new AlexaColor();
      color.setBrightness(1.0);
      color.setHue(0.0);
      color.setSaturation(1.0);

      AlexaPropertyReport colorRpt = new AlexaPropertyReport();
      colorRpt.setValue(color.toMap());
      colorRpt.setUncertaintyInMilliseconds(0L);
      colorRpt.setTimeOfSample(ZonedDateTime.now(ZoneOffset.UTC).toString());
      colorRpt.setName(AlexaInterfaces.ColorController.PROP_COLOR);
      colorRpt.setNamespace(AlexaInterfaces.ColorController.NAMESPACE);

      MessageBody resp = AlexaService.ExecuteResponse.builder()
         .withProperties(ImmutableList.of(colorRpt.toMap()))
         .build();

      AlexaMessage response = TxfmTestUtil.txfmResponse(setColor, resp);
      ShsAssertions.assertCommonResponseHeader(setColor, response, "SetColorConfirmation", "2");
      assertTrue(response.getPayload() instanceof SetColorConfirmation);
      SetColorConfirmation confirmation = (SetColorConfirmation) response.getPayload();
      Color confirmedColor = (Color) confirmation.getAchievedState().get("color");
      assertEquals(1.0, confirmedColor.getBrightness(), .001);
      assertEquals(1.0, confirmedColor.getSaturation(), .001);
      assertEquals(0.0, confirmedColor.getHue(), .001);
   }

   @Test
   public void testNullPropertiesResponse() {
      try {
         TxfmTestUtil.txfmResponse(setColor, AlexaService.ExecuteResponse.builder().build());
      } catch(ErrorPayloadException epe) {
         assertTrue(epe.getPayload() instanceof DriverInternalError);
         DriverInternalError die = (DriverInternalError) epe.getPayload();
         assertEquals("Alexa.ConnectedHome.Control", die.getNamespace());
      }
   }

   @Test
   public void testEmptyPropertiesResponse() {
      MessageBody resp = AlexaService.ExecuteResponse.builder()
         .withProperties(ImmutableList.of())
         .build();
      try {
         TxfmTestUtil.txfmResponse(setColor, resp);
      } catch(ErrorPayloadException epe) {
         assertTrue(epe.getPayload() instanceof DriverInternalError);
         DriverInternalError die = (DriverInternalError) epe.getPayload();
         assertEquals("Alexa.ConnectedHome.Control", die.getNamespace());
      }
   }
}


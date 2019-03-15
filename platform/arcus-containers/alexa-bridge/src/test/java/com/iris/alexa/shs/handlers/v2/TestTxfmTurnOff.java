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

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.alexa.AlexaInterfaces;
import com.iris.alexa.message.AlexaMessage;
import com.iris.alexa.message.Header;
import com.iris.alexa.message.v2.Appliance;
import com.iris.alexa.message.v2.request.TurnOffRequest;
import com.iris.alexa.message.v2.response.TurnOffConfirmation;
import com.iris.alexa.shs.ShsAssertions;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.service.AlexaService;
import com.iris.util.IrisUUID;

public class TestTxfmTurnOff {

   private AlexaMessage turnOffMessage;
   private Appliance app;

   @Before
   public void setup() {
      Header h = Header.v2(IrisUUID.randomUUID().toString(), "TurnOffRequest", "Alexa.ConnectedHome.Control");

      app = new Appliance();
      app.setApplianceId(Address.platformDriverAddress(IrisUUID.randomUUID()).getRepresentation());

      TurnOffRequest payload = new TurnOffRequest();
      payload.setAccessToken("token");
      payload.setAppliance(app);

      turnOffMessage = new AlexaMessage(h, payload);
   }

   @Test
   public void testTurnOff() {
      PlatformMessage platMsg = TxfmTestUtil.txfmReq(turnOffMessage);

      ShsAssertions.assertExecuteRequest(
         platMsg,
         app.getApplianceId(),
         AlexaInterfaces.PowerController.REQUEST_TURNOFF,
         ImmutableMap.of(),
         null,
         false
      );
   }

   @Test
   public void testConfirmation() {
      AlexaMessage response = TxfmTestUtil.txfmResponse(turnOffMessage, AlexaService.ExecuteResponse.builder().build());
      ShsAssertions.assertCommonResponseHeader(turnOffMessage, response, "TurnOffConfirmation", "2");
      assertTrue(response.getPayload() instanceof TurnOffConfirmation);
   }
}


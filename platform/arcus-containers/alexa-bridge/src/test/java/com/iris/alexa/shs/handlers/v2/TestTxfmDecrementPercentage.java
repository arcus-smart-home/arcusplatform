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
import com.iris.alexa.message.v2.DoubleValue;
import com.iris.alexa.message.v2.request.DecrementPercentageRequest;
import com.iris.alexa.message.v2.response.DecrementPercentageConfirmation;
import com.iris.alexa.shs.ShsAssertions;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.FanCapability;
import com.iris.messages.service.AlexaService;
import com.iris.util.IrisUUID;

public class TestTxfmDecrementPercentage {

   private Header decPercentageHeader;

   @Before
   public void setup() {
      decPercentageHeader = Header.v2(IrisUUID.randomUUID().toString(), "DecrementPercentageRequest", "Alexa.ConnectedHome.Control");
   }

   @Test
   public void testDecrementPercentageLight() {
      Appliance app = new Appliance();
      app.setApplianceId(Address.platformDriverAddress(IrisUUID.randomUUID()).getRepresentation());

      DecrementPercentageRequest payload = new DecrementPercentageRequest();
      payload.setAccessToken("token");
      payload.setAppliance(app);
      payload.setDeltaPercentage(new DoubleValue(25.0));

      AlexaMessage msg = new AlexaMessage(decPercentageHeader, payload);
      PlatformMessage platMsg = TxfmTestUtil.txfmReq(msg);

      ShsAssertions.assertExecuteRequest(
         platMsg,
         app.getApplianceId(),
         AlexaInterfaces.BrightnessController.REQUEST_ADJUSTBRIGHTNESS,
         ImmutableMap.of(AlexaInterfaces.BrightnessController.ARG_BRIGHTNESSDELTA, -25),
         null,
         false
      );
   }

   @Test
   public void testDecrementPercentageFan() {
      Appliance app = new Appliance();
      app.setApplianceId(Address.platformDriverAddress(IrisUUID.randomUUID()).getRepresentation());
      app.setAdditionalApplianceDetails(ImmutableMap.of(FanCapability.ATTR_MAXSPEED, "3"));

      DecrementPercentageRequest payload = new DecrementPercentageRequest();
      payload.setAccessToken("token");
      payload.setAppliance(app);
      payload.setDeltaPercentage(new DoubleValue(25.0));

      AlexaMessage msg = new AlexaMessage(decPercentageHeader, payload);
      PlatformMessage platMsg = TxfmTestUtil.txfmReq(msg);

      ShsAssertions.assertExecuteRequest(
         platMsg,
         app.getApplianceId(),
         AlexaInterfaces.PercentageController.REQUEST_ADJUSTPERCENTAGE,
         ImmutableMap.of(AlexaInterfaces.PercentageController.ARG_PERCENTAGEDELTA, -25),
         null,
         false
      );
   }

   @Test
   public void testConfirmation() {
      Appliance app = new Appliance();
      app.setApplianceId(Address.platformDriverAddress(IrisUUID.randomUUID()).getRepresentation());

      DecrementPercentageRequest payload = new DecrementPercentageRequest();
      payload.setAccessToken("token");
      payload.setAppliance(app);
      payload.setDeltaPercentage(new DoubleValue(25.0));

      AlexaMessage msg = new AlexaMessage(decPercentageHeader, payload);
      AlexaMessage response = TxfmTestUtil.txfmResponse(msg, AlexaService.ExecuteResponse.builder().build());
      ShsAssertions.assertCommonResponseHeader(msg, response, "DecrementPercentageConfirmation", "2");
      assertTrue(response.getPayload() instanceof DecrementPercentageConfirmation);
   }
}


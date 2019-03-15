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

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.iris.alexa.message.AlexaMessage;
import com.iris.alexa.message.Header;
import com.iris.alexa.message.v2.Appliance;
import com.iris.alexa.message.v2.request.DiscoverAppliancesRequest;
import com.iris.alexa.message.v2.response.DiscoverAppliancesResponse;
import com.iris.alexa.shs.ShsAssertions;
import com.iris.alexa.shs.ShsFixtures;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.service.AlexaService;
import com.iris.messages.type.AlexaEndpoint;
import com.iris.util.IrisUUID;

public class TestTxfmDiscoverAppliances {

   private AlexaMessage discoverMessage;

   @Before
   public void setup() {
      Header h = Header.v2(IrisUUID.randomUUID().toString(), "DiscoverAppliancesRequest", "Alexa.ConnectedHome.Discovery");
      DiscoverAppliancesRequest req = new DiscoverAppliancesRequest();
      req.setAccessToken("token");
      discoverMessage = new AlexaMessage(h, req);
   }

   @Test
   public void testDiscoverAppliancesRequest() {
      PlatformMessage platMsg = TxfmTestUtil.txfmReq(discoverMessage);
      assertEquals(AlexaService.DiscoverRequest.NAME, platMsg.getMessageType());
   }

   @Test
   public void testDiscoverAppliancesRequestNullEndpoints() {
      MessageBody body = AlexaService.DiscoverResponse.builder().build();
      AlexaMessage response = TxfmTestUtil.txfmResponse(discoverMessage, body);
      ShsAssertions.assertCommonResponseHeader(discoverMessage, response, "DiscoverAppliancesResponse", "2");
      assertPayload(response, ImmutableList.of());
   }

   @Test
   public void testDiscoverAppliancesRequestEmptyEndpoints() {
      MessageBody body = AlexaService.DiscoverResponse.builder().withEndpoints(ImmutableList.of()).build();
      AlexaMessage response = TxfmTestUtil.txfmResponse(discoverMessage, body);
      ShsAssertions.assertCommonResponseHeader(discoverMessage, response, "DiscoverAppliancesResponse", "2");
      assertPayload(response, ImmutableList.of());
   }

   @Test
   public void testDiscoverAppliancesRequestAllEndpointTypes() {

      Appliance expectedColorBulb = createAppliance(
         ShsFixtures.colorBulb,
         ImmutableSet.of(
            "setPercentage", "incrementPercentage", "decrementPercentage",
            "setColor",
            "setColorTemperature", "incrementColorTemperature", "decrementColorTemperature",
            "turnOn", "turnOff"
         ),
         ImmutableSet.of(Appliance.Type.LIGHT)
      );
      expectedColorBulb.setVersion("1");

      Appliance expectedThermostat = createAppliance(
         ShsFixtures.thermostat,
         ImmutableSet.of(
            "getTemperatureReading",
            "setTargetTemperature", "incrementTargetTemperature", "decrementTargetTemperature", "getTargetTemperature"
         ),
         ImmutableSet.of(Appliance.Type.THERMOSTAT)
      );

      Appliance expectedFan = createAppliance(
         ShsFixtures.fan,
         ImmutableSet.of(
            "setPercentage", "incrementPercentage", "decrementPercentage",
            "turnOn", "turnOff"
         ),
         ImmutableSet.of(Appliance.Type.SWITCH)
      );

      Appliance expectedScene = createAppliance(
         ShsFixtures.scene,
         ImmutableSet.of("turnOn"),
         ImmutableSet.of(Appliance.Type.SCENE_TRIGGER)
      );


      Appliance expectedLock = createAppliance(
         ShsFixtures.lock,
         ImmutableSet.of(
            "getLockState", "setLockState"
         ),
         ImmutableSet.of(Appliance.Type.SMARTLOCK)
      );

      MessageBody body = AlexaService.DiscoverResponse.builder()
         .withEndpoints(
            ImmutableList.of(
               ShsFixtures.colorBulb.toMap(),
               ShsFixtures.thermostat.toMap(),
               ShsFixtures.fan.toMap(),
               ShsFixtures.scene.toMap(),
               ShsFixtures.lock.toMap()
            )
         )
         .build();
      AlexaMessage response = TxfmTestUtil.txfmResponse(discoverMessage, body);
      ShsAssertions.assertCommonResponseHeader(discoverMessage, response, "DiscoverAppliancesResponse", "2");
      assertPayload(response, ImmutableList.of(
         expectedColorBulb,
         expectedThermostat,
         expectedFan,
         expectedScene,
         expectedLock
      ));
   }

   private Appliance createAppliance(AlexaEndpoint endpoint, Set<String> actions, Set<Appliance.Type> types) {
      Appliance appliance = new Appliance();
      appliance.setActions(actions);
      appliance.setAdditionalApplianceDetails(endpoint.getCookie());
      appliance.setApplianceId(endpoint.getEndpointId());
      appliance.setApplianceTypes(types);
      appliance.setFriendlyDescription(endpoint.getDescription());
      appliance.setFriendlyName(endpoint.getFriendlyName());
      appliance.setManufacturerName(endpoint.getManufacturerName());
      appliance.setModelName(endpoint.getModel());
      appliance.setReachable(endpoint.getOnline());
      appliance.setVersion("1");
      return appliance;
   }

   private void assertPayload(AlexaMessage msg, List<Appliance> expected) {
      assertTrue(msg.getPayload() instanceof DiscoverAppliancesResponse);
      DiscoverAppliancesResponse payload = (DiscoverAppliancesResponse) msg.getPayload();
      assertTrue(payload.getDiscoveredAppliances() != null);
      assertEquals(expected, payload.getDiscoveredAppliances());
   }

}


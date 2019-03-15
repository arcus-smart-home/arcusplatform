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
package com.iris.alexa.shs.handlers.v3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.alexa.AlexaInterfaces;
import com.iris.alexa.message.AlexaMessage;
import com.iris.alexa.message.Header;
import com.iris.alexa.shs.ShsAssertions;
import com.iris.alexa.shs.ShsFixtures;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.service.AlexaService;
import com.iris.messages.type.AlexaEndpoint;
import com.iris.util.IrisUUID;

public class TestTxfmDiscovery {

   private AlexaMessage discoverMessage;

   @Before
   public void setup() {
      Header h = Header.v3(IrisUUID.randomUUID().toString(), AlexaInterfaces.Discovery.REQUEST_DISCOVER, AlexaInterfaces.Discovery.NAMESPACE, null);
      discoverMessage = new AlexaMessage(h, ImmutableMap.of("scope", ImmutableMap.of("type", "BearerToken", "token", "token")), null, null);
   }

   @Test
   public void testDiscoverAppliancesRequest() {
      PlatformMessage platMsg = TxfmTestUtil.txfmReq(discoverMessage);
      assertEquals(AlexaService.DiscoverRequest.NAME, platMsg.getMessageType());
   }

   @Test
   public void testDiscoverNullEndpoints() {
      MessageBody body = AlexaService.DiscoverResponse.builder().build();
      AlexaMessage response = TxfmTestUtil.txfmResponse(discoverMessage, body);
      ShsAssertions.assertCommonResponseHeader(discoverMessage, response, AlexaInterfaces.Discovery.RESPONSE_DISCOVER, "3");
      assertPayload(response, ImmutableList.of());
   }

   @Test
   public void testDiscoverEmptyEndpoints() {
      MessageBody body = AlexaService.DiscoverResponse.builder()
         .withEndpoints(ImmutableList.of())
         .build();
      AlexaMessage response = TxfmTestUtil.txfmResponse(discoverMessage, body);
      ShsAssertions.assertCommonResponseHeader(discoverMessage, response, AlexaInterfaces.Discovery.RESPONSE_DISCOVER, "3");
      assertPayload(response, ImmutableList.of());
   }

   @Test
   public void testDiscoverAppliancesRequestAllEndpointTypes() {
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
      ShsAssertions.assertCommonResponseHeader(discoverMessage, response, AlexaInterfaces.Discovery.RESPONSE_DISCOVER, "3");
      assertPayload(response, ImmutableList.of(
         txfmEndpoint(ShsFixtures.colorBulb),
         txfmEndpoint(ShsFixtures.thermostat),
         txfmEndpoint(ShsFixtures.fan),
         txfmEndpoint(ShsFixtures.scene),
         txfmEndpoint(ShsFixtures.lock)
      ));
   }

   private Map<String, Object> txfmEndpoint(AlexaEndpoint endpoint) {
      Map<String, Object> attrs = new HashMap<>(endpoint.toMap());
      attrs.remove(AlexaEndpoint.ATTR_ONLINE);
      attrs.remove(AlexaEndpoint.ATTR_MODEL);
      return attrs;
   }

   private void assertPayload(AlexaMessage msg, List<Map<String, Object>> expected) {
      Map<String, Object> payload = (Map<String, Object>) msg.getPayload();
      assertNotNull(payload);
      List<Map<String, Object>> endpoints = (List<Map<String, Object>>) payload.get("endpoints");
      assertNotNull(endpoints);
      assertEquals(expected, endpoints);
   }

   @Test
   public void testExtractOauthToken() {
      Optional<String> token = Txfm.transformerFor(discoverMessage).extractRequestOauthToken(discoverMessage);
      assertEquals("token", token.get());
   }

   @Test
   public void testExtractOauthTokenNoPayload() {
      AlexaMessage msg = new AlexaMessage(discoverMessage.getHeader(), null, null, null);
      Optional<String> token = Txfm.transformerFor(msg).extractRequestOauthToken(msg);
      assertFalse(token.isPresent());
   }

   @Test
   public void testExtractOauthTokenNoScope() {
      AlexaMessage msg = new AlexaMessage(discoverMessage.getHeader(), ImmutableMap.of(), null, null);
      Optional<String> token = Txfm.transformerFor(msg).extractRequestOauthToken(msg);
      assertFalse(token.isPresent());
   }

   @Test
   public void testExtractOauthTokenScopeHasNoToken() {
      Map<String, Object> payload = ImmutableMap.of(
         "scope", ImmutableMap.of("type", "BearerToken")
      );
      AlexaMessage msg = new AlexaMessage(discoverMessage.getHeader(), payload, null, null);
      Optional<String> token = Txfm.transformerFor(msg).extractRequestOauthToken(msg);
      assertFalse(token.isPresent());
   }
}


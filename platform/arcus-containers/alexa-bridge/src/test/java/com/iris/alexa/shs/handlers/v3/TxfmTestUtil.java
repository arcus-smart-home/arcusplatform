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
import static org.junit.Assert.assertNotNull;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.iris.alexa.AlexaInterfaces;
import com.iris.alexa.AlexaUtil;
import com.iris.alexa.message.AlexaMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.service.AlexaService;
import com.iris.messages.service.VoiceService;
import com.iris.messages.type.AlexaPropertyReport;
import com.iris.messages.type.Population;
import com.iris.util.IrisUUID;

enum TxfmTestUtil {
   ;

   static final UUID placeId = IrisUUID.randomUUID();
   static final int ttl = 30;
   static final String population = Population.NAME_GENERAL;

   static PlatformMessage txfmReq(AlexaMessage msg) {
      PlatformMessage platMsg = Txfm.transformerFor(msg).txfmRequest(msg, placeId, population, ttl);
      assertEquals(placeId.toString(), platMsg.getPlaceId());
      assertEquals(30, platMsg.getTimeToLive());
      return platMsg;
   }

   static AlexaMessage txfmResponse(AlexaMessage msg, MessageBody body) {
      PlatformMessage response = createResponse(body, msg.getHeader().getMessageId());
      return Txfm.transformerFor(msg).transformResponse(response, msg.getHeader().getCorrelationToken());
   }

   private static PlatformMessage createResponse(MessageBody body, String corrId) {
      return PlatformMessage.buildMessage(body, Address.platformService(VoiceService.NAMESPACE), AlexaUtil.ADDRESS_BRIDGE)
         .withPlaceId(placeId)
         .withPopulation(population)
         .withCorrelationId(corrId)
         .create();
   }

   static void assertResponse(AlexaMessage req, AlexaMessage resp, Map<String, Object> payload, List<Map<String, Object>> properties) {
      assertEquals(AlexaInterfaces.RESPONSE_NAME, resp.getHeader().getName());
      assertEquals(AlexaInterfaces.RESPONSE_NAMESPACE, resp.getHeader().getNamespace());
      assertEquals(req.getHeader().getMessageId(), resp.getHeader().getMessageId());
      assertEquals(req.getHeader().getCorrelationToken(), resp.getHeader().getCorrelationToken());
      assertEquals("3", resp.getHeader().getPayloadVersion());
      assertEquals(resp.getPayload(), payload);
      if(properties == null) {
         Map<String, Object> context = resp.getContext();
         if(context != null) {
            List<Map<String, Object>> p = (List<Map<String, Object>>) context.get("properties");
            if(p != null) {
               assertEquals(0, p.size());
            }
         }
         return;
      }
      assertNotNull(resp.getContext());
      Map<String, Object> context = resp.getContext();
      List<Map<String, Object>> actualProperties = (List<Map<String, Object>>) context.get("properties");
      assertNotNull(actualProperties);
      assertEquals(properties, actualProperties);
   }

   static void assertDeferred(AlexaMessage req, AlexaMessage resp) {
      assertEquals(AlexaInterfaces.RESPONSE_DEFERRED, resp.getHeader().getName());
      assertEquals(AlexaInterfaces.RESPONSE_NAMESPACE, resp.getHeader().getNamespace());
      assertEquals(req.getHeader().getMessageId(), resp.getHeader().getMessageId());
      assertEquals(req.getHeader().getCorrelationToken(), resp.getHeader().getCorrelationToken());
      assertEquals("3", resp.getHeader().getPayloadVersion());
   }

   static MessageBody executeResponse(Map<String, Object> payload, List<Map<String, Object>> props, boolean deferred) {
      return AlexaService.ExecuteResponse.builder()
         .withPayload(payload)
         .withProperties(props)
         .withDeferred(deferred)
         .build();
   }

   static AlexaPropertyReport report(String propName, String propNs, Object value) {
      AlexaPropertyReport prop = new AlexaPropertyReport();
      prop.setValue(value);
      prop.setNamespace(propNs);
      prop.setName(propName);
      prop.setTimeOfSample(ZonedDateTime.now().toString());
      prop.setUncertaintyInMilliseconds(0L);
      return prop;
   }
}


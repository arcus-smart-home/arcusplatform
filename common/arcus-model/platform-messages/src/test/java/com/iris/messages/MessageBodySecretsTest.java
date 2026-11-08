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
package com.iris.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.address.Address;
import com.iris.messages.capability.HubZwaveCapability;
import com.iris.messages.capability.PairingSubsystemCapability;

/**
 * Builds each secret message through its generated class, so that renaming one
 * in the capability XML fails here instead of quietly starting to log it.
 */
public class MessageBodySecretsTest {
   private static final String PIN = "12345";
   private static final String DSK = "00000-11111-22222-33333-44444-55555-11111-22222";

   @Test
   public void testHubPinRequestIsRedacted() {
      assertRedacted(
            HubZwaveCapability.S2PinRequiredEvent
               .builder()
               .withRequestId("request")
               .withNode(12)
               .withDsk(DSK)
               .withTimeoutMs(200000L)
               .build()
      );
   }

   @Test
   public void testHubPinAnswerIsRedacted() {
      assertRedacted(
            HubZwaveCapability.SubmitS2PinRequest
               .builder()
               .withRequestId("request")
               .withNode(12)
               .withPin(PIN)
               .build()
      );
   }

   @Test
   public void testClientPinAnswerIsRedacted() {
      assertRedacted(
            PairingSubsystemCapability.SubmitS2PinRequest
               .builder()
               .withRequestId("request")
               .withPin(PIN)
               .build()
      );
   }

   @Test
   public void testDskResponseIsRedacted() {
      assertRedacted(
            PairingSubsystemCapability.GetS2PinRequestResponse
               .builder()
               .withRequestId("request")
               .withDsk(DSK)
               .withExpires(new Date())
               .build()
      );
   }

   @Test
   public void testRedactionOnlyCoversPrinting() {
      Map<String, Object> attributes = ImmutableMap.of(PairingSubsystemCapability.SubmitS2PinRequest.ATTR_PIN, PIN);
      MessageBody body = MessageBody.buildMessage(PairingSubsystemCapability.SubmitS2PinRequest.NAME, attributes);

      assertEquals(attributes, body.getAttributes());
      assertEquals(body, MessageBody.buildMessage(PairingSubsystemCapability.SubmitS2PinRequest.NAME, attributes));
   }

   @Test
   public void testOtherMessagesStillPrintTheirAttributes() {
      MessageBody body = HubZwaveCapability.CancelS2PinRequest.builder().withRequestId("request").withNode(12).build();
      assertTrue(body.toString(), body.toString().contains("request"));

      body = HubZwaveCapability.S2PinFinishedEvent
            .builder()
            .withRequestId("request")
            .withNode(12)
            .withResult(HubZwaveCapability.S2PinFinishedEvent.RESULT_GRANTED)
            .build();
      assertTrue(body.toString(), body.toString().contains("GRANTED"));
   }

   private void assertRedacted(MessageBody body) {
      assertClean(body.toString());

      Address source = Address.platformService("test");
      assertClean(PlatformMessage.buildMessage(body, source, source).create().toString());
      assertClean(ClientMessage.builder().withPayload(body).withSource("test").create().toString());
   }

   private void assertClean(String printed) {
      assertTrue(printed, printed.contains("<redacted>"));
      assertFalse(printed, printed.contains(PIN));
      assertFalse(printed, printed.contains("11111"));
   }
}

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
package com.iris.messages.errors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.junit.Test;

import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;

/**
 * Errors.assertSenderAtPlace is assertPlaceMatches for a sender that may only reach something at
 * its own place: a client or a hub has to send a place header, and a target that isn't at a place
 * is at no sender's place. A service may still send without a place header.
 */
public class AssertSenderAtPlaceTest {
   private static final String PLACE = UUID.randomUUID().toString();
   private static final String OTHER_PLACE = UUID.randomUUID().toString();
   private static final Address TARGET = Address.hubService("ABC-1234", "hub");

   private static final Address CLIENT = Address.clientAddress("android", "session");
   private static final Address HUB = Address.hubService("DEF-5678", "hub");
   private static final Address SERVICE = Address.platformService("subs");

   @Test
   public void testSenderAtThePlacePasses() {
      Errors.assertSenderAtPlace(message(CLIENT, PLACE), PLACE);
      Errors.assertSenderAtPlace(message(HUB, PLACE), PLACE);
      Errors.assertSenderAtPlace(message(SERVICE, PLACE), PLACE);
   }

   @Test
   public void testSenderAtAnotherPlaceIsRefused() {
      assertRefused(message(CLIENT, OTHER_PLACE), PLACE);
      assertRefused(message(HUB, OTHER_PLACE), PLACE);
      assertRefused(message(SERVICE, OTHER_PLACE), PLACE);
   }

   @Test
   public void testClientWithoutAPlaceHeaderIsRefused() {
      assertRefused(message(CLIENT, null), PLACE);
      assertRefused(message(CLIENT, null), null);
   }

   @Test
   public void testHubWithoutAPlaceHeaderIsRefused() {
      assertRefused(message(HUB, null), PLACE);
      assertRefused(message(Address.hubAddress("DEF-5678"), null), PLACE);
      assertRefused(message(HUB, null), null);
   }

   @Test
   public void testServiceWithoutAPlaceHeaderPasses() {
      Errors.assertSenderAtPlace(message(SERVICE, null), PLACE);
      Errors.assertSenderAtPlace(message(SERVICE, null), null);
   }

   @Test
   public void testTargetAtNoPlaceRefusesAPlaceHeader() {
      assertRefused(message(CLIENT, PLACE), null);
      assertRefused(message(HUB, PLACE), null);
      assertRefused(message(SERVICE, PLACE), null);
   }

   private static PlatformMessage message(Address source, String place) {
      return PlatformMessage.buildRequest(MessageBody.ping(), source, TARGET).withPlaceId(place).create();
   }

   private static void assertRefused(PlatformMessage message, String place) {
      try {
         Errors.assertSenderAtPlace(message, place);
         fail("Expected " + message.getSource() + " at place " + message.getPlaceId() + " to be refused for place " + place);
      }
      catch(UnauthorizedRequestException e) {
         // encoded as not found, so the error doesn't say the target exists
         assertEquals(Errors.CODE_NOT_FOUND, e.getCode());
      }
   }
}

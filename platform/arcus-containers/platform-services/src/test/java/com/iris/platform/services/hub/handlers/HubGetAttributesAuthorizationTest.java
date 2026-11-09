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
package com.iris.platform.services.hub.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.errors.UnauthorizedRequestException;
import com.iris.messages.model.Hub;

/**
 * Hub ids are short and guessable, so reading a hub's attributes (its place among them) has to be
 * limited to clients whose active place is the hub's place. The transformer is a strict mock: a
 * refused read that still built the hub's attributes fails the test.
 */
public class HubGetAttributesAuthorizationTest {
   private static final String HUB_ID = "ABC-1234";

   private final UUID hubPlace = UUID.randomUUID();
   private final Map<String, Object> attributes = ImmutableMap.<String, Object>of(HubCapability.ATTR_PLACE, hubPlace.toString());

   private BeanAttributesTransformer<Hub> transformer;
   private HubGetAttributesHandler handler;
   private Hub hub;

   @SuppressWarnings("unchecked")
   @Before
   public void setUp() {
      transformer = EasyMock.createMock(BeanAttributesTransformer.class);
      handler = new HubGetAttributesHandler(transformer);
      hub = new Hub();
      hub.setId(HUB_ID);
      hub.setPlace(hubPlace);
   }

   @After
   public void tearDown() {
      EasyMock.verify(transformer);
   }

   private static PlatformMessage request(Address source, UUID placeHeader) {
      PlatformMessage.Builder builder = PlatformMessage
            .buildRequest(Capability.CMD_GET_ATTRIBUTES, ImmutableMap.<String, Object>of(), source, Address.hubService(HUB_ID, "hub"));
      if(placeHeader != null) {
         builder.withPlaceId(placeHeader);
      }
      return builder.create();
   }

   private static PlatformMessage clientRequest(UUID placeHeader) {
      return request(Address.clientAddress("android", "1"), placeHeader);
   }

   private void assertRefused(PlatformMessage message) {
      EasyMock.replay(transformer);
      try {
         handler.handleRequest(hub, message);
         fail("expected the hub read to be refused");
      }
      catch(UnauthorizedRequestException e) {
         // expected
      }
   }

   private void assertRead(PlatformMessage message) {
      EasyMock.expect(transformer.transform(hub)).andReturn(attributes);
      EasyMock.replay(transformer);

      MessageBody response = handler.handleRequest(hub, message);
      assertEquals(hubPlace.toString(), response.getAttributes().get(HubCapability.ATTR_PLACE));
   }

   @Test
   public void testClientAtAnotherPlaceCannotReadTheHub() {
      assertRefused(clientRequest(UUID.randomUUID()));
   }

   @Test
   public void testClientWithoutAnActivePlaceCannotReadTheHub() {
      assertRefused(clientRequest(null));
   }

   @Test
   public void testClientCannotReadAHubWithoutAPlace() {
      hub.setPlace(null);
      assertRefused(clientRequest(hubPlace));
   }

   @Test
   public void testClientAtTheHubsPlaceReadsTheHub() {
      assertRead(clientRequest(hubPlace));
   }

   @Test
   public void testServicesReadTheHubWithoutAPlace() {
      assertRead(request(Address.platformService("hub-bridge"), null));
   }
}

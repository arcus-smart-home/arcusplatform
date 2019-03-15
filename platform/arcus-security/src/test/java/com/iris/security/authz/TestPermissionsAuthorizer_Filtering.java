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
package com.iris.security.authz;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.google.inject.Inject;
import com.iris.messages.MessageBody;
import com.iris.messages.MessagesModule;
import com.iris.messages.PlatformMessage;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;


@Modules({MessagesModule.class, AuthzModule.class})
public class TestPermissionsAuthorizer_Filtering extends IrisTestCase {

   @Inject
   private Authorizer authorizer;

   @Test
   public void testDeviceEventNoFilteringRequired() {

      UUID devId = UUID.randomUUID();

      AuthorizationContext context = AuthzFixtures.createContext(AuthzFixtures.placeId, "dev:*:*", "swit:*:*");

      Map<String,Object> attributes = new HashMap<>();
      attributes.put("dev:name", "foobar");
      attributes.put("swit:state", "ON");

      PlatformMessage msg = AuthzFixtures.createDeviceValueChange(devId, attributes);

      PlatformMessage filteredMsg = authorizer.filter(context, AuthzFixtures.placeId.toString(), msg);
      assertNotNull(filteredMsg);
      assertHeadersEquals(msg, filteredMsg);
      assertEquals(msg.getValue(), filteredMsg.getValue());
   }

   @Test
   public void testDeviceEventFiltersOutAttributesLackingPermissions() {
      UUID devId = UUID.randomUUID();

      AuthorizationContext context = AuthzFixtures.createContext(AuthzFixtures.placeId, "dev:*:*", "swit:w,x:*");

      Map<String,Object> attributes = new HashMap<>();
      attributes.put("dev:name", "foobar");
      attributes.put("swit:state", "ON");

      PlatformMessage msg = AuthzFixtures.createDeviceValueChange(devId, attributes);

      PlatformMessage filteredMsg = authorizer.filter(context, AuthzFixtures.placeId.toString(), msg);
      assertNotNull(filteredMsg);
      assertHeadersEquals(msg, filteredMsg);

      attributes.remove("swit:state");

      assertEventAttributes(filteredMsg.getValue(), attributes);
   }

   @Test
   public void testDeviceEventFiltersOutAttributesSpecificPermission() {
      UUID devId = UUID.randomUUID();

      AuthorizationContext context = AuthzFixtures.createContext(AuthzFixtures.placeId, "dev:*:*", "swit:w,x" + devId.toString());

      Map<String,Object> attributes = new HashMap<>();
      attributes.put("dev:name", "foobar");
      attributes.put("swit:state", "ON");

      PlatformMessage msg = AuthzFixtures.createDeviceValueChange(devId, attributes);

      PlatformMessage filteredMsg = authorizer.filter(context, AuthzFixtures.placeId.toString(), msg);
      assertNotNull(filteredMsg);
      assertHeadersEquals(msg, filteredMsg);

      attributes.remove("swit:state");

      assertEventAttributes(filteredMsg.getValue(), attributes);
   }

   @Test
   public void testDeviceEventFiltersOutAttributesNoPermissions() {
      UUID devId = UUID.randomUUID();

      AuthorizationContext context = AuthzFixtures.createContext(AuthzFixtures.placeId, "dev:*:*");

      Map<String,Object> attributes = new HashMap<>();
      attributes.put("dev:name", "foobar");
      attributes.put("swit:state", "ON");

      PlatformMessage msg = AuthzFixtures.createDeviceValueChange(devId, attributes);

      PlatformMessage filteredMsg = authorizer.filter(context, AuthzFixtures.placeId.toString(), msg);
      assertNotNull(filteredMsg);
      assertHeadersEquals(msg, filteredMsg);

      attributes.remove("swit:state");

      assertEventAttributes(filteredMsg.getValue(), attributes);
   }


   @Test
   public void testDeviceEventFiltersOutAllAttributesResultsInNoMessage() {
      UUID devId = UUID.randomUUID();

      AuthorizationContext context = AuthzFixtures.createContext(AuthzFixtures.placeId, "dev:w,x:*", "swit:w,x:*");

      Map<String,Object> attributes = new HashMap<>();
      attributes.put("dev:name", "foobar");
      attributes.put("swit:state", "ON");

      PlatformMessage msg = AuthzFixtures.createDeviceValueChange(devId, attributes);
      PlatformMessage filtered = authorizer.filter(context, AuthzFixtures.placeId.toString(), msg);

      assertEquals(0, filtered.getValue().getAttributes().size());
   }

   @Test
   public void testDeviceEventFiltersNotForAuthorizedPlace() {
      UUID devId = UUID.randomUUID();

      AuthorizationContext context = AuthzFixtures.createContext(UUID.randomUUID(), "dev:*:*", "swit:*:*");

      Map<String,Object> attributes = new HashMap<>();
      attributes.put("dev:name", "foobar");
      attributes.put("swit:state", "ON");

      PlatformMessage msg = AuthzFixtures.createDeviceValueChange(devId, attributes);
      PlatformMessage filtered = authorizer.filter(context, AuthzFixtures.placeId.toString(), msg);
      assertEquals(0, filtered.getValue().getAttributes().size());
   }

   @Test
   public void testListDeviceResponseAllAllowed() {
      Device d1 = Fixtures.createDevice();
      Device d2 = Fixtures.createDevice();

      AuthorizationContext context = AuthzFixtures.createContext(AuthzFixtures.placeId, "dev:*:*", "devadv:*:*", "base:*:*");

      PlatformMessage msg = AuthzFixtures.createListDevices(d1, d2);
      PlatformMessage filtered = authorizer.filter(context, AuthzFixtures.placeId.toString(), msg);

      assertHeadersEquals(msg, filtered);
      MessageBody messagebody = filtered.getValue();
      Collection<Map<String,Object>> devices = (Collection<Map<String,Object>>) messagebody.getAttributes().get("devices");
      assertEquals(2, devices.size());

      devices.forEach((d) -> { assertEquals(5, d.size()); });
   }

   @Test
   public void testListDeviceResponseSpecificFilteredAllowed() {
      Device d1 = Fixtures.createDevice();
      Device d2 = Fixtures.createDevice();

      AuthorizationContext context = AuthzFixtures.createContext(AuthzFixtures.placeId, "dev:w,x:" + d1.getId().toString(), "dev:*:*");

      PlatformMessage msg = AuthzFixtures.createListDevices(d1, d2);
      PlatformMessage filtered = authorizer.filter(context, AuthzFixtures.placeId.toString(), msg);

      assertHeadersEquals(msg, filtered);

      MessageBody body = filtered.getValue();
      Collection<Map<String,Object>> devices = (Collection<Map<String,Object>>) body.getAttributes().get("devices");

      assertEquals(1, devices.size());
      devices.forEach((d) -> { assertEquals(1, d.size()); });
   }

   @Test
   public void testListDeviceResponseAllFiltered() {
      Device d1 = Fixtures.createDevice();
      Device d2 = Fixtures.createDevice();

      AuthorizationContext context = AuthzFixtures.createContext(AuthzFixtures.placeId, "dev:w,x:*");

      PlatformMessage msg = AuthzFixtures.createListDevices(d1, d2);
      PlatformMessage filtered = authorizer.filter(context, AuthzFixtures.placeId.toString(), msg);

      assertHeadersEquals(msg, filtered);
      MessageBody body = filtered.getValue();

      Collection<Map<String,Object>> devices = (Collection<Map<String,Object>>) body.getAttributes().get("devices");
      assertEquals(0, devices.size());
   }

   @Test
   public void testListDeviceResponseAllFilteredWrongPlace() {
      Device d1 = Fixtures.createDevice();
      Device d2 = Fixtures.createDevice();

      AuthorizationContext context = AuthzFixtures.createContext(UUID.randomUUID(), "dev:*:*");

      PlatformMessage msg = AuthzFixtures.createListDevices(d1, d2);
      PlatformMessage filtered = authorizer.filter(context, AuthzFixtures.placeId.toString(), msg);

      assertHeadersEquals(msg, filtered);
      MessageBody body = filtered.getValue();

      Collection<Map<String,Object>> devices = (Collection<Map<String,Object>>) body.getAttributes().get("devices");
      assertEquals(0, devices.size());
   }

   @Test
   public void testMessageFilteredIfNoActivePlace() {
      Device d1 = Fixtures.createDevice();
      Device d2 = Fixtures.createDevice();

      AuthorizationContext context = AuthzFixtures.createContext(AuthzFixtures.placeId, "dev:*:*");
      PlatformMessage msg = AuthzFixtures.createListDevices(d1, d2);
      assertNull(authorizer.filter(context, null, msg));
   }

   @Test
   public void testMessageFilteredIfPlacesDifferent() {
      Device d1 = Fixtures.createDevice();
      Device d2 = Fixtures.createDevice();

      AuthorizationContext context = AuthzFixtures.createContext(AuthzFixtures.placeId, "dev:*:*");
      PlatformMessage msg = AuthzFixtures.createListDevices(d1, d2);
      assertNull(authorizer.filter(context, UUID.randomUUID().toString(), msg));
   }

   private void assertEventAttributes(MessageBody event, Map<String,Object> attributes) {
      assertEquals(attributes, event.getAttributes());
   }

   private void assertHeadersEquals(PlatformMessage expected, PlatformMessage actual) {
      assertEquals(expected.getCorrelationId(), actual.getCorrelationId());
      assertEquals(expected.getMessageType(), actual.getMessageType());
      assertEquals(expected.getDestination(), actual.getDestination());
      assertEquals(expected.getSource(), actual.getSource());
      assertEquals(expected.getTimestamp(), actual.getTimestamp());
      assertEquals(expected.getTimeToLive(), actual.getTimeToLive());
   }
}


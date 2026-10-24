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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.service.RuleService;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

public class RoleAuthorizerTest {

   private final UUID personId = UUID.randomUUID();
   private final UUID placeId = UUID.randomUUID();
   private final UUID otherPlaceId = UUID.randomUUID();
   private final Address actor = Address.platformService(personId, PersonCapability.NAMESPACE);
   private final AuthorizationContext context = AuthzFixtures.createContext(placeId, "*:*:*");

   private AuthzMetrics metrics;
   private RoleAuthorizer authorizer;
   private Logger logger;
   private ListAppender<ILoggingEvent> appender;

   @Before
   public void setUp() {
      metrics = EasyMock.createMock(AuthzMetrics.class);
      authorizer = new RoleAuthorizer(metrics);

      logger = (Logger) LoggerFactory.getLogger(RoleAuthorizer.class);
      appender = new ListAppender<>();
      appender.start();
      logger.addAppender(appender);
   }

   @After
   public void tearDown() {
      logger.detachAppender(appender);
      EasyMock.verify(metrics);
   }

   @Test
   public void testSupportRequestDenied() {
      metrics.onUnauthorizedSupport();
      EasyMock.replay(metrics);

      PlatformMessage message = request(NotificationCapability.NotifyRequest.NAME, ImmutableMap.of(), Address.platformService(NotificationCapability.NAMESPACE));
      assertFalse(authorizer.isAuthorized(context, placeId.toString(), message));
      assertDenied("supportrequest", message, "activePlace=" + placeId, "requestedPlace=null");
   }

   @Test
   public void testNonOwnerDeletePlaceDenied() {
      metrics.onUnauthorizedNonAccountHolder();
      EasyMock.replay(metrics);

      PlatformMessage message = request(PlaceCapability.DeleteRequest.NAME, ImmutableMap.of(), Address.platformService(placeId, PlaceCapability.NAMESPACE));
      assertFalse(authorizer.isAuthorized(context, placeId.toString(), message));
      assertDenied("notowner", message, "requestedPlace=" + placeId);
   }

   @Test
   public void testSelfRequestForOtherPersonDenied() {
      metrics.onUnauthorizedWrongPerson();
      EasyMock.replay(metrics);

      Address otherPerson = Address.platformService(UUID.randomUUID(), PersonCapability.NAMESPACE);
      PlatformMessage message = request(PersonCapability.ListAvailablePlacesRequest.NAME, ImmutableMap.of(), otherPerson);
      assertFalse(authorizer.isAuthorized(context, placeId.toString(), message));
      assertDenied("wrongperson", message);
   }

   @Test
   public void testNoActivePlaceDenied() {
      metrics.onUnauthorizedNoPlace();
      EasyMock.replay(metrics);

      PlatformMessage message = request(Capability.CMD_GET_ATTRIBUTES, ImmutableMap.of(), Address.platformService(placeId, PlaceCapability.NAMESPACE));
      assertFalse(authorizer.isAuthorized(context, null, message));
      assertDenied("noplace", message, "activePlace=null");
   }

   @Test
   public void testServiceRequestForOtherPlaceDenied() {
      metrics.onUnauthorizedWrongPlace();
      EasyMock.replay(metrics);

      PlatformMessage message = request(RuleService.ListRulesRequest.NAME, ImmutableMap.of("placeId", otherPlaceId.toString()), Address.platformService(RuleService.NAMESPACE));
      assertFalse(authorizer.isAuthorized(context, placeId.toString(), message));
      assertDenied("wrongplace", message, "activePlace=" + placeId, "requestedPlace=" + otherPlaceId);
   }

   @Test
   public void testPlaceRequestForOtherPlaceDenied() {
      metrics.onUnauthorizedWrongPlace();
      EasyMock.replay(metrics);

      PlatformMessage message = request(Capability.CMD_GET_ATTRIBUTES, ImmutableMap.of(), Address.platformService(otherPlaceId, PlaceCapability.NAMESPACE));
      assertFalse(authorizer.isAuthorized(context, placeId.toString(), message));
      assertDenied("wrongplace", message, "requestedPlace=" + otherPlaceId);
   }

   @Test
   public void testAuthorizedRequestNotLogged() {
      metrics.onAuthorized();
      EasyMock.replay(metrics);

      PlatformMessage message = request(Capability.CMD_GET_ATTRIBUTES, ImmutableMap.of(), Address.platformService(placeId, PlaceCapability.NAMESPACE));
      assertTrue(authorizer.isAuthorized(context, placeId.toString(), message));
      assertTrue(appender.list.isEmpty());
   }

   private PlatformMessage request(String type, Map<String, Object> attributes, Address destination) {
      return PlatformMessage
            .buildRequest(type, attributes, Address.clientAddress("android", "1"), destination)
            .withActor(actor)
            .create();
   }

   private void assertDenied(String reason, PlatformMessage message, String... fields) {
      assertEquals(1, appender.list.size());
      ILoggingEvent event = appender.list.get(0);
      assertEquals(Level.WARN, event.getLevel());

      String line = event.getFormattedMessage();
      assertContains(line, "reason=" + reason);
      assertContains(line, "type=" + message.getMessageType());
      assertContains(line, "actor=" + actor.getRepresentation());
      assertContains(line, "destination=" + message.getDestination().getRepresentation());
      for(String field : fields) {
         assertContains(line, field);
      }
   }

   private static void assertContains(String line, String expected) {
      assertTrue("expected [" + expected + "] in [" + line + "]", line.contains(expected));
   }
}

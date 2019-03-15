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
package com.iris.platform.rule.environment;

import java.util.TimeZone;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.common.scheduler.Scheduler;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.RuleCapability;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Modules({ InMemoryMessageModule.class })
@Mocks({ PlaceEnvironmentExecutor.class, PlaceExecutorRegistry.class, Scheduler.class })
public class TestPlatformRuleContext extends IrisMockTestCase {

   @Inject InMemoryPlatformMessageBus platformBus;
   @Inject Scheduler scheduler;
   @Inject PlaceExecutorEventLoop eventLoop;
   
   PlatformRuleContext context;
   
   UUID placeId;
   Address address;
   
   @Before
   public void setUp() throws Exception {
      super.setUp();
      
      placeId = UUID.randomUUID();
      address = Address.platformService(UUID.randomUUID(), RuleCapability.NAMESPACE, 1);
      
      context =
            PlatformRuleContext
               .builder()
               .withLogger(LoggerFactory.getLogger(TestPlatformRuleContext.class))
               .withModels(new RuleModelStore())
               .withPlatformBus(platformBus)
               .withEventLoop(eventLoop)
               .withPlaceId(placeId)
               .withSource(address)
               .withTimeZone(TimeZone.getDefault())
               .build();
   }
   
   @Test
   public void testBroadcast() throws Exception {
      context.broadcast(
            MessageBody
               .buildMessage(Capability.EVENT_VALUE_CHANGE, ImmutableMap.of("test:attr", "new-value"))
      );
      
      PlatformMessage message = platformBus.poll();
      assertEquals(Address.broadcastAddress(), message.getDestination());
      assertEquals(address, message.getSource());
      assertEquals(placeId.toString(), message.getPlaceId());
      assertEquals(Capability.EVENT_VALUE_CHANGE, message.getMessageType());
      assertFalse(message.isRequest());
      assertFalse(message.isError());
      assertNotNull(message.getTimestamp());
   }
   
   @Test
   public void testSend() throws Exception {
      Address notifications = Address.platformService(NotificationCapability.NAMESPACE);
      context.send(
            notifications,
            MessageBody
               .buildMessage(Capability.EVENT_VALUE_CHANGE, ImmutableMap.of("test:attr", "new-value"))
      );
      
      PlatformMessage message = platformBus.poll();
      assertEquals(notifications, message.getDestination());
      assertEquals(address, message.getSource());
      assertEquals(placeId.toString(), message.getPlaceId());
      assertEquals(Capability.EVENT_VALUE_CHANGE, message.getMessageType());
      assertTrue(message.isRequest());
      assertFalse(message.isError());
      // no response will be watched for a rule
      assertNull(message.getCorrelationId());
      assertNotNull(message.getTimestamp());
   }
   
}


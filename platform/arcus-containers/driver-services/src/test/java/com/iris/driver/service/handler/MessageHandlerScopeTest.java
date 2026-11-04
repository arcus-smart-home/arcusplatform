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
package com.iris.driver.service.handler;

import static org.junit.Assert.fail;

import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.service.executor.DriverExecutor;
import com.iris.driver.service.executor.DriverExecutorRegistry;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DoorLockCapability;
import com.iris.messages.errors.UnauthorizedRequestException;
import com.iris.messages.model.Fixtures;

/**
 * Every device command and attribute write that has no handler of its own lands in
 * {@link MessageHandler}, so this is the check that keeps one place's clients off another
 * place's locks, switches and thermostats.
 */
public class MessageHandlerScopeTest {

   private final UUID devicePlace = UUID.randomUUID();
   private final Address device = Address.platformDriverAddress(UUID.randomUUID());

   private DriverExecutorRegistry registry;
   private DriverExecutor executor;
   private DeviceDriverContext context;
   private MessageHandler handler;

   @Before
   public void setUp() {
      registry = EasyMock.createMock(DriverExecutorRegistry.class);
      executor = EasyMock.createMock(DriverExecutor.class);
      context = EasyMock.createMock(DeviceDriverContext.class);
      handler = new MessageHandler(registry);

      EasyMock.expect(registry.loadConsumer(device)).andReturn(executor).anyTimes();
      EasyMock.expect(executor.context()).andReturn(context).anyTimes();
      EasyMock.expect(context.getPlaceId()).andReturn(devicePlace).anyTimes();
   }

   @After
   public void tearDown() {
      EasyMock.verify(registry, executor, context);
   }

   private void replay() {
      EasyMock.replay(registry, executor, context);
   }

   private PlatformMessage request(MessageBody body, UUID placeHeader) {
      PlatformMessage.Builder builder =
            PlatformMessage
               .request(device)
               .from(Fixtures.createClientAddress())
               .withPayload(body);
      if(placeHeader != null) {
         builder.withPlaceId(placeHeader);
      }
      return builder.create();
   }

   private static MessageBody unlock() {
      return MessageBody.buildMessage(
            Capability.CMD_SET_ATTRIBUTES,
            ImmutableMap.of(DoorLockCapability.ATTR_LOCKSTATE, DoorLockCapability.LOCKSTATE_UNLOCKED));
   }

   private void assertRefused(PlatformMessage message) throws Exception {
      try {
         handler.handleMessage(message);
         fail("expected " + message.getMessageType() + " from another place to be refused");
      }
      catch(UnauthorizedRequestException e) {
         // expected; the strict executor mock fails the test if the driver saw the message
      }
   }

   @Test
   public void testCommandFromAnotherPlaceNeverReachesTheDriver() throws Exception {
      replay();
      assertRefused(request(DoorLockCapability.BuzzInRequest.instance(), UUID.randomUUID()));
   }

   @Test
   public void testAttributeWriteFromAnotherPlaceNeverReachesTheDriver() throws Exception {
      replay();
      assertRefused(request(unlock(), UUID.randomUUID()));
   }

   @Test
   public void testCommandAtTheDevicesPlaceReachesTheDriver() throws Exception {
      PlatformMessage message = request(DoorLockCapability.BuzzInRequest.instance(), devicePlace);
      EasyMock.expect(executor.fire(message)).andReturn(Futures.immediateFuture(null));
      replay();

      handler.handleMessage(message);
   }

   @Test
   public void testAttributeWriteAtTheDevicesPlaceReachesTheDriver() throws Exception {
      PlatformMessage message = request(unlock(), devicePlace);
      EasyMock.expect(executor.fire(message)).andReturn(Futures.immediateFuture(null));
      replay();

      handler.handleMessage(message);
   }

   @Test
   public void testInternalMessageWithoutAPlaceHeaderReachesTheDriver() throws Exception {
      PlatformMessage message = request(DoorLockCapability.BuzzInRequest.instance(), null);
      EasyMock.expect(executor.fire(message)).andReturn(Futures.immediateFuture(null));
      replay();

      handler.handleMessage(message);
   }
}

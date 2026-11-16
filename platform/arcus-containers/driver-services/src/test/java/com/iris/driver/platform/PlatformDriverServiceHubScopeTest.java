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
package com.iris.driver.platform;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.Futures;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.protocol.ProtocolMessageBus;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.service.DriverConfig;
import com.iris.driver.service.DriverServiceConfig;
import com.iris.driver.service.executor.DriverExecutor;
import com.iris.driver.service.executor.DriverExecutorRegistry;
import com.iris.driver.service.registry.DriverRegistry;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.capability.Capability;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.reflex.ReflexProtocol;
import com.iris.protocol.zwave.ZWaveProtocol;

/**
 * hub-bridge only lets a hub send protocol messages from its own addresses, but a destination other
 * than a broadcast picks the driver that runs, so it has to be one of the sending hub's devices.
 */
public class PlatformDriverServiceHubScopeTest {
   private static final String HUB_ID = "ABC-1234";
   private static final String OTHER_HUB_ID = "XYZ-5678";
   private static final ProtocolDeviceId NODE = ProtocolDeviceId.fromBytes(new byte[] { 0, 0, 0, 5 });

   private final Address hubDevice = Address.hubProtocolAddress(HUB_ID, ZWaveProtocol.NAMESPACE, NODE);
   private final Address otherHubsDevice = Address.hubProtocolAddress(OTHER_HUB_ID, ZWaveProtocol.NAMESPACE, NODE);
   private final Address driver = Address.platformDriverAddress(UUID.randomUUID());

   private DriverExecutorRegistry registry;
   private DriverExecutor executor;
   private DeviceDriverContext context;
   private PlatformDriverService service;

   @Before
   public void setUp() {
      registry = EasyMock.createMock(DriverExecutorRegistry.class);
      executor = EasyMock.createMock(DriverExecutor.class);
      context = EasyMock.createMock(DeviceDriverContext.class);
      service = new PlatformDriverService(
            new DriverConfig(),
            new DriverServiceConfig(),
            EasyMock.createNiceMock(ProtocolMessageBus.class),
            EasyMock.createNiceMock(PlatformMessageBus.class),
            EasyMock.createNiceMock(DriverRegistry.class),
            registry,
            Collections.emptySet(),
            EasyMock.createNiceMock(ThreadPoolExecutor.class),
            Collections.emptySet(),
            Collections.emptySet(),
            EasyMock.createNiceMock(PlacePopulationCacheManager.class));
   }

   private void expectDriverOf(Address address, Address deviceProtocolAddress) {
      EasyMock.expect(registry.loadConsumer(address)).andReturn(executor);
      EasyMock.expect(executor.context()).andReturn(context).anyTimes();
      EasyMock.expect(context.getProtocolAddress()).andReturn(deviceProtocolAddress).anyTimes();
   }

   private void replayAll() {
      EasyMock.replay(registry, executor, context);
   }

   private void verifyAll() {
      // also fails on an unexpected call the service caught and logged
      EasyMock.verify(registry, executor, context);
   }

   private static ProtocolMessage zwave(Address source, Address destination) {
      return ProtocolMessage.buildProtocolMessage(source, destination, ZWaveProtocol.NAMESPACE, new byte[] {}).create();
   }

   @Test
   public void testMessageToAnotherHubsDriverIsDropped() {
      expectDriverOf(driver, otherHubsDevice);
      replayAll();

      service.handleProtocolMessage(zwave(hubDevice, driver));

      verifyAll(); // the driver was never fired
   }

   @Test
   public void testMessageToAPlatformDevicesDriverIsDropped() {
      expectDriverOf(driver, Address.protocolAddress("IPCD", NODE));
      replayAll();

      service.handleProtocolMessage(zwave(hubDevice, driver));

      verifyAll();
   }

   @Test
   public void testReflexMessageToAnotherHubsDriverIsDropped() {
      expectDriverOf(driver, otherHubsDevice);
      replayAll();

      MessageBody attributes = MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, Collections.<String, Object>singletonMap("swit:state", "ON"));
      service.handleReflexProtocolMessage(ProtocolMessage.builder().from(hubDevice).to(driver).withPayload(ReflexProtocol.INSTANCE, attributes).create());

      verifyAll(); // the driver's reflexes were never looked at
   }

   @Test
   public void testBroadcastFromItsDeviceFiresItsDriver() {
      ProtocolMessage message = zwave(hubDevice, Address.broadcastAddress());
      expectDriverOf(hubDevice, hubDevice);
      EasyMock.expect(executor.fire(message)).andReturn(Futures.immediateFuture(null));
      replayAll();

      service.handleProtocolMessage(message);

      verifyAll();
   }

   @Test
   public void testMessageToItsOwnDevicesDriverFiresIt() {
      ProtocolMessage message = zwave(hubDevice, driver);
      expectDriverOf(driver, hubDevice);
      EasyMock.expect(executor.fire(message)).andReturn(Futures.immediateFuture(null));
      replayAll();

      service.handleProtocolMessage(message);

      verifyAll();
   }
}

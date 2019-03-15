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
package com.iris.driver.groovy.control;

import java.util.Date;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.groovy.GroovyDriverTestCase;
import com.iris.driver.groovy.plugin.GroovyDriverPlugin;
import com.iris.driver.groovy.zwave.ZWaveProtocolPlugin;
import com.iris.messages.MessageBody;
import com.iris.messages.model.Fixtures;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.control.ControlProtocol;
import com.iris.protocol.control.DeviceOfflineEvent;
import com.iris.protocol.control.DeviceOnlineEvent;
import com.iris.util.IrisCollections;

public class TestGroovyDriverControlHandlers extends GroovyDriverTestCase {
   private final ControlProtocol controlProtocol = ControlProtocol.INSTANCE;
   private DeviceDriver driver;
   private DeviceDriverContext context;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      driver = factory.load("ControlMessageHandler.driver");
      context = new PlatformDeviceDriverContext(createDevice(driver), driver, mockPopulationCacheMgr);
   }

   @Test
   public void testMatchOffline() throws Exception {
      Date lastContact = new Date();
      ProtocolMessage message = createMessage(DeviceOfflineEvent.create(lastContact));
      driver.handleProtocolMessage(message, context);
      assertEquals("exact", context.getVariable("match"));
      assertEquals(lastContact.getTime(), ((Number) context.getVariable("lastContact")).longValue());
   }

   @Test
   public void testMatchOnline() throws Exception {
      ProtocolMessage message = createMessage(DeviceOnlineEvent.create());
      driver.handleProtocolMessage(message, context);
      assertEquals("online", context.getVariable("match"));
   }

   @Test
   public void testMatchWildcard() throws Exception {
      ProtocolMessage message = createMessage(MessageBody.buildMessage("Foo", ImmutableMap.of()));
      driver.handleProtocolMessage(message, context);
      assertEquals("protocol", context.getVariable("match"));
   }

   @Override
   protected Set<GroovyDriverPlugin> getPlugins() {
      return IrisCollections.<GroovyDriverPlugin>setOf(new ControlProtocolPlugin(), new ZWaveProtocolPlugin());
   }

   private ProtocolMessage createMessage(MessageBody messageBody) {
      return ProtocolMessage.builder()
               .from(Fixtures.createProtocolAddress(ControlProtocol.NAMESPACE))
               .to(Fixtures.createDeviceAddress())
               .withPayload(controlProtocol, messageBody)
               .create();
   }
}


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
package com.iris.driver.groovy.ipcd;

import java.util.Set;

import com.iris.device.model.CapabilityDefinition;
import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.groovy.GroovyDriverTestCase;
import com.iris.driver.groovy.control.ControlProtocolPlugin;
import com.iris.driver.groovy.plugin.GroovyDriverPlugin;
import com.iris.driver.groovy.zigbee.ZigbeeProtocolPlugin;
import com.iris.driver.groovy.zwave.ZWaveProtocolPlugin;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.ipcd.IpcdProtocol;
import com.iris.protocol.ipcd.message.IpcdMessage;
import com.iris.util.IrisCollections;

public class IpcdHandlersTestCase extends GroovyDriverTestCase {
   private String driverFile;
   protected DeviceDriver driver;
   protected DeviceDriverContext context;
   
   protected IpcdHandlersTestCase(String driverFile) {
      this.driverFile = driverFile;
   }
   
   @Override
   public void setUp() throws Exception {
      super.setUp();
      driver = factory.load("com/iris/driver/groovy/ipcd/" + driverFile);
      Device device = createDevice(driver);
      device.setProtocolAttributes(IpcdFixtures.createProtocolAttributes());
      for(CapabilityDefinition def: driver.getDefinition().getCapabilities()) {
          device.getCaps().add(def.getNamespace());
      }
      device.setDrivername(driver.getDefinition().getName());
      device.setDriverversion(driver.getDefinition().getVersion());
      context = new PlatformDeviceDriverContext(device, driver, mockPopulationCacheMgr);
   }
   
   @Override
   protected Set<GroovyDriverPlugin> getPlugins() {
      return IrisCollections.<GroovyDriverPlugin>setOf(new IpcdProtocolPlugin(), new ZigbeeProtocolPlugin(), new ZWaveProtocolPlugin(), new ControlProtocolPlugin());
   }
   
   protected void sendMessage(IpcdMessage msg) {
      ProtocolMessage protocolMessage = createProtocolMessage(msg);
      driver.handleProtocolMessage(protocolMessage, context);
   }
   
   protected static ProtocolMessage createProtocolMessage(IpcdMessage msg) {
      return ProtocolMessage.builder()
                  .from(Fixtures.createProtocolAddress(IpcdProtocol.NAMESPACE))
                  .to(Fixtures.createDeviceAddress())
                  .withPayload(IpcdProtocol.INSTANCE, msg)
                  .create();
   }
}


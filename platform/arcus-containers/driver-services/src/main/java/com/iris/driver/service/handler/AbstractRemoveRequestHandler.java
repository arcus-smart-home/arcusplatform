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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.core.dao.DeviceDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.driver.groovy.plugin.GroovyDriverPlugin;
import com.iris.driver.groovy.plugin.ProtocolPlugin;
import com.iris.driver.service.executor.DriverExecutorRegistry;
import com.iris.driver.service.registry.DriverRegistry;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceDriverAddress;
import com.iris.messages.errors.NotFoundException;
import com.iris.messages.model.Device;

public abstract class AbstractRemoveRequestHandler implements DriverServiceRequestHandler {
   private static final Logger logger = LoggerFactory.getLogger(AbstractRemoveRequestHandler.class);

   private final DeviceDAO deviceDao;
   private final PlatformMessageBus platformBus;
   private final Map<String,ProtocolPlugin> protocols;

   public AbstractRemoveRequestHandler(
         DeviceDAO deviceDao,
         PlatformMessageBus platformBus,
         Set<GroovyDriverPlugin> plugins
   ) {

      this.deviceDao = deviceDao;
      this.platformBus = platformBus;

      this.protocols = new HashMap<>();
      for (GroovyDriverPlugin p : plugins) {
         if (p instanceof ProtocolPlugin) {
            ProtocolPlugin pp = (ProtocolPlugin)p;
            this.protocols.put(pp.getProtocol().getNamespace(), pp);
         }
      }
   }
   
   protected Device loadDevice(PlatformMessage message) throws NotFoundException {
      Address destination = message.getDestination();
      UUID deviceId = ((DeviceDriverAddress) destination).getDeviceId();

      Device dev = deviceDao.findById(deviceId);
      if (dev == null) {
         logger.warn("Message sent to non-existent device [{}]", (deviceId));
         throw new NotFoundException(destination);
      }
      return dev;
   }

   public void sendRemoveRequest(Device dev, long duration, boolean force) throws Exception {
      String prot = dev.getProtocol();
      ProtocolPlugin pp = protocols.get(prot);
      if (pp == null) {
         logger.warn("Message sent to device with non-existent protocol [{}]", dev);
         throw new IllegalStateException("Unable to remove device due to unrecognized protocol: " + prot);
      }

      PlatformMessage req = pp.handleRemove(dev, duration, force);
      if(req != null) {
         platformBus.send(req);
      }
   }
}


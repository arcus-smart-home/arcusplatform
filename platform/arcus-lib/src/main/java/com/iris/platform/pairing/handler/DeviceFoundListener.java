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
package com.iris.platform.pairing.handler;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.PairingDeviceCapability;
import com.iris.messages.listener.annotation.OnMessage;
import com.iris.platform.pairing.PairingDevice;
import com.iris.platform.pairing.PairingDeviceDao;

@Singleton
public class DeviceFoundListener extends BaseChangeListener {
   private static final Logger logger = LoggerFactory.getLogger(DeviceFoundListener.class);
   
   private final PairingDeviceDao pairingDeviceDao;
   
   @Inject
   public DeviceFoundListener(
         PlatformMessageBus messageBus,
         PairingDeviceDao pairingDeviceDao) {
      super(messageBus, pairingDeviceDao);
      this.pairingDeviceDao = pairingDeviceDao;
   }
   
   @OnMessage(from="HUB:*", types=HubCapability.DeviceFoundEvent.NAME)
   public void onDeviceFound(PlatformMessage message) {
      DeviceProtocolAddress protocolAddress = (DeviceProtocolAddress)Address.fromString(HubCapability.DeviceFoundEvent.getProtocolAddress(message.getValue()));
      UUID placeId = UUID.fromString(message.getPlaceId());
      String phase = HubCapability.DeviceFoundEvent.getPhase(message.getValue());
      
      logger.debug("Device Found at place {} with protocol address {}", placeId, protocolAddress);
      
      PairingDevice device = new PairingDevice();
      device.setPlaceId(placeId);
      device.setProtocolAddress(protocolAddress);
      device = pairingDeviceDao.save(device);
      
      updatePairingStatus(device, message.getValue(), null, PairingDeviceCapability.PAIRINGSTATE_PAIRING, phase);
   }

}


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
package com.iris.ipcd.delivery;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.server.session.ClientToken;
import com.iris.core.protocol.ipcd.IpcdDeviceDao;
import com.iris.ipcd.session.IpcdClientToken;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.type.IpcdDeviceType;
import com.iris.protocol.ipcd.IpcdDevice;
import com.iris.protocol.ipcd.message.model.Device;

@Singleton
public class IpcdDeliveryStrategyRegistry {

   private static final Logger logger = LoggerFactory.getLogger(IpcdDeliveryStrategyRegistry.class);

   private DefaultIpcdDeliveryStrategy fallback;
   private Map<String, IpcdDeliveryStrategy> strategyLookup;
   private final ConcurrentMap<String, IpcdDeliveryStrategy> strategies = new ConcurrentHashMap<>();
   private final IpcdDeviceDao dao;

   @Inject
   public IpcdDeliveryStrategyRegistry(
      IpcdDeviceDao dao,
      DefaultIpcdDeliveryStrategy fallback,
      Map<String, IpcdDeliveryStrategy> strategyLookup
   ) {
      this.dao = dao;
      this.fallback = fallback;
      this.strategyLookup = strategyLookup;
   }

   public static String lookupKey(Device device) {
      return (device.getVendor() + '-' + device.getModel()).toLowerCase();
   }

   public static String lookupKey(IpcdDeviceType deviceType) {
      return (deviceType.getVendor() + '-' + deviceType.getModel()).toLowerCase();
   }

   public IpcdDeliveryStrategy deliveryStrategyFor(String protocolAddress) {
      return strategies.computeIfAbsent(protocolAddress, addr -> {
         IpcdDevice dev = dao.findByProtocolAddress(addr);
         if(dev == null) {
            logger.info("returning default strategy for {} for which no ipcd device exists", addr);
            return fallback;
         }
         IpcdDeliveryStrategy strategy = strategyLookup.get(lookupKey(dev.getDevice()));
         return strategy == null ? fallback : strategy;
      });
   }

   public IpcdDeliveryStrategy deliveryStrategyFor(Address address) {
      Preconditions.checkArgument(address instanceof DeviceProtocolAddress, "address must be a protocol address");
      return deliveryStrategyFor(address.getRepresentation());
   }

   public IpcdDeliveryStrategy deliveryStrategyFor(ClientToken clientToken) {
      Preconditions.checkArgument(clientToken instanceof IpcdClientToken, "clientToken must be an ipcd client token");
      return deliveryStrategyFor(clientToken.getRepresentation());
   }

   public IpcdDeliveryStrategy deliveryStrategyFor(String protocolAddress, Device device) {
      return strategies.computeIfAbsent(protocolAddress, addr -> {
         if(device == null) {
            logger.info("returning default strategy for {} for which no ipcd device exists", addr);
            return fallback;
         }
         IpcdDeliveryStrategy strategy = strategyLookup.get(lookupKey(device));
         return strategy == null ? fallback : strategy;
      });
   }

   public IpcdDeliveryStrategy deliveryStrategyFor(Address address, Device device) {
      Preconditions.checkArgument(address instanceof DeviceProtocolAddress, "address must be a protocol address");
      return deliveryStrategyFor(address.getRepresentation(), device);
   }

   public IpcdDeliveryStrategy deliveryStrategyFor(ClientToken clientToken, Device device) {
      Preconditions.checkArgument(clientToken instanceof IpcdClientToken, "clientToken must be an ipcd client token");
      return deliveryStrategyFor(clientToken.getRepresentation(), device);
   }
}


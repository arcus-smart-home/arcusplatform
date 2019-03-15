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
package com.iris.ipcd.server.session;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.bus.ProtocolBusService;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionFactory;
import com.iris.bridge.server.session.SessionRegistry;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.protocol.ipcd.IpcdDeviceDao;
import com.iris.platform.partition.Partitioner;
import com.iris.population.PlacePopulationCacheManager;

import io.netty.channel.Channel;

@Singleton
public class IpcdSessionFactory implements SessionFactory {
   private final SessionRegistry parent;
   private final IpcdDeviceDao ipcdDeviceDao;
   private final DeviceDAO deviceDao;
   private final PlaceDAO placeDao;
   private final PlatformMessageBus platformBus;
   private final ProtocolBusService protocolBusService;
   private final Partitioner partitioner;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public IpcdSessionFactory(
         SessionRegistry parent,
         IpcdDeviceDao ipcdDeviceDao,
         DeviceDAO deviceDao,
         PlaceDAO placeDao,
         PlatformMessageBus platformBus,
         ProtocolBusService protocolBusService,
         Partitioner partitioner,
         PlacePopulationCacheManager populationCacheMgr) {
      this.parent = parent;
      this.ipcdDeviceDao = ipcdDeviceDao;
      this.deviceDao = deviceDao;
      this.placeDao = placeDao;
      this.platformBus = platformBus;
      this.protocolBusService = protocolBusService;
      this.partitioner = partitioner;
      this.populationCacheMgr = populationCacheMgr;
   }

   @Override
   public Session createSession(Client client, Channel channel, BridgeMetrics bridgeMetrics) {
      return new IpcdSocketSession(parent, ipcdDeviceDao, deviceDao, placeDao, channel,
         platformBus, protocolBusService, partitioner, bridgeMetrics, populationCacheMgr);
   }

}


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
package com.iris.ipcd.bus;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.bus.PlatformBusListener;
import com.iris.bridge.server.session.ClientToken;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionRegistry;
import com.iris.core.dao.PlaceDAO;
import com.iris.ipcd.session.IpcdClientToken;
import com.iris.ipcd.session.IpcdSession;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.service.IpcdService;
import com.iris.messages.type.Population;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.population.PopulationUtils;

@Singleton
public class IpcdServiceEventListener implements PlatformBusListener {

   private static final Logger logger = LoggerFactory.getLogger(IpcdServiceEventListener.class);

   private final SessionRegistry registry;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public IpcdServiceEventListener(SessionRegistry registry, PlacePopulationCacheManager populationCacheMgr) {
      this.registry = registry;
      this.populationCacheMgr = populationCacheMgr;
   }

   @Override
   public void onMessage(ClientToken ct, PlatformMessage msg) {
      switch(msg.getMessageType()) {
      case IpcdService.DeviceClaimedEvent.NAME:
         onClaimed(msg);
         break;
      case IpcdService.DeviceRegisteredEvent.NAME:
         onRegistered(msg);
         break;
      case IpcdService.DeviceUnregisteredEvent.NAME:
         onUnregistered(msg);
         break;
      default:
         break;
      }
   }

   private void onClaimed(PlatformMessage msg) {
      MessageBody body = msg.getValue();
      logger.trace("handling claim event: [{}]", body);
      String protocolAddress = IpcdService.DeviceClaimedEvent.getProtocolAddress(body);
      IpcdSession session = getSession(protocolAddress);
      if(session == null) {
         logger.debug("dropping claim event for [{}], no IPCD session found in the registry", protocolAddress);
         return;
      }
      String curPlaceId = IpcdService.DeviceClaimedEvent.getPlaceId(body);   
      session.claim(IpcdService.DeviceClaimedEvent.getAccountId(body), curPlaceId, lookupPopulationByPlace(curPlaceId));
   }

   private void onRegistered(PlatformMessage msg) {
      MessageBody body = msg.getValue();
      logger.trace("handling register event: [{}]", body);
      String protocolAddress = IpcdService.DeviceRegisteredEvent.getProtocolAddress(body);
      IpcdSession session = getSession(protocolAddress);
      if(session == null) {
         logger.debug("dropping register event for [{}], no IPCD session found in the registry", protocolAddress);
         return;
      }
      String curPlaceId = IpcdService.DeviceRegisteredEvent.getPlaceId(body);
      session.register(
            IpcdService.DeviceRegisteredEvent.getAccountId(body),
            curPlaceId,
            lookupPopulationByPlace(curPlaceId),
            IpcdService.DeviceRegisteredEvent.getDriverAddress(body));
   }

   private void onUnregistered(PlatformMessage msg) {
      MessageBody body = msg.getValue();
      logger.trace("handling unregister event: [{}]", body);
      String protocolAddress = IpcdService.DeviceUnregisteredEvent.getProtocolAddress(body);
      IpcdSession session = getSession(protocolAddress);
      if(session == null) {
         logger.debug("dropping unregister event for [{}], no IPCD session found in the registry", protocolAddress);
         return;
      }
      session.unregister();
   }

   private IpcdSession getSession(String protocolAddress) {
      if(StringUtils.isBlank(protocolAddress)) {
         return null;
      }
      ClientToken tok = IpcdClientToken.fromProtocolAddress(protocolAddress);
      Session session = registry.getSession(tok);
      if(!(session instanceof IpcdSession)) {
         return null;
      }
      return (IpcdSession) session;
   }
   
   private String lookupPopulationByPlace(String curPlaceId) {
   	return populationCacheMgr.getPopulationByPlaceId(curPlaceId);
   }
}


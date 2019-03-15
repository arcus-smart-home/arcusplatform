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
package com.iris.hubcom.server.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.bus.PlatformBusService;
import com.iris.bridge.server.session.Session;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.platform.hub.registration.HubRegistrationMessageHandlerAdaptor;
import com.iris.population.PlacePopulationCacheManager;


@Singleton
public class HubFirmwareUpgradeProcessEventHandler extends DirectMessageHandler {
   private static final Logger logger = LoggerFactory.getLogger(HubFirmwareUpgradeProcessEventHandler.class);   
   private final HubRegistrationMessageHandlerAdaptor hubRegistrationMsgAdaptor;
   
   @Inject
   public HubFirmwareUpgradeProcessEventHandler(
      HubRegistrationMessageHandlerAdaptor hubRegistrationMsgAdaptor,
         PlatformBusService platformBus,
         PlacePopulationCacheManager populationCacheMgr
   ) {
      super(platformBus, populationCacheMgr);
	   this.hubRegistrationMsgAdaptor = hubRegistrationMsgAdaptor;
   }
      

   @Override
   public String supportsMessageType() {
		return HubAdvancedCapability.FirmwareUpgradeProcessEvent.NAME;
   }
	
   @Override
   protected void doHandle(Session session, PlatformMessage msg) {
      hubRegistrationMsgAdaptor.handleFirmwareUpgradeProcessEvent(msg.getSource().getHubId(), msg);
		
   }

}


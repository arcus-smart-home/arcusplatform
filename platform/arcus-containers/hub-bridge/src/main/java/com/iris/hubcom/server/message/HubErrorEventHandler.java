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
import com.iris.hubcom.server.session.HubSession;
import com.iris.messages.ErrorEvent;
import com.iris.messages.PlatformMessage;
import com.iris.platform.hub.registration.HubRegistrationMessageHandlerAdaptor;
import com.iris.population.PlacePopulationCacheManager;

/**
 * Routes {@code Error} events sent by the hub back to the registration
 * registry, e.g. a failed firmware download (port.error() on the agent
 * side). DefaultHubMessageFilterImpl already allow-lists ErrorEvent through
 * regardless of session authorization state, but without this handler
 * registered nothing in hub-bridge ever consumed it: it reached
 * HubRegistrationMessageHandlerAdaptor.handleErrorEvent() from nowhere, so a
 * real download/install failure never updated hub_registration's error
 * fields and a stuck upgrade looked identical to one that had simply never
 * been attempted.
 */
@Singleton
public class HubErrorEventHandler extends DirectMessageHandler {
   private static final Logger logger = LoggerFactory.getLogger(HubErrorEventHandler.class);
   private final HubRegistrationMessageHandlerAdaptor hubRegistrationMsgAdaptor;

   @Inject
   public HubErrorEventHandler(
      HubRegistrationMessageHandlerAdaptor hubRegistrationMsgAdaptor,
         PlatformBusService platformBus,
         PlacePopulationCacheManager populationCacheMgr
   ) {
      super(platformBus, populationCacheMgr);
      this.hubRegistrationMsgAdaptor = hubRegistrationMsgAdaptor;
   }

   @Override
   public String supportsMessageType() {
      return ErrorEvent.MESSAGE_TYPE;
   }

   @Override
   protected void doHandle(Session session, PlatformMessage msg) {
      String hubId = ((HubSession) session).getHubId();
      logger.debug("hub [{}] reported error: {}", hubId, msg.getValue());
      hubRegistrationMsgAdaptor.handleErrorEvent(hubId, msg);
   }
}

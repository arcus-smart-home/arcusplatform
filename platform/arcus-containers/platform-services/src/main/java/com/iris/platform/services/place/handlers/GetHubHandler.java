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
package com.iris.platform.services.place.handlers;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.Utils;
import com.iris.core.dao.HubDAO;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.Place;
import com.iris.platform.model.ModelEntity;
import com.iris.platform.services.hub.HubRegistry;

@Singleton
public class GetHubHandler implements ContextualRequestMessageHandler<Place> {
   private static final Logger logger = LoggerFactory.getLogger(GetHubHandler.class);

   private final HubDAO hubDao;
   private final HubRegistry registry;

   @Inject
   public GetHubHandler(
         HubDAO hubDao,
         HubRegistry registry
   ) {
      this.hubDao = hubDao;
      this.registry = registry;
   }

   @Override
   public String getMessageType() {
      return MessageConstants.MSG_GET_HUB;
   }

   @Override
   public MessageBody handleRequest(Place context, PlatformMessage msg) {
      Utils.assertNotNull(context, "The place is required");
      Map<String,Object> hubAttrs = new HashMap<>();
      ModelEntity hub = hubDao.findHubModelForPlace(context.getId());
      if(hub != null) {
         if(HubCapability.STATE_DOWN.equals(hub.getAttribute(HubCapability.ATTR_STATE))) {
            // double check its down
            if(registry.isOnline(hub.getId())) {
               logger.warn("Hub {} incorrectly marked as offline", hub.getId());

               hub.setAttribute(HubCapability.ATTR_STATE, HubCapability.STATE_NORMAL);
               // TODO fire a ValueChange to get everyone else back in sync
            }
         }
         hubAttrs = hub.toMap();
      }

      return
            PlaceCapability.GetHubResponse
               .builder()
               .withHub(hubAttrs)
               .build();
   }
}


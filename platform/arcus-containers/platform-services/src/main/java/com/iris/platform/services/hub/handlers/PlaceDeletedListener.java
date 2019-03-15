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
package com.iris.platform.services.hub.handlers;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.HubDAO;
import com.iris.core.platform.ContextualEventMessageHandler;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.model.Hub;
import com.iris.messages.services.PlatformConstants;


@Singleton
public class PlaceDeletedListener implements ContextualEventMessageHandler<Hub> {

   private static final Logger logger = LoggerFactory.getLogger(PlaceDeletedListener.class);

   private final HubDAO hubDao;
   private final HubDeleteHandler deleteHandler;

   @Inject
   public PlaceDeletedListener(HubDAO hubDao, HubDeleteHandler deleteHandler) {
      this.hubDao = hubDao;
      this.deleteHandler = deleteHandler;
   }

   @Override
   public String getEventType() {
      return Capability.EVENT_DELETED;
   }

   @Override
   public void handleEvent(Hub context, PlatformMessage message) {
      logger.trace("Unhandled event [{}]", message);
   }

   @Override
   public void handleStaticEvent(PlatformMessage msg) {
      Address address = msg.getSource();
      if(PlatformConstants.SERVICE_PLACES.equals(address.getGroup())) {
         onPlaceDeleted((UUID) address.getId());
      }
   }

   private void onPlaceDeleted(UUID placeId) {
      Hub hub = hubDao.findHubForPlace(placeId);
      if(hub != null) {
         deleteHandler.deleteHub(hub);
      }
   }
}


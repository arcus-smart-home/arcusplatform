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

import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.Utils;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.HubRegistrationDAO;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Hub;
import com.iris.messages.model.Place;
import com.iris.population.PlacePopulationCacheManager;

@Singleton
public class RegisterHubHandler extends AbstractRegisterHubHandler implements ContextualRequestMessageHandler<Place> {
   private static final Logger logger = LoggerFactory.getLogger(RegisterHubHandler.class);

   public static final String MISSING_ARGUMENT_CODE = "error.register.missingargument";
   public static final String MISSING_ARGUMENT_MSG_BASE = "Missing required argument ";
   public static final String ALREADY_REGISTERED_CODE = "error.register.alreadyregistered";
   public static final String ALREADY_REGISTERED_MSG = "This hub is already registred.";
   public static final String HUB_NOTFOUND_CODE = "error.register.hubnotfound";
   public static final String HUB_NOTFOUND_MSG = "The hub could not be found.";
   public static final String ORPHANED_HUB_CODE = "error.register.huborphaned";
   public static final String ORPHANED_HUB_msg = "The hub requires a factory reset before registration";


   @Inject
   public RegisterHubHandler(PlatformMessageBus bus, 
   		HubDAO hubDao,
   		HubRegistrationDAO hubRegistrationDao,
   		PlacePopulationCacheManager populationCacheMgr) {
      super(bus, hubDao, hubRegistrationDao, populationCacheMgr);
   }

   @Override
   public String getMessageType() {
      return MessageConstants.MSG_REGISTER_HUB;
   }

   @Override
   public MessageBody handleRequest(Place context, PlatformMessage msg) {
      Utils.assertNotNull(context, "The place is required");

      MessageBody message = msg.getValue();
      String hubId = (String) message.getAttributes().get("hubId");

      UUID accountId = context.getAccount();

      if(StringUtils.isBlank(hubId)) {
         logger.warn(MISSING_ARGUMENT_MSG_BASE + hubId);
         return Errors.fromCode(MISSING_ARGUMENT_CODE, MISSING_ARGUMENT_MSG_BASE + "hubId");
      }

      Hub hub = hubDao.findById(hubId);
      if(hub == null) {
         logger.warn(HUB_NOTFOUND_MSG + hubId);
         return Errors.fromCode(HUB_NOTFOUND_CODE, HUB_NOTFOUND_MSG);
      }
      if(HubCapability.REGISTRATIONSTATE_ORPHANED.equals(hub.getRegistrationState())) {
         return Errors.fromCode(ORPHANED_HUB_CODE, ORPHANED_HUB_CODE);
      }

      if(hub.getAccount() == null) {
         hub.setAccount(accountId);
         hub.setPlace(context.getId());
         hub.setRegistrationState(HubCapability.REGISTRATIONSTATE_REGISTERED);
         hubDao.save(hub);
         updateHubRegistrationIfNecessary(hub);
         sendHubRegistered(hub.getId(), accountId, context.getId(), context.getPopulation());

      } else if(!Objects.equals(accountId, hub.getAccount()) || !Objects.equals(context.getId(), hub.getPlace())) {
         logger.warn(ALREADY_REGISTERED_MSG);
         return Errors.fromCode(ALREADY_REGISTERED_CODE, ALREADY_REGISTERED_MSG);
      } else {
         emitAddedEvent(hubDao.findHubModel(hub.getId()), msg.getCorrelationId());
         return PlaceCapability.RegisterHubResponse.instance();
      }

      return PlaceCapability.RegisterHubResponse.instance();
   }
}


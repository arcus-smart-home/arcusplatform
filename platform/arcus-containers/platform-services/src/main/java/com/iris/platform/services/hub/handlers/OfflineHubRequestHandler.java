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

import java.util.Optional;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.HubDAO;
import com.iris.core.platform.AbstractPlatformMessageListener;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.address.HubServiceAddress;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Hub;
import com.iris.messages.model.Model;
import com.iris.messages.model.hub.HubModel;
import com.iris.platform.services.hub.HubRegistry;

@Singleton
public class OfflineHubRequestHandler extends AbstractPlatformMessageListener {
   public static final String PROP_THREADPOOL = "platform.service.huboffline.threadpool";
   private static final Logger logger = LoggerFactory.getLogger(OfflineHubRequestHandler.class);

   private final HubRegistry registry;
   private final HubDAO hubDao;
   private final HubDeleteHandler hubDeleteHandler;
   private final BeanAttributesTransformer<Hub> hubTransformer;

   @Inject
   public OfflineHubRequestHandler(
         PlatformMessageBus platformBus,
         @Named(PROP_THREADPOOL) Executor executor,
         HubRegistry registry,
         HubDAO hubDao,
         HubDeleteHandler hubDeleteHandler,
         BeanAttributesTransformer<Hub> hubTransformer
   ) {
      super(platformBus, executor);
      this.registry = registry;
      this.hubDao = hubDao;
      this.hubDeleteHandler = hubDeleteHandler;
      this.hubTransformer = hubTransformer;
   }

   @Override
   protected void onStart() {
      addListeners(AddressMatchers.hubNamespaces(MessageConstants.SERVICE));
   }

   /* (non-Javadoc)
    * @see com.iris.core.platform.AbstractPlatformMessageListener#onMessage(com.iris.messages.PlatformMessage)
    */
   @Override
   public void onMessage(PlatformMessage message) {
      if(message.isRequest()) {
         super.onMessage(message);
      }
      // else ignore it
   }

   /* (non-Javadoc)
    * @see com.iris.core.platform.AbstractPlatformMessageListener#handleMessage(com.iris.messages.PlatformMessage)
    */
   @Override
   protected void handleMessage(PlatformMessage request) {
      getMessageBus().invokeAndSendIfNotNull(request, () -> {

         HubServiceAddress hubAddress = (HubServiceAddress) request.getDestination();

         // all delete requests are handled here, regardless of
         if(HubCapability.DeleteRequest.NAME.equals(request.getMessageType())) {
            Model hub = hubDao.findHubModel(hubAddress.getHubId());
            Errors.assertPlaceMatches(request, HubModel.getPlace(hub));
            MessageBody response = hubDeleteHandler.handleRequest(hubTransformer.transform(hub.toMap()), request);
            return Optional.of(response);
         }

         if(registry.isOnline(hubAddress.getHubId())) {
            // not our problem
            return Optional.empty();
         }

         String hubId = hubAddress.getHubId();
         Model hub = hubDao.findHubModel(hubId);
         if(hub == null) {
            // FIXME uncomment this when ITWO-3929 is resolved
            //throw new NotFoundException(hubAddress);

            logger.warn("Received request for unrecognized hub: [{}]", hubId);
            return Optional.empty();
         }

         if(!HubCapability.STATE_DOWN.equals(HubModel.getState(hub))) {
            // this shouldn't happen, but if it isn't fully offline yet, don't respond
            return Optional.empty();
         }

         Errors.assertPlaceMatches(request, HubModel.getPlace(hub));
         if(Capability.CMD_GET_ATTRIBUTES.equals(request.getMessageType())) {
            MessageBody message = MessageBody.buildMessage(Capability.EVENT_GET_ATTRIBUTES_RESPONSE, hub.toMap());
            return Optional.of(message);
         }
         else {
            return Optional.of(Errors.hubOffline());
         }
      });
   }

}


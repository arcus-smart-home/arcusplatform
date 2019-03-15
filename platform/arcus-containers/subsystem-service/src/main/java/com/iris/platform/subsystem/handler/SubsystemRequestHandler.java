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
/**
 * 
 */
package com.iris.platform.subsystem.handler;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.common.subsystem.SubsystemExecutor;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.PlaceCapability.DeleteRequest;
import com.iris.messages.listener.annotation.OnMessage;
import com.iris.messages.listener.annotation.Request;
import com.iris.platform.subsystem.SubsystemRegistry;

@Singleton
public class SubsystemRequestHandler {
	private static final Logger logger = LoggerFactory.getLogger(SubsystemRequestHandler.class);
	
	private final SubsystemRegistry registry;
	
	@Inject
	public SubsystemRequestHandler(SubsystemRegistry registry) {
		this.registry = registry;
	}
	
	@Request(value = MessageConstants.MSG_ANY_MESSAGE_TYPE, response = false)
	public void handleRequest(SubsystemExecutor executor, PlatformMessage message) {
		// dispatch to subsystem which handles sending the proper response
		executor.onPlatformMessage(message);
	}

	@OnMessage(types = MessageConstants.MSG_ANY_MESSAGE_TYPE)
	public void onMessage(Optional<SubsystemExecutor> executorRef, PlatformMessage message) {
		if(executorRef.isPresent()) {
			SubsystemExecutor executor = executorRef.get();
			UUID placeId = executor.context().getPlaceId();
         if(isDeletePlace(message, placeId)) {
         	logger.debug("Place [{}] has been deleted, removing subsystem", placeId);
            registry.removeByPlace(placeId);
            executor.delete();
         }
         else {
            executor.onPlatformMessage(message);
         }
		}
	}

   private boolean isDeletePlace(PlatformMessage message, UUID placeId) {
      if(!DeleteRequest.NAME.equals(message.getMessageType())) {
         return false;
      }
      Address source = message.getSource();
      if(!Address.platformService(placeId, PlaceCapability.NAMESPACE).equals(source)) {
         return false;
      }
      
      return true;
   }

}


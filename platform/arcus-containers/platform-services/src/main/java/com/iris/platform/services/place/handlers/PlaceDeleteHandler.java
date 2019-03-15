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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.Place;
import com.iris.platform.services.PlaceDeleter;

@Singleton
public class PlaceDeleteHandler implements ContextualRequestMessageHandler<Place> {

   private static final Logger logger = LoggerFactory.getLogger(PlaceDeleteHandler.class);

   private final PlaceDeleter placeDeleter;

   @Inject
   public PlaceDeleteHandler(PlaceDeleter placeDeleter) {      
      this.placeDeleter = placeDeleter;
   }

   @Override
   public String getMessageType() {
      return PlaceCapability.DeleteRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Place context, PlatformMessage msg) {
      return placeDeleter.deletePlace(context, true);
   }
   
}


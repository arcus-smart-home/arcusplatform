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
package com.iris.platform.subsystem.incident;

import java.util.UUID;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.errors.Errors;
import com.iris.messages.listener.annotation.Request;
import com.iris.platform.alarm.incident.AlarmIncident;
import com.iris.platform.alarm.incident.AlarmIncidentDAO;

/**
 * 
 */
@Singleton
public class GetAttributesHandler {
   private final AlarmIncidentDAO incidentDao;
   
   @Inject
   public GetAttributesHandler(AlarmIncidentDAO incidentDao) {
      this.incidentDao = incidentDao;
   }
   
   
   @Request(value=Capability.GetAttributesRequest.NAME)
   public MessageBody getAttributes(PlatformMessage message) {
   	UUID placeId = UUID.fromString(message.getPlaceId());
   	AlarmIncident incident = incidentDao.findById(placeId, (UUID) message.getDestination().getId());
   	Errors.assertFound(incident, message.getDestination());
      return MessageBody.buildMessage(Capability.EVENT_GET_ATTRIBUTES_RESPONSE, incident.asMap());
   }
   
}


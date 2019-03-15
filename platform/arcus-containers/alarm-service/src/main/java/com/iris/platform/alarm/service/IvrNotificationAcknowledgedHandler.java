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
package com.iris.platform.alarm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.common.alarm.AlertType;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.listener.annotation.OnMessage;
import com.iris.messages.model.Place;
import com.iris.platform.alarm.incident.AlarmIncident;
import com.iris.platform.alarm.incident.AlarmIncidentDAO;
import com.iris.platform.alarm.notification.strategy.NotificationConstants;
import com.iris.platform.alarm.notification.strategy.NotificationStrategyRegistry;

@Singleton
public class IvrNotificationAcknowledgedHandler {

   private static final Logger logger = LoggerFactory.getLogger(IvrNotificationAcknowledgedHandler.class);

   private final NotificationStrategyRegistry notificationStrategyRegistry;
   private final AlarmIncidentDAO incidentDAO;

   @Inject
   public IvrNotificationAcknowledgedHandler(NotificationStrategyRegistry notificationStrategyRegistry, AlarmIncidentDAO incidentDAO) {
      this.notificationStrategyRegistry = notificationStrategyRegistry;
      this.incidentDAO = incidentDAO;
   }

   @OnMessage(types = { NotificationCapability.IvrNotificationAcknowledgedEvent.NAME })
   public void onIvrAcknowledged(
         Place place,
         @Named(NotificationCapability.IvrNotificationAcknowledgedEvent.ATTR_MSGKEY) String msgKey
   ) {
      logger.trace("handling incoming ivr acknowledgement for {} with key {}", place.getId(), msgKey);
      if(msgKey == null) {
         return;
      }
      switch(msgKey) {
         case NotificationConstants.SECURITY_KEY:
            doAcknowledge(place, AlertType.SECURITY);
            break;
         default: /* no op */
      }
   }

   private void doAcknowledge(Place place, AlertType type) {
      AlarmIncident incident = incidentDAO.current(place.getId());
      if(incident != null) {
         notificationStrategyRegistry.forPlace(place).acknowledge(incident.getAddress(), type);
      }
   }

}


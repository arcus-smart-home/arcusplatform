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

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.Place;
import com.iris.messages.service.AlarmService;
import com.iris.platform.alarm.incident.Trigger;
import com.iris.platform.alarm.notification.strategy.NotificationStrategyRegistry;

@Singleton
public class AddAlarmHandler {

   private static final Logger logger = LoggerFactory.getLogger(AddAlarmHandler.class);

   private final NotificationStrategyRegistry notificationStrategyRegistry;

   @Inject
   public AddAlarmHandler(NotificationStrategyRegistry notificationStrategyRegistry) {
      this.notificationStrategyRegistry = notificationStrategyRegistry;
   }

   @Request(value= AlarmService.AddAlarmRequest.NAME, service = true)
   public MessageBody updateIncident(
      PlatformMessage msg,
      Place place,
      @Named(AlarmService.AddAlarmRequest.ATTR_ALARM) String alarm,
      @Named(AlarmService.AddAlarmRequest.ATTR_ALARMS) List<String> alarms,
      @Named(AlarmService.AddAlarmRequest.ATTR_TRIGGERS) List<Map<String, Object>> triggers
   ) {
      logger.trace("incoming alert request from {} for alarm {}", msg.getSource(), alarm);
      Preconditions.checkNotNull(place, "place is required");
      AlertUtil.validateTriggers(AlarmService.AddAlarmRequest.ATTR_TRIGGERS, triggers);

      List<Trigger> convertedTriggers = AlertUtil.convertTriggers(triggers);
      notificationStrategyRegistry.forPlace(place).execute(msg.getSource(), place.getId(), convertedTriggers);

      return AlarmService.AddAlarmResponse.instance();
   }

}


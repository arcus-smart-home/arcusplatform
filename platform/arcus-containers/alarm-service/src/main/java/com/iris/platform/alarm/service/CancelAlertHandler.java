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
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.Place;
import com.iris.messages.service.AlarmService;
import com.iris.messages.type.IncidentTrigger;
import com.iris.platform.alarm.notification.strategy.NotificationStrategyRegistry;

@Singleton
public class CancelAlertHandler {

   public static final String CANCEL_FAILED = "alarm.cancel.failed";

   private static final Logger logger = LoggerFactory.getLogger(CancelAlertHandler.class);

   private final NotificationStrategyRegistry notificationStrategyRegistry;

   @Inject
   public CancelAlertHandler(NotificationStrategyRegistry notificationStrategyRegistry) {
      this.notificationStrategyRegistry = notificationStrategyRegistry;
   }

   @Request(value= AlarmService.CancelAlertRequest.NAME, service = true)
   public MessageBody cancelAlert(
      PlatformMessage msg,
      Place place,
      @Named(AlarmService.CancelAlertRequest.ATTR_METHOD) String method,
      @Named(AlarmService.CancelAlertRequest.ATTR_ALARMS) List<String> alarms
   ) {
      logger.trace("incoming cancel alert request from {}", msg.getSource());
      Preconditions.checkNotNull(place, "place is required");
      validateMethod(method);
      validateAlarms(alarms);

      if(notificationStrategyRegistry.forPlace(place).cancel(msg.getSource(), place.getId(), msg.getActor(), alarms)) {
         return AlarmService.CancelAlertResponse.instance();
      }

      return Errors.fromCode(CANCEL_FAILED, "alarm could not be canceled");
   }

   private void validateMethod(String method) {
      if(method == null) {
         throw new ErrorEventException(Errors.missingParam(AlarmService.CancelAlertRequest.ATTR_METHOD));
      }
      try {
         AlarmService.CancelAlertRequest.TYPE_METHOD.coerce(method);
      } catch(IllegalArgumentException iae) {
         throw new ErrorEventException(Errors.invalidParam(AlarmService.CancelAlertRequest.ATTR_METHOD));
      }
      if(!Objects.equals(method, AlarmService.CancelAlertRequest.METHOD_APP) &&
         !Objects.equals(method, AlarmService.CancelAlertRequest.METHOD_KEYPAD)) {

         throw new ErrorEventException(Errors.invalidParam(AlarmService.CancelAlertRequest.ATTR_METHOD));
      }
   }

   private void validateAlarms(List<String> alarms) {
      if(alarms == null || alarms.isEmpty()) {
         throw new ErrorEventException(Errors.missingParam(AlarmService.CancelAlertRequest.ATTR_ALARMS));
      }
      alarms.forEach((s) -> {
         try {
            IncidentTrigger.TYPE_ALARM.coerce(s);
         } catch(IllegalArgumentException iae) {
            throw new ErrorEventException(Errors.invalidParam(AlarmService.CancelAlertRequest.ATTR_ALARMS));
         }
      });
   }
}


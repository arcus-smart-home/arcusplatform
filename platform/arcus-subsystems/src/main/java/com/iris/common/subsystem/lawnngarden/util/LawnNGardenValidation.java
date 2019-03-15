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
package com.iris.common.subsystem.lawnngarden.util;

import org.apache.commons.lang3.StringUtils;

import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.lawnngarden.model.schedules.ScheduleMode;
import com.iris.common.time.TimeOfDay;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.event.ModelEvent;
import com.iris.messages.model.subs.LawnNGardenSubsystemModel;

public class LawnNGardenValidation {

   public static final String CODE_INVALID_STATE = "lawnngarden.scheduling.invalid_state";
   public static final String CODE_NOEVENT_FOUND = "lawnngarden.scheduling.no_event_found";
   public static final String CODE_NOEVENTS = "lawnngarden.scheduling.no_events";
   public static final String CODE_INTERVAL_NOTCONFIGURED = "lawnngarden.scheduling.interval_notconfigured";
   public static final String CODE_SCHEDULE_OVERLAPS = "lawnngarden.scheduling.has_overlaps";
   public static final String CODE_SCHEDULE_MAXTRANSITIONS = "lawnngarden.scheduling.max_transitions_exceeded";

   private LawnNGardenValidation() {
   }

   public static void validateRequiredParam(Object o, String param) {
      if(o == null) {
         throw new ErrorEventException(Errors.CODE_MISSING_PARAM, param + " is required");
      }
   }

   public static void validateDays(Integer days) {
      validateRequiredParam(days, "days");
      if(days < 1 || days > 254) {
         throw new ErrorEventException(Errors.CODE_INVALID_PARAM, "days must be between 1n and 254");
      }
   }

   public static String getAndValidateEventId(MessageBody body, String attr) {
      String eventId = LawnNGardenTypeUtil.string(body.getAttributes().get(attr));
      validateEventId(eventId);
      return eventId;
   }

   public static void validateEventId(String eventId) {
      validateRequiredParam(StringUtils.trimToNull(eventId), "eventId");
   }

   public static TimeOfDay validateTimeOfDay(String timeOfDay) {
      validateRequiredParam(StringUtils.trimToNull(timeOfDay), "timeOfDay");
      String tod = ensureTimeOfDaySeconds(timeOfDay);
      try {
         return LawnNGardenTypeUtil.timeOfDay(tod);
      } catch(IllegalArgumentException iae) {
         throw new ErrorEventException(Errors.CODE_INVALID_PARAM, "timeOfDay must be HH:mm or HH:mm:ss");
      }
   }

   private static String ensureTimeOfDaySeconds(String timeOfDay) {
      if(StringUtils.countMatches(timeOfDay, ":") != 2) {
         return timeOfDay + ":00";
      }
      return timeOfDay;
   }

   public static ScheduleMode getAndValidateMode(MessageBody body, String attribute) {
      return getAndValidateMode(body, attribute, null);
   }

   public static ScheduleMode getAndValidateMode(MessageBody body, String attribute, ScheduleMode disallowed) {
      ScheduleMode type = LawnNGardenTypeUtil.scheduleMode(body.getAttributes().get(attribute));
      validateRequiredParam(type, "mode");
      if(type == disallowed) {
         throw new ErrorEventException(Errors.CODE_INVALID_PARAM, "mode " + type + " is invalid");
      }
      return type;
   }

   public static Address getAndValidateController(MessageBody body, String attr, SubsystemContext<LawnNGardenSubsystemModel> context) {
      Address controller = LawnNGardenTypeUtil.address(body.getAttributes().get(attr));
      validateController(controller, context);
      return controller;
   }

   public static Address getAndValidateController(ModelEvent event, SubsystemContext<LawnNGardenSubsystemModel> context) {
      Address controller = event.getAddress();
      validateController(controller, context);
      return controller;
   }

   public static Address getAndValidateController(PlatformMessage msg, SubsystemContext<LawnNGardenSubsystemModel> context) {
      Address controller = msg.getSource();
      validateController(controller, context);
      return controller;
   }

   public static void validateController(Address controller, SubsystemContext<LawnNGardenSubsystemModel> context) {
      validateRequiredParam(controller, "controller");
      if(!controllerExists(controller, context)) {
         throw new ErrorEventException(Errors.CODE_INVALID_PARAM, "controller " + controller.getRepresentation() + " does not exist");
      }
   }

   private static boolean controllerExists(Address address, SubsystemContext<LawnNGardenSubsystemModel> context) {
      return context.model().getControllers().contains(address.getRepresentation());
   }

}


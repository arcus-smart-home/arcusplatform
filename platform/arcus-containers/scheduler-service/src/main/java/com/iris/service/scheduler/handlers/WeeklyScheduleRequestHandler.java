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
package com.iris.service.scheduler.handlers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.sunrise.GeoLocation;
import com.iris.common.sunrise.SunriseSunsetCalc;
import com.iris.common.sunrise.SunriseSunsetInfo;
import com.iris.common.time.DayOfWeek;
import com.iris.common.time.TimeOfDay;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.SchedulerCapability;
import com.iris.messages.capability.WeeklyScheduleCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Model;
import com.iris.messages.model.ModelUtils;
import com.iris.messages.model.serv.ScheduleModel;
import com.iris.messages.model.serv.SchedulerModel;
import com.iris.messages.type.TimeOfDayCommand;
import com.iris.service.scheduler.SchedulerContext;
import com.iris.service.scheduler.model.TimeOfDayScheduledCommand;
import com.iris.service.scheduler.model.TimeOfDayScheduledCommand.Mode;

/**
 *
 */
public class WeeklyScheduleRequestHandler {
   private static final Comparator<Map<String, Object>> SORT_BY_TIME =
         new Comparator<Map<String,Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
               TimeOfDay tod1 = TimeOfDay.fromString((String) o1.get(TimeOfDayCommand.ATTR_TIME));
               TimeOfDay tod2 = TimeOfDay.fromString((String) o2.get(TimeOfDayCommand.ATTR_TIME));
               return tod1.compareTo(tod2);
            }
         };

   // TODO turn this into a builder
   public static WeeklyScheduleRequestHandler fromContext(String instanceId, SchedulerContext context) {
      return new WeeklyScheduleRequestHandler(
            context.getCalculator(),
            instanceId,
            context.getScheduler(),
            context.getTimeZone(),
            context.getLocation()
      );
   }

   private final SunriseSunsetCalc calculator;
   private String instanceId;
   private Model model;
   private Map<String, TimeOfDayCommand> commands;
   private TimeZone tz;
   @Nullable private GeoLocation location;

   private WeeklyScheduleRequestHandler(SunriseSunsetCalc calculator, String instanceId, Model model, TimeZone tz, GeoLocation location) {
      this.calculator = calculator;
      this.instanceId = instanceId;
      this.model = model;
      this.tz = tz;
      this.location = location;
      this.commands = new HashMap<>();
      for(Map<String, Object> commandAttributes: SchedulerModel.getCommands(model, ImmutableMap.of()).values()) {
         if(instanceId.equals(commandAttributes.get(TimeOfDayCommand.ATTR_SCHEDULEID))) {
            TimeOfDayCommand command = new TimeOfDayCommand(commandAttributes);
            commands.put(command.getId(), command);
         }
      }
   }

   public boolean isEnabled() {
      return ScheduleModel.getEnabled(instanceId, model, false);
   }

   public boolean isSunriseSunsetSupported() {
      return location != null;
   }

   public void setEnabled(boolean enabled) {
      String group = ScheduleModel.getGroup(instanceId, model);
      if(enabled) {
         for(String instanceId: model.getInstances().keySet()) {
            String otherGroup = ScheduleModel.getGroup(instanceId, model);
            if(StringUtils.equals(group, otherGroup)) {
               ScheduleModel.setEnabled(instanceId, model, false);
            }
         }
      }
      ScheduleModel.setEnabled(instanceId, model, enabled);
      syncNextFireTime(new Date());
   }

   public TimeOfDayCommand addTimeOfDayCommand(TimeOfDayScheduledCommand toAdd) {
      TimeOfDayCommand command = new TimeOfDayCommand();
      command.setId(UUID.randomUUID().toString());
      command.setScheduleId(instanceId);
      command.setMessageType(StringUtils.isEmpty(toAdd.getMessageType()) ? Capability.CMD_SET_ATTRIBUTES : toAdd.getMessageType() );
      command.setAttributes(toAdd.getAttributes());
      if(toAdd.getMode() == null || toAdd.getMode() == Mode.ABSOLUTE) {
         command.setMode(TimeOfDayCommand.MODE_ABSOLUTE);
         command.setTime(toAdd.getTime().toString());
      }
      else {
         command.setMode(toAdd.getMode().toString());
         command.setOffsetMinutes(toAdd.getOffsetMinutes());
         updateRelativeTime(command);
      }
      command.setDays(toAbbrDays(toAdd.getDays()));
      commands.put(command.getId(), command);
      commit();
      return command;
   }

   public void updateTimeOfDayCommand(TimeOfDayScheduledCommand toUpdate) {
      TimeOfDayCommand command = commands.get(toUpdate.getId());
      Errors.assertValidRequest(command != null, "Command not found");

      if(toUpdate.getDays() != null) {
         command.setDays(toAbbrDays(toUpdate.getDays()));
      }
      if(toUpdate.getMode() != null) {
         command.setMode(toUpdate.getMode().toString());
      }
      if(toUpdate.getTime() != null) {
         command.setTime(toUpdate.getTime().toString());
      }
      if(toUpdate.getOffsetMinutes() != null) {
         command.setOffsetMinutes(toUpdate.getOffsetMinutes());
      }
      if(!StringUtils.isEmpty(toUpdate.getMessageType())) {
         command.setMessageType(toUpdate.getMessageType());
      }
      if(toUpdate.getAttributes() != null) {
         command.setAttributes(toUpdate.getAttributes());
      }
      String mode = command.getMode();
      boolean isRelative = mode != null && !TimeOfDayCommand.MODE_ABSOLUTE.equals(mode);
      if(isRelative) {
         command.setTime(null);
      } else {
         command.setOffsetMinutes(null);
      }
      TimeOfDayScheduledCommand.fromMap(command.toMap()).validate();
      if(isRelative) {
         updateRelativeTime(command);
      }
      commit();
   }

   public boolean deleteCommand(String commandId) {
      if(commands.remove(commandId) != null) {
         ModelUtils.removeFromMap(model, SchedulerCapability.ATTR_COMMANDS, commandId);
         commit();
         return true;
      }
      else {
         return false;
      }
   }

   // commits all changes to the model
   public void commit() {
      for(Map.Entry<String, List<Map<String, Object>>> schedule: toScheduleMap().entrySet()) {
         model.setAttribute(schedule.getKey(), schedule.getValue());
      }
      syncNextFireTime(new Date());
   }

   protected void syncNextFireTime(Date currentTime) {
      // if it's disabled there is no next fire time
      if(!ScheduleModel.getEnabled(instanceId, model, false)) {
         ScheduleModel.setNextFireTime(instanceId, model, null);
         ScheduleModel.setNextFireCommand(instanceId, model, null);
         return;
      }

      Calendar now = Calendar.getInstance(tz);
      now.setTime(currentTime);

      Calendar nextFireTime = (Calendar) now.clone();
      for(int i=0; i<8; i++) {
         DayOfWeek day = DayOfWeek.from(nextFireTime);
         String key = getKeyFor(day);
         List<Map<String, Object>> commandsForDay = (List<Map<String, Object>>) model.getAttribute(key);
         calculateRelativeTimes(nextFireTime, commandsForDay);
         for(Map<String, Object> commandForDay: commandsForDay) {
            String id = (String) commandForDay.get(TimeOfDayCommand.ATTR_ID);
            TimeOfDay tod = TimeOfDay.fromString((String) commandForDay.get(TimeOfDayCommand.ATTR_TIME));
            nextFireTime.set(Calendar.HOUR_OF_DAY, (int) tod.getHours());
            nextFireTime.set(Calendar.MINUTE, (int) tod.getMinutes());
            nextFireTime.set(Calendar.SECOND, 0);

            if(nextFireTime.after(now)) {
               ScheduleModel.setNextFireTime(instanceId, model, nextFireTime.getTime());
               ScheduleModel.setNextFireCommand(instanceId, model, id);
               return;
            }
         }

         // try the next day
         nextFireTime.add(Calendar.DAY_OF_YEAR, 1);
      }

      // no executable commands?
      ScheduleModel.setNextFireTime(instanceId, model, null);
      ScheduleModel.setNextFireCommand(instanceId, model, null);
   }

   private void calculateRelativeTimes(Calendar now, List<Map<String, Object>> commandsForDay) {
      SunriseSunsetInfo info = null;
      for(Map<String, Object> command: commandsForDay) {
         String mode = (String) command.get(TimeOfDayCommand.ATTR_MODE);
         if(mode == null || TimeOfDayCommand.MODE_ABSOLUTE.equals(mode)) {
            // not a relative command
            continue;
         }

         if(info == null) { // only calculate it if its needed
            info = calculator.calculateSunriseSunset(now, location);
         }
         updateRelativeTime(info, command);
      }
      Collections.sort(commandsForDay, SORT_BY_TIME);
   }

   protected void updateRelativeTime(TimeOfDayCommand command) {
      Errors.assertValidRequest(isSunriseSunsetSupported(), "The place's location must be set in order to use sunrise / sunset");
      SunriseSunsetInfo info = calculator.calculateSunriseSunset(Calendar.getInstance(tz), location);
      Calendar time;
      if(TimeOfDayCommand.MODE_SUNRISE.equals(command.getMode())) {
         time = (Calendar) info.getSunrise().clone();
      }
      else {
         time = (Calendar) info.getSunset().clone();
      }
      time.add(Calendar.MINUTE, command.getOffsetMinutes() == null ? 0 : command.getOffsetMinutes());
      command.setTime(TimeOfDay.fromCalendar(time).toString());
   }

   private void updateRelativeTime(SunriseSunsetInfo info, Map<String, Object> command) {
      String mode = (String) command.get(TimeOfDayCommand.ATTR_MODE);
      int offsetMinutes =
            Optional
               .fromNullable( (Integer) TimeOfDayCommand.TYPE_OFFSETMINUTES.coerce( command.get(TimeOfDayCommand.ATTR_OFFSETMINUTES) ) )
               .or(0);
      Calendar time;
      if(TimeOfDayCommand.MODE_SUNRISE.equals(mode)) {
         time = (Calendar) info.getSunrise().clone();
      }
      else {
         time = (Calendar) info.getSunset().clone();
      }
      time.add(Calendar.MINUTE, offsetMinutes);
      command.put(TimeOfDayCommand.ATTR_TIME, TimeOfDay.fromCalendar(time).toString());
   }

   private Map<String, List<Map<String, Object>>> toScheduleMap() {
      Map<String, List<Map<String, Object>>> commandsByDayAndTime = new HashMap<>();
      for(DayOfWeek day: DayOfWeek.values()) {
         String key = getKeyFor(day);
         commandsByDayAndTime.put(key, new ArrayList<Map<String, Object>>());
      }

      for(TimeOfDayCommand command: commands.values()) {
         Map<String, Object> attributes = command.toMap();
         for(String day: command.getDays()) {
            commandsByDayAndTime.get(getKeyForDay(day)).add(attributes);
         }
      }

      for(List<Map<String, Object>> commandsForDay: commandsByDayAndTime.values()) {
         Collections.sort(commandsForDay, SORT_BY_TIME);
      }
      return commandsByDayAndTime;
   }

   private Set<String> toAbbrDays(Set<DayOfWeek> days) {
      if(days == null || days.isEmpty()) {
         return ImmutableSet.<String>of();
      }

      Set<String> value = new HashSet<>(2 * days.size());
      for(DayOfWeek day: days) {
         value.add(day.name().substring(0, 3));
      }
      return value;
   }

   private String getKeyFor(DayOfWeek day) {
      return getKeyForDay(day.name().substring(0, 3));
   }

   private String getKeyForDay(String day) {
      return WeeklyScheduleCapability.NAMESPACE + ":" + day.toLowerCase() + ":" + instanceId;
   }

}


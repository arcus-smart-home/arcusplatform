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
package com.iris.service.scheduler.handlers;

import static com.iris.messages.type.TimeOfDayCommand.MODE_ABSOLUTE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.rule.time.DayOfWeek;
import com.iris.common.sunrise.GeoLocation;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ScheduleCapability;
import com.iris.messages.capability.WeeklyScheduleCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Model;
import com.iris.messages.model.ModelUtils;
import com.iris.messages.model.serv.ScheduleModel;
import com.iris.messages.model.serv.SchedulerModel;
import com.iris.messages.model.serv.WeeklyScheduleModel;
import com.iris.messages.type.TimeOfDayCommand;
import com.iris.service.scheduler.SchedulerContext;
import com.iris.service.scheduler.SchedulerErrors;
import com.iris.service.scheduler.model.TimeOfDayScheduledCommand;
import com.iris.util.TypeMarker;

public class SchedulerRequestHandler {
   private static final Logger logger = LoggerFactory.getLogger(SchedulerRequestHandler.class);
   
   public static SchedulerRequestHandler fromContext(SchedulerContext context) {
      return new SchedulerRequestHandler(context);
   }
   
   private SchedulerContext context;

   private SchedulerRequestHandler(SchedulerContext context) {
      this.context = context;
   }
   
   public Optional<MessageBody> fire(String commandId, Date currentTime) {
      try {
         Model scheduler = context.getScheduler();

         Map<String, Object> attributes =
               SchedulerModel
                  .getCommands(scheduler, ImmutableMap.of())
                  .get(commandId);
         
         if(attributes == null) {
            logger.warn("No command with id [{}], unable to fire", commandId);
            return Optional.absent();
         }
         
         TimeOfDayCommand command = new TimeOfDayCommand(attributes);
         String scheduleId = command.getScheduleId();
         if(!scheduler.getInstances().containsKey(scheduleId)) {
            logger.warn("No schedule with id [{}], unable to fire", scheduleId);
            return Optional.absent();
         }
         
         logger.debug(
               "Sending schedule message from command [{}] for place time [{}] to [{}]", 
               command.getId(), command.getTime(), SchedulerModel.getTarget(scheduler)
         );
         
         Date now = new Date();
         SchedulerModel.setLastFireSchedule(scheduler, scheduleId);
         SchedulerModel.setLastFireTime(scheduler, now);
         ScheduleModel.setLastFireTime(scheduleId, scheduler, now);
         ScheduleModel.setLastFireCommand(scheduleId, scheduler, command.getId());
         ScheduleModel.setLastFireMessageType(scheduleId, scheduler, command.getMessageType());
         ScheduleModel.setLastFireAttributes(scheduleId, scheduler, command.getAttributes());
         
         WeeklyScheduleRequestHandler weeklyScheduleRequestHandler =
            WeeklyScheduleRequestHandler.fromContext(scheduleId, context);
         weeklyScheduleRequestHandler.syncNextFireTime(currentTime);
         
         if (command.getMode() != null && !command.getMode().equals(MODE_ABSOLUTE))
         {
            weeklyScheduleRequestHandler.updateRelativeTime(command);

            Map<String, Map<String, Object>> commands = SchedulerModel.getCommands(scheduler);
            commands.put(commandId, command.toMap());
            SchedulerModel.setCommands(scheduler, commands);
         }
         
         MessageBody request =
               MessageBody.buildMessage(command.getMessageType(), command.getAttributes());
         return Optional.of(request);
      }
      finally {
         syncNextFireTime(currentTime);
      }
   }

   
   public WeeklyScheduleRequestHandler addWeeklySchedule(String scheduleId, String group) {
      Model scheduler = context.getScheduler();
      Set<String> caps = scheduler.getInstances().get(scheduleId);
      if(caps != null && !caps.contains(WeeklyScheduleCapability.NAMESPACE)) {
         throw new ErrorEventException(SchedulerErrors.typeConflict());
      }
      
      if(caps == null) {
         ModelUtils.putInMap(scheduler, Capability.ATTR_INSTANCES, scheduleId, ImmutableSet.of(ScheduleCapability.NAMESPACE, WeeklyScheduleCapability.NAMESPACE));
         WeeklyScheduleModel.setGroup(scheduleId, scheduler, StringUtils.isEmpty(group) ? scheduleId : group);
         WeeklyScheduleModel.setMon(scheduleId, scheduler, ImmutableList.<Map<String,Object>>of());
         WeeklyScheduleModel.setTue(scheduleId, scheduler, ImmutableList.<Map<String,Object>>of());
         WeeklyScheduleModel.setWed(scheduleId, scheduler, ImmutableList.<Map<String,Object>>of());
         WeeklyScheduleModel.setThu(scheduleId, scheduler, ImmutableList.<Map<String,Object>>of());
         WeeklyScheduleModel.setFri(scheduleId, scheduler, ImmutableList.<Map<String,Object>>of());
         WeeklyScheduleModel.setSat(scheduleId, scheduler, ImmutableList.<Map<String,Object>>of());
         WeeklyScheduleModel.setSun(scheduleId, scheduler, ImmutableList.<Map<String,Object>>of());
         
         boolean enable = true;
         if(!StringUtils.isEmpty(group)) {
            for(String instanceId: scheduler.getInstances().keySet()) {
               if(group.equals(ScheduleModel.getGroup(instanceId, scheduler))) {
                  enable = false;
                  break;
               }
            }
         }
         WeeklyScheduleModel.setEnabled(scheduleId, scheduler, enable);
      }
      
      return WeeklyScheduleRequestHandler.fromContext(scheduleId, context);
   }
   
   public void deleteSchedule(String scheduleId) {
      Model scheduler = context.getScheduler();
      ModelUtils.removeFromMap(scheduler, Capability.ATTR_INSTANCES, scheduleId);
      WeeklyScheduleModel.setEnabled(scheduleId, scheduler, null);
      WeeklyScheduleModel.setGroup(scheduleId, scheduler, null);
      WeeklyScheduleModel.setMon(scheduleId, scheduler, null);
      WeeklyScheduleModel.setTue(scheduleId, scheduler, null);
      WeeklyScheduleModel.setWed(scheduleId, scheduler, null);
      WeeklyScheduleModel.setThu(scheduleId, scheduler, null);
      WeeklyScheduleModel.setFri(scheduleId, scheduler, null);
      WeeklyScheduleModel.setSat(scheduleId, scheduler, null);
      WeeklyScheduleModel.setSun(scheduleId, scheduler, null);
      sync();
   }
   
   public List<String> scheduleCommands(Optional<String> group, List<TimeOfDayScheduledCommand> commands) {
      if(commands == null || commands.isEmpty()) {
         return ImmutableList.of();
      }
      
      List<String> commandIds = new ArrayList<String>();
      for(TimeOfDayScheduledCommand command: commands) {
         WeeklyScheduleRequestHandler schedule = addWeeklySchedule(command.getScheduleId(), group.orNull());
         boolean isCreate = StringUtils.isEmpty(command.getId());
         boolean isDelete = !isCreate && command.getDays() != null && command.getDays().isEmpty();
         if(isCreate) {
            String id = schedule.addTimeOfDayCommand(command).getId();
            commandIds.add(id);
         }
         else if(isDelete) {
            schedule.deleteCommand(command.getId());
            commandIds.add(command.getId());
         }
         else {
            schedule.updateTimeOfDayCommand(command);
         }
         sync(); // TODO defer this until the end, currently we reset the weekly handler every iteration through the loop
      }
      return commandIds;
   }
      
   public TimeOfDayCommand scheduleWeeklyCommand(String scheduleId, TimeOfDayScheduledCommand toAdd) {
      Errors.assertValidRequest(toAdd.getId() == null, "This can only be used to schedule new commands");
      WeeklyScheduleRequestHandler schedule = addWeeklySchedule(scheduleId, null);
      TimeOfDayCommand command = schedule.addTimeOfDayCommand(toAdd);
      sync();
      return command;
   }

   public void updateWeeklyCommand(String scheduleId, TimeOfDayScheduledCommand toUpdate) {
      WeeklyScheduleRequestHandler schedule = WeeklyScheduleRequestHandler.fromContext(scheduleId, context);
      schedule.updateTimeOfDayCommand(toUpdate);
      sync();
   }
   
   public void deleteCommand(String scheduleId, String commandId) {
      WeeklyScheduleRequestHandler schedule = WeeklyScheduleRequestHandler.fromContext(scheduleId, context);
      schedule.deleteCommand(commandId);
      sync();
   }
   
   public void setTimeZone(TimeZone tz) {
      this.context.setTimeZone(tz);
      updated();
   }

   public void setLocation(GeoLocation location) {
      this.context.setLocation(location);
      updated();
   }

   public void updated() {
      for(Map.Entry<String, Set<String>> instance: context.getScheduler().getInstances().entrySet()) {
         String scheduleId = instance.getKey();
         Set<String> caps = instance.getValue();
         if(caps == null || !caps.contains(WeeklyScheduleCapability.NAMESPACE)) {
            continue;
         }
         
         WeeklyScheduleRequestHandler
            .fromContext(scheduleId, context)
            .syncNextFireTime(new Date());
      }
      sync();
   }

   protected void sync() {
      syncCommands();
      syncNextFireTime(new Date());
   }

   protected void syncCommands() {
      Model scheduler = context.getScheduler();
      Map<String, Map<String, Object>> commands = new HashMap<String, Map<String,Object>>();
      for(String instanceId: scheduler.getInstances().keySet()) {
         for(String day: Arrays.asList("mon", "tue", "wed", "thu", "fri", "sat", "sun")) {
            List<Map<String, Object>> commandsForDay = scheduler.getAttribute(
                  new TypeMarker<List<Map<String, Object>>>() {},
                  WeeklyScheduleCapability.NAMESPACE + ":" + day + ":" + instanceId,
                  ImmutableList.of()
            );
            for(Map<String, Object> command: commandsForDay) {
               String id = (String) command.get(TimeOfDayCommand.ATTR_ID);
               commands.put(id, command);
            }
         }
      }
      SchedulerModel.setCommands(scheduler, commands);
   }
   
   protected void syncNextFireTime(Date currentTime) {
      Model scheduler = context.getScheduler();
      Date nextFireTime = null;
      String nextFireSchedule = null;
      for(String instanceId: scheduler.getInstances().keySet()) {
         if(!ScheduleModel.getEnabled(instanceId, scheduler, false)) {
            continue;
         }
         
         Date scheduleNextFireTime = ScheduleModel.getNextFireTime(instanceId, scheduler);
         if(scheduleNextFireTime == null) {
            continue;
         }
         
         if(nextFireTime == null || scheduleNextFireTime.before(nextFireTime)) {
            nextFireTime = scheduleNextFireTime;
            nextFireSchedule = instanceId;
         }
      }
      SchedulerModel.setNextFireTime(scheduler, nextFireTime);
      SchedulerModel.setNextFireSchedule(scheduler, nextFireSchedule);
      
   }
   
   protected void updateDays(
         TimeOfDayCommand command,
         Set<DayOfWeek> days
   ) {
      Set<String> daysToWrite = new HashSet<>(days.size());
      for(DayOfWeek day: days) {
         daysToWrite.add(day.name().substring(0, 3));
      }
      command.setDays(daysToWrite);
   }

}


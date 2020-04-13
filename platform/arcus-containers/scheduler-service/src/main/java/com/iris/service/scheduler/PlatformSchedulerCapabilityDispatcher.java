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
package com.iris.service.scheduler;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.google.common.base.Optional;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.capability.key.NamespacedKey;
import com.iris.common.sunrise.GeoLocation;
import com.iris.core.messaging.SingleThreadDispatcher;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.ScheduleCapability;
import com.iris.messages.capability.SchedulerCapability;
import com.iris.messages.capability.WeeklyScheduleCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.event.AddressableEvent;
import com.iris.messages.event.MessageReceivedEvent;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.handler.BaseModelHandler;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.ScheduleModel;
import com.iris.messages.model.serv.SchedulerModel;
import com.iris.messages.service.SchedulerService;
import com.iris.messages.service.SchedulerService.FireCommandRequest;
import com.iris.messages.service.SchedulerService.ScheduleCommandsRequest;
import com.iris.messages.service.SchedulerService.ScheduleWeeklyCommandRequest;
import com.iris.messages.service.SchedulerService.UpdateWeeklyCommandRequest;
import com.iris.messages.type.TimeOfDayCommand;
import com.iris.platform.model.ModelEntity;
import com.iris.platform.scheduler.SchedulerModelDao;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.service.scheduler.handlers.SchedulerRequestHandler;
import com.iris.service.scheduler.model.TimeOfDayScheduledCommand;
import com.iris.util.MdcContext;
import com.iris.util.MdcContext.MdcContextReference;

class PlatformSchedulerCapabilityDispatcher extends BaseModelHandler implements SchedulerCapabilityDispatcher {
   private static final Logger logger = LoggerFactory.getLogger(PlatformSchedulerCapabilityDispatcher.class);
   
   private final SingleThreadDispatcher<AddressableEvent> dispatcher;
   private final SchedulerRequestHandler handler;
   private final PlatformMessageBus platformBus;
   private final SchedulerModelDao schedulerDao;
   private final EventSchedulerService schedulerService;
   private final PlacePopulationCacheManager populationCacheMgr;

   private ModelEntity scheduler;
   private boolean delete = false;

   public PlatformSchedulerCapabilityDispatcher(
         SchedulerContext context,
         PlatformMessageBus platformBus,
         SchedulerModelDao schedulerDao,
         EventSchedulerService schedulerService,
         DefinitionRegistry registry, 
         PlacePopulationCacheManager populationCacheMgr
   ) {
      this.scheduler = context.getScheduler();
      this.platformBus = platformBus;
      this.schedulerDao = schedulerDao;
      this.populationCacheMgr = populationCacheMgr;
      
      this.schedulerService = schedulerService;
      this.handler = SchedulerRequestHandler.fromContext(context);
      this.dispatcher = new SingleThreadDispatcher<>((event) -> handleEvent(event), 100);
      
      super.setDefinitionRegistry(registry);
   }

   @Override
   public Model getScheduler() {
      return scheduler;
   }

   @Override
   public void onPlatformMessage(PlatformMessage message) {
      dispatcher.dispatchOrQueue(new MessageReceivedEvent(message));
   }

   @Override
   public void onScheduledEvent(ScheduledEvent event) {
      dispatcher.dispatchOrQueue(event);
   }
   
   @Override
   public void delete() {
      Date nextFireTime = SchedulerModel.getNextFireTime(scheduler);
      deleteScheduler();
      commit(nextFireTime);
   }

   protected void handleEvent(AddressableEvent event) {
      try(MdcContextReference context = MdcContext.captureMdcContext()) {
         MDC.put(MdcContext.MDC_TARGET, scheduler.getAddress().getRepresentation());
         MDC.put(MdcContext.MDC_PLACE, SchedulerModel.getPlaceId(scheduler));
         
         Date nextFireTime = SchedulerModel.getNextFireTime(scheduler);
         if(event instanceof ScheduledEvent) {
            handleScheduledEvent((ScheduledEvent) event);
         }
         else if(event instanceof MessageReceivedEvent)  {
            handleRequest(((MessageReceivedEvent) event).getMessage());
         }
         commit(nextFireTime);
      }
      catch(Exception e) {
         logger.warn("Error handling request", e);
      }
   }
   
   /* (non-Javadoc)
    * @see com.iris.messages.handler.BaseModelHandler#setAttributes(com.iris.messages.model.Model, java.util.Map)
    */
   @Override
   protected MessageBody setAttributes(Model model, Map<String, Object> attributes) {
      try {
         return super.setAttributes(model, attributes);
      }
      finally {
         handler.updated();
      }
   }

   @Override
   protected void setAttribute(Model model, String name, Object value) throws ErrorEventException {
      super.setAttribute(model, name, value);
      if(name.startsWith(ScheduleCapability.ATTR_ENABLED) && Boolean.TRUE.equals(value)) {
         String enabledInstanceId = NamespacedKey.parse(name).getInstance();
         String group = ScheduleModel.getGroup(enabledInstanceId, model, "");
         for(String instanceId: model.getInstances().keySet()) {
            if(enabledInstanceId.equals(instanceId)) {
               continue;
            }
            if(!group.equals(ScheduleModel.getGroup(instanceId, model))) {
               continue;
            }
            ScheduleModel.setEnabled(instanceId, model, false);
         }
      }
   }

   private void commit(Date previousFireTime) {
   	String placeIdStr = SchedulerModel.getPlaceId(scheduler);
      UUID placeId = UUID.fromString(placeIdStr);
      
      String command;
      Map<String, Object> attributes;
      if(delete) {
         command = Capability.EVENT_DELETED;
         attributes = scheduler.toMap();
         schedulerDao.delete(scheduler);
         schedulerService.cancelEvent(placeId, scheduler.getAddress(), previousFireTime);
      }
      else if(scheduler.isPersisted() && !scheduler.getCreated().equals(scheduler.getModified())) {
         if(!scheduler.isDirty()) {
            return;
         }
         
         command = Capability.EVENT_VALUE_CHANGE;
         attributes = scheduler.getDirtyAttributes();
         scheduler = schedulerDao.save(scheduler);
         Date nextFireTime = SchedulerModel.getNextFireTime(scheduler);
         if(nextFireTime == null) {
            schedulerService.cancelEvent(placeId, scheduler.getAddress(), previousFireTime);
         }
         else if(previousFireTime == null) {
            schedulerService.fireEventAt(placeId, scheduler.getAddress(), nextFireTime);
         }
         else if(previousFireTime != null) {
            schedulerService.rescheduleEventAt(placeId, scheduler.getAddress(), previousFireTime, nextFireTime);
         }
      }
      else {
         scheduler = schedulerDao.save(scheduler);
         command = Capability.EVENT_ADDED;
         attributes = scheduler.toMap();
         Date nextFireTime = SchedulerModel.getNextFireTime(scheduler);
         if(nextFireTime != null) {
            schedulerService.fireEventAt(placeId, scheduler.getAddress(), nextFireTime);
         }
      }
      
      if(attributes.isEmpty()) {
         return;
      }
      
      MessageBody event = 
            MessageBody.buildMessage(command, attributes);
      PlatformMessage message = 
            PlatformMessage
               .builder()
               .from(scheduler.getAddress())
               .withPayload(event)
               .withPlaceId(placeIdStr)
               .withPopulation(populationCacheMgr.getPopulationByPlaceId(placeId))
               .broadcast()
               .create();
      platformBus.send(message);
      scheduler.clearDirty();
   }
   
   private void handleScheduledEvent(ScheduledEvent event) {
      Date nextFireTime = SchedulerModel.getNextFireTime(scheduler, null);
      if(nextFireTime == null) {
         logger.debug("Received scheduled event for disabled schedule: [{}]", event);
         return;
      }
      
      if(nextFireTime.getTime() > event.getScheduledTimestamp()) {
         logger.debug("Ignoring early scheduled event, likely failed to cancel previous event: [{}]", event);
         return;
      }
      
      String scheduleId = SchedulerModel.getNextFireSchedule(scheduler);
      String commandId = ScheduleModel.getNextFireCommand(scheduleId, scheduler, null);
      if(commandId == null) {
         logger.debug("Corrupt config, can't determine command to run, exiting");
         // attempt to re-calculate next fire time
         handler.updated();
         return;
      }

      // advance current time by at least 1 second
      Optional<MessageBody> request = handler.fire(commandId, new Date(event.getScheduledTimestamp() + 1000));
      if(!request.isPresent()) {
         logger.debug("No requests to send for command [{}]", commandId);
         return;
      }
      
      sendRequest(request.get());
   }
   
   private void handleRequest(PlatformMessage message) {
      if(message.getDestination().isBroadcast()) {
         handleUpdate(message);
      }
      else {
         platformBus.invokeAndSendResponse(message, () -> doHandleRequest(message, message.getValue()));
      }
   }
   
   private void handleUpdate(PlatformMessage message) {
      Date nextFireTime = SchedulerModel.getNextFireTime(scheduler);
      if(Capability.EVENT_VALUE_CHANGE.equals(message.getMessageType())) {
         MessageBody value = message.getValue();
         String tzId = PlaceCapability.getTzId(value);
         if(!StringUtils.isEmpty(tzId)) {
            TimeZone tz = TimeZone.getTimeZone(tzId); 
            logger.info("Updating timezone for place [{}], requested: [{}] result: [{}]", message.getPlaceId(), tzId, tz.getID());
            handler.setTimeZone(tz);
         }
         Double longitude = PlaceCapability.getAddrLongitude(value);
         Double latitude = PlaceCapability.getAddrLatitude(value);
         
         if(longitude != null && latitude != null) {
            handler.setLocation(GeoLocation.fromCoordinates(latitude, longitude));
         }
      }
      commit(nextFireTime);
   }
   
   private MessageBody doHandleRequest(PlatformMessage message, MessageBody request) {
      boolean isStatic = Address.ZERO_UUID.equals(message.getDestination().getId());
      if(isStatic) {
         switch(message.getMessageType()) {
         case SchedulerService.ScheduleCommandsRequest.NAME:
            List<String> commandIds = scheduleCommands(request);
            return
               SchedulerService.ScheduleCommandsResponse
                  .builder()
                  .withSchedulerAddress(scheduler.getAddress().getRepresentation())
                  .withCommandIds(commandIds)
                  .build();
            
         case SchedulerService.ScheduleWeeklyCommandRequest.NAME:
            TimeOfDayCommand command = scheduleWeeklyCommand(request);
            return
               SchedulerService.ScheduleWeeklyCommandResponse
                  .builder()
                  .withCommandId(command.getId())
                  .withSchedulerAddress(scheduler.getAddress().getRepresentation())
                  .build();
            
         case SchedulerService.FireCommandRequest.NAME:
            fireCommand(request);
            return SchedulerService.FireCommandResponse.instance();
            
         case SchedulerService.UpdateWeeklyCommandRequest.NAME:
            updateWeeklyCommand(request);
            return SchedulerService.UpdateWeeklyCommandResponse.instance();
            
         case SchedulerService.DeleteCommandRequest.NAME:
            deleteCommand(request);
            return SchedulerService.DeleteCommandResponse.instance();
         }
         
         return Errors.unsupportedMessageType(message.getMessageType());
      }
      
      NamespacedKey key = NamespacedKey.parse(message.getMessageType());
      if(!key.isInstanced()) {
         switch(key.getNamedRepresentation()) {
         case Capability.CMD_GET_ATTRIBUTES:
            Set<String> names = Capability.GetAttributesRequest.getNames(message.getValue());
            return getAttributes(scheduler, names);
            
         case Capability.CMD_SET_ATTRIBUTES:
            Map<String, Object> attributes = message.getValue().getAttributes();
            return setAttributes(scheduler, attributes);
            
         case Capability.CMD_ADD_TAGS:
            Set<String> tagsToAdd = Capability.AddTagsRequest.getTags(message.getValue());
            removeTags(scheduler, tagsToAdd);
            return Capability.AddTagsResponse.instance();
            
         case Capability.CMD_REMOVE_TAGS:
            Set<String> tagsToRemove = Capability.RemoveTagsRequest.getTags(message.getValue());
            removeTags(scheduler, tagsToRemove);
            return Capability.RemoveTagsResponse.instance();
            
         case SchedulerCapability.DeleteRequest.NAME:
            deleteScheduler();
            return SchedulerCapability.DeleteResponse.instance();
            
         case SchedulerCapability.AddWeeklyScheduleRequest.NAME:
            addWeeklySchedule(request);
            return SchedulerCapability.AddWeeklyScheduleResponse.instance();
            
         case SchedulerCapability.FireCommandRequest.NAME:
            fireCommand(request);
            return SchedulerCapability.FireCommandResponse.instance();

         case SchedulerCapability.RecalculateScheduleRequest.NAME:
            handler.updated();
            return SchedulerCapability.RecalculateScheduleResponse.instance();
         }
         
         return Errors.unsupportedMessageType(message.getMessageType());
      }
      else {
         String scheduleId = key.getInstance();
         switch(key.getNamedRepresentation()) {
         case ScheduleCapability.DeleteRequest.NAME:
            deleteSchedule(scheduleId);
            return ScheduleCapability.DeleteResponse.instance();
         
         case ScheduleCapability.DeleteCommandRequest.NAME:
            deleteCommand(scheduleId, request);
            return ScheduleCapability.DeleteCommandResponse.instance();
         
         case WeeklyScheduleCapability.ScheduleWeeklyCommandRequest.NAME:
            TimeOfDayCommand command = scheduleWeeklyCommand(scheduleId, request);
            return
               WeeklyScheduleCapability.ScheduleWeeklyCommandResponse
                  .builder()
                  .withCommandId(command.getId())
                  .build();
         
         case WeeklyScheduleCapability.UpdateWeeklyCommandRequest.NAME:
            updateWeeklyCommand(scheduleId, request);
            return WeeklyScheduleCapability.UpdateWeeklyCommandResponse.instance();
         
         }
         
         return Errors.unsupportedMessageType(message.getMessageType());
      }
   }
   
   private void addWeeklySchedule(MessageBody request) {
      String scheduleId = SchedulerCapability.AddWeeklyScheduleRequest.getId(request);
      Errors.assertRequiredParam(scheduleId, SchedulerCapability.AddWeeklyScheduleRequest.ATTR_ID);
      String group = SchedulerCapability.AddWeeklyScheduleRequest.getGroup(request);
      
      handler.addWeeklySchedule(scheduleId, group);
   }
   
   private void deleteScheduler() {
      delete = true;
   }
   
   private void deleteSchedule(String scheduleId) {
      handler.deleteSchedule(scheduleId);
   }
   
   private void deleteCommand(MessageBody request) {
      String scheduleId = SchedulerService.DeleteCommandRequest.getSchedule(request);
      String commandId = ScheduleCapability.DeleteCommandRequest.getCommandId(request);
      Errors.assertRequiredParam(scheduleId, SchedulerService.DeleteCommandRequest.ATTR_SCHEDULE);
      Errors.assertRequiredParam(commandId, SchedulerService.DeleteCommandRequest.ATTR_COMMANDID);
      handler.deleteCommand(scheduleId, commandId);
   }
   
   private void deleteCommand(String scheduleId, MessageBody request) {
      String commandId = ScheduleCapability.DeleteCommandRequest.getCommandId(request);
      Errors.assertRequiredParam(commandId, ScheduleCapability.DeleteCommandRequest.ATTR_COMMANDID);
      handler.deleteCommand(scheduleId, commandId);
   }

   private List<String> scheduleCommands(MessageBody request) {
      String group = ScheduleCommandsRequest.getGroup(request);
      if(StringUtils.isEmpty(group)) {
         group = null;
      }
      
      List<Map<String, Object>> requestCommands = ScheduleCommandsRequest.getCommands(request);
      List<TimeOfDayScheduledCommand> translated = new ArrayList<>(requestCommands.size());
      
      for(Map<String, Object> requestCommand: requestCommands) {
         TimeOfDayScheduledCommand command = TimeOfDayScheduledCommand.fromMap(requestCommand);
         command.validate();
         
         translated.add(command);
      }
      
      return handler.scheduleCommands(Optional.fromNullable(group), translated);
   }
   
   private void fireCommand(MessageBody request) {
      String commandId = FireCommandRequest.getCommandId(request);
      Errors.assertRequiredParam(commandId, FireCommandRequest.ATTR_COMMANDID);
      
      MessageBody body = handler.fire(commandId, new Date()).orNull();
      if(body == null) {
         throw new ErrorEventException(Errors.invalidParam(FireCommandRequest.ATTR_COMMANDID));
      }
      
      sendRequest(body);
   }

   private TimeOfDayCommand scheduleWeeklyCommand(MessageBody request) {
      String scheduleId = ScheduleWeeklyCommandRequest.getSchedule(request);
      Errors.assertRequiredParam(scheduleId, ScheduleWeeklyCommandRequest.ATTR_SCHEDULE);

      TimeOfDayScheduledCommand command = TimeOfDayScheduledCommand.fromMap(request.getAttributes());
      command.setScheduleId(scheduleId);
      command.validate();
      
      return handler.scheduleWeeklyCommand(scheduleId, command);
   }
   
   private TimeOfDayCommand scheduleWeeklyCommand(String scheduleId, MessageBody request) {
      TimeOfDayScheduledCommand command = TimeOfDayScheduledCommand.fromMap(request.getAttributes());
      command.setScheduleId(scheduleId);
      command.validate();
      
      return handler.scheduleWeeklyCommand(scheduleId, command);
   }
   
   private void updateWeeklyCommand(MessageBody request) {
      String scheduleId = UpdateWeeklyCommandRequest.getSchedule(request);
      String commandId = UpdateWeeklyCommandRequest.getCommandId(request);
      Errors.assertRequiredParam(scheduleId, UpdateWeeklyCommandRequest.ATTR_SCHEDULE);
      Errors.assertRequiredParam(commandId, UpdateWeeklyCommandRequest.ATTR_COMMANDID);

      TimeOfDayScheduledCommand command = TimeOfDayScheduledCommand.fromMap(request.getAttributes());
      command.setId(commandId);
      command.setScheduleId(scheduleId);
      // don't validate on update
      
      handler.updateWeeklyCommand(scheduleId, command);
   }
   
   private void updateWeeklyCommand(String scheduleId, MessageBody request) {
      String commandId = WeeklyScheduleCapability.UpdateWeeklyCommandRequest.getCommandId(request);
      Errors.assertRequiredParam(commandId, WeeklyScheduleCapability.UpdateWeeklyCommandRequest.ATTR_COMMANDID);
      
      TimeOfDayScheduledCommand command = TimeOfDayScheduledCommand.fromMap(request.getAttributes());
      command.setId(commandId);
      command.setScheduleId(scheduleId);
      // don't validate on an update, we need to merge with the existing command first
      
      handler.updateWeeklyCommand(scheduleId, command);
   }
   
   // TODO retries & timeouts
   private void sendRequest(MessageBody request) {
      Address targetAddress = Address.fromString(SchedulerModel.getTarget(scheduler));
      String placeIdStr = SchedulerModel.getPlaceId(scheduler);
      PlatformMessage message =
            PlatformMessage
               .request(targetAddress)
               .from(scheduler.getAddress())
               .withPlaceId(placeIdStr)
               .withPopulation(populationCacheMgr.getPopulationByPlaceId(placeIdStr))
               .withActor(scheduler.getAddress())
               .withPayload(request)
               .create();
      platformBus.send(message);
      
   }
   
}


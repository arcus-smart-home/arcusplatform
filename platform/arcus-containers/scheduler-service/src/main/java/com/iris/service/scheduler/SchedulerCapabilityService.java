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
package com.iris.service.scheduler;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.platform.AbstractPlatformMessageListener;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.SchedulerCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.errors.NotFoundException;
import com.iris.messages.event.Listener;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.serv.SchedulerModel;
import com.iris.messages.service.SchedulerService.DeleteCommandRequest;
import com.iris.messages.service.SchedulerService.FireCommandRequest;
import com.iris.messages.service.SchedulerService.GetSchedulerRequest;
import com.iris.messages.service.SchedulerService.GetSchedulerResponse;
import com.iris.messages.service.SchedulerService.ListSchedulersRequest;
import com.iris.messages.service.SchedulerService.ListSchedulersResponse;
import com.iris.messages.service.SchedulerService.ScheduleCommandsRequest;
import com.iris.messages.service.SchedulerService.ScheduleWeeklyCommandRequest;
import com.iris.messages.service.SchedulerService.UpdateWeeklyCommandRequest;

/**
 * 
 */
@Singleton
public class SchedulerCapabilityService extends AbstractPlatformMessageListener implements Listener<ScheduledEvent> {
   private static final Logger logger = LoggerFactory.getLogger(SchedulerCapabilityService.class);
   
   private final SchedulerRegistry registry;
   
   /**
    * 
    */
   @Inject
   public SchedulerCapabilityService(
         PlatformMessageBus platformBus,
         SchedulerServiceConfig config,
         SchedulerRegistry registry
   ) {
      super(platformBus, "scheduler-service", config.getThreads(), config.getKeepAliveMs());
      this.registry = registry;
   }

   /* (non-Javadoc)
    * @see com.iris.core.platform.AbstractPlatformMessageListener#onStart()
    */
   @Override
   protected void onStart() {
      logger.info("Adding listeners...");
      addListeners(
            AddressMatchers.equals(Address.broadcastAddress()), // pick up base:Deleted for related schedules
            AddressMatchers.equals(Address.platformService(SchedulerCapability.NAMESPACE)), // pick up Scheduler service requests
            AddressMatchers.platformService(MessageConstants.SERVICE, SchedulerCapability.NAMESPACE) // pick up Scheduler capability requests
      );
   }

   @Override
   public void onEvent(ScheduledEvent event) {
      Optional<SchedulerCapabilityDispatcher> executor = registry.loadByAddress(event.getAddress());
      if(!executor.isPresent()) {
         logger.debug("Dropping scheduled event [{}] -- no executor available for address [{}]", event, event.getAddress());
         return;
      }
      
      executor.get().onScheduledEvent(event);
   }
   
   /* (non-Javadoc)
    * @see com.iris.core.platform.AbstractPlatformMessageListener#handleMessage(com.iris.messages.PlatformMessage)
    */
   @Override
   protected void handleMessage(PlatformMessage message) {
      if(!message.isRequest()) {
         super.handleMessage(message);
         return;
      }
      
      try {
         if(Address.ZERO_UUID.equals(message.getDestination().getId())) {
            handleServiceRequest(message);
         }
         else {
            handleSchedulerRequest(message);
         }
      }
      catch(Exception e) {
         PlatformMessage response = 
               PlatformMessage
                  .respondTo(message)
                  .withPayload(Errors.fromException(e))
                  .create()
                  ;
         sendMessage(response);
      }
   }

   /* (non-Javadoc)
    * @see com.iris.core.platform.AbstractPlatformMessageListener#handleEvent(com.iris.messages.PlatformMessage)
    */
   @Override
   protected void handleEvent(PlatformMessage message) throws Exception {
      if(Capability.EVENT_DELETED.equals(message.getMessageType())) {
         // TODO verify the place header matches? 
         registry.deleteByAddress(message.getSource());
      }
      
      if(
            Capability.EVENT_VALUE_CHANGE.equals(message.getMessageType()) && 
            PlaceCapability.NAMESPACE.equals(message.getSource().getGroup())
      ) {
         Set<String> names = message.getValue().getAttributes().keySet();
         if(
               names.contains(PlaceCapability.ATTR_TZNAME) ||
               names.contains(PlaceCapability.ATTR_TZOFFSET) ||
               names.contains(PlaceCapability.ATTR_TZUSESDST) ||
               names.contains(PlaceCapability.ATTR_ZIPCODE) ||
               names.contains(PlaceCapability.ATTR_ADDRLATITUDE) ||
               names.contains(PlaceCapability.ATTR_ADDRLONGITUDE)
         ) {
            handlePlaceChanged(message);
         }
      }
   }

   protected void handleServiceRequest(PlatformMessage message) {
      Optional<MessageBody> response = Optional.empty();
      switch(message.getMessageType()) {
      case ListSchedulersRequest.NAME:
         response = Optional.of( listSchedulers(message) );
         break;
      case GetSchedulerRequest.NAME:
         response = Optional.of( getScheduler(message) );
         break;
      case ScheduleCommandsRequest.NAME:
         scheduleCommands(message);
         break;
      case ScheduleWeeklyCommandRequest.NAME:
         scheduleWeeklyCommand(message);
         break;
      case UpdateWeeklyCommandRequest.NAME:
         updateWeeklyCommand(message);
         break;
      case FireCommandRequest.NAME:
         fireCommand(message);
         break;
      case DeleteCommandRequest.NAME:
         deleteCommand(message);
         break;
      default:
         throw new ErrorEventException(Errors.unsupportedMessageType(message.getMessageType()));
      }
      
      if(!response.isPresent()) {
         return;
      }
      
      PlatformMessage responseMessage =
            PlatformMessage
               .respondTo(message)
               .withPayload(response.get())
               .create();
      sendMessage(responseMessage);
   }
   
   protected void handleSchedulerRequest(PlatformMessage message) {
      SchedulerCapabilityDispatcher executor =
         registry
            .loadByAddress(message.getDestination())
            .orElseThrow(() -> new NotFoundException(message.getDestination()))
            ;
      
      Errors.assertPlaceMatches(message, SchedulerModel.getPlaceId(executor.getScheduler()));
      executor.onPlatformMessage(message);
   }

   protected void handlePlaceChanged(PlatformMessage message) {
      UUID placeId = (UUID) message.getSource().getId();
      registry
         .loadByPlace(placeId, true)
         .forEach((executor) -> executor.onPlatformMessage(message));
   }

   private MessageBody listSchedulers(PlatformMessage message) {
      String placeHeader = message.getPlaceId();
      String placeId = ListSchedulersRequest.getPlaceId(message.getValue());
      Errors.assertRequiredParam(placeId, ListSchedulersRequest.ATTR_PLACEID);
      
      boolean includeWeekdays = !Boolean.FALSE.equals(ListSchedulersRequest.getIncludeWeekdays(message.getValue())); //true if null 
      
      List<Map<String, Object>> schedulers;
      if(placeHeader != null && !placeHeader.equals(placeId)) {
         logger.warn("Invalid request for objects from another place, source place [{}] requested place [{}]", placeHeader, placeId);
         SchedulerMetrics.invalidPlaceRequest();
         schedulers = ImmutableList.<Map<String, Object>>of();
      }
      else {
         List<SchedulerCapabilityDispatcher> executors = registry.loadByPlace(UUID.fromString(placeId), includeWeekdays);
         schedulers = 
               executors
                  .stream()
                  .map((executor) -> executor.getScheduler().toMap())
                  .collect(Collectors.toList())
                  ;
      }
      
      MessageBody response =
            ListSchedulersResponse
               .builder()
               .withSchedulers(schedulers)
               .build();
      return response;
   }

   private MessageBody getScheduler(PlatformMessage message) {
      String address = GetSchedulerRequest.getTarget(message.getValue());
      Errors.assertRequiredParam(address, GetSchedulerRequest.ATTR_TARGET);
      
      SchedulerCapabilityDispatcher executor = getExecutor(message, address);
      
      MessageBody response =
            GetSchedulerResponse
               .builder()
               .withScheduler(executor.getScheduler().toMap())
               .build();
      return response;
   }
   
   private void scheduleCommands(PlatformMessage message) {
      String address = ScheduleCommandsRequest.getTarget(message.getValue());
      Errors.assertRequiredParam(address, ScheduleCommandsRequest.ATTR_TARGET);
      
      SchedulerCapabilityDispatcher executor = getExecutor(message, address);
      executor.onPlatformMessage(message);
   }

   private void scheduleWeeklyCommand(PlatformMessage message) {
      String address = ScheduleWeeklyCommandRequest.getTarget(message.getValue());
      Errors.assertRequiredParam(address, ScheduleWeeklyCommandRequest.ATTR_TARGET);
      
      SchedulerCapabilityDispatcher executor = getExecutor(message, address);
      executor.onPlatformMessage(message);
   }

   private void updateWeeklyCommand(PlatformMessage message) {
      String address = UpdateWeeklyCommandRequest.getTarget(message.getValue());
      Errors.assertRequiredParam(address, UpdateWeeklyCommandRequest.ATTR_TARGET);
      
      SchedulerCapabilityDispatcher executor = getExecutor(message, address);
      executor.onPlatformMessage(message);
   }
   
   private void fireCommand(PlatformMessage message) {
      String address = FireCommandRequest.getTarget(message.getValue());
      Errors.assertRequiredParam(address, FireCommandRequest.ATTR_TARGET);
      
      SchedulerCapabilityDispatcher executor = getExecutor(message, address);
      executor.onPlatformMessage(message);
   }

   private void deleteCommand(PlatformMessage message) {
      String address = FireCommandRequest.getTarget(message.getValue());
      Errors.assertRequiredParam(address, FireCommandRequest.ATTR_TARGET);
      
      SchedulerCapabilityDispatcher executor = getExecutor(message, address);
      executor.onPlatformMessage(message);
   }
   
   private SchedulerCapabilityDispatcher getExecutor(PlatformMessage message, String address) {
      UUID placeId = UUID.fromString(message.getPlaceId());
      
      // FIXME need a way to verify the thing being targetted is actually associated with the same place
      
      SchedulerCapabilityDispatcher executor = registry.loadOrCreateByAddress(placeId, Address.fromString(address));
      Errors.assertPlaceMatches(message, SchedulerModel.getPlaceId(executor.getScheduler()));
      return executor;
   }
   
}


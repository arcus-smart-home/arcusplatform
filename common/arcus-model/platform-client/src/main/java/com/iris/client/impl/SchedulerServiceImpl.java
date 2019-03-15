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
package com.iris.client.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.iris.capability.util.Addresses;
import com.iris.client.ClientEvent;
import com.iris.client.ClientRequest;
import com.iris.client.IrisClient;
import com.iris.client.IrisClientFactory;
import com.iris.client.capability.Scheduler;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Futures;
import com.iris.client.service.SchedulerService;

/**
 * 
 */
public class SchedulerServiceImpl implements SchedulerService {
   private IrisClient client;

   /**
    * 
    */
   public SchedulerServiceImpl(IrisClient client) {
      this.client = client;
   }

   /* (non-Javadoc)
    * @see com.iris.client.service.Service#getName()
    */
   @Override
   public String getName() {
      return SchedulerService.NAME;
   }

   /* (non-Javadoc)
    * @see com.iris.client.service.Service#getAddress()
    */
   @Override
   public String getAddress() {
      return Addresses.toServiceAddress(SchedulerService.NAMESPACE);
   }

   /* (non-Javadoc)
    * @see com.iris.client.service.SchedulerService#listSchedulers(java.lang.String, java.lang.Boolean)
    */
   @Override
   public ClientFuture<ListSchedulersResponse> listSchedulers(String placeId, Boolean includeWeekdays) {
      ListSchedulersRequest request = new ListSchedulersRequest();
      request.setAddress(getAddress());
      request.setPlaceId(placeId);
      if(Boolean.FALSE.equals(includeWeekdays)) {
    	  request.setIncludeWeekdays(false);
      }else {
    	  request.setIncludeWeekdays(true);
      }
      
      
      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result, new Function<ClientEvent, ListSchedulersResponse>() {
         @Override
         public ListSchedulersResponse apply(ClientEvent input) {
            ListSchedulersResponse response = new ListSchedulersResponse(input);
            IrisClientFactory.getModelCache().retainAll(Scheduler.NAMESPACE, response.getSchedulers());
            return response;
         }
      });
   }

   /* (non-Javadoc)
    * @see com.iris.client.service.SchedulerService#getScheduler(java.lang.String)
    */
   @Override
   public ClientFuture<GetSchedulerResponse> getScheduler(String target) {
      GetSchedulerRequest request = new GetSchedulerRequest();
      request.setAddress(getAddress());
      request.setTarget(target);
      
      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result, new Function<ClientEvent, GetSchedulerResponse>() {
         @Override
         public GetSchedulerResponse apply(ClientEvent response) {
            return new GetSchedulerResponse(response);
         }
      });
   }

   /* (non-Javadoc)
    * @see com.iris.client.service.SchedulerService#fire(java.lang.String, java.lang.String)
    */
   @Override
   public ClientFuture<FireCommandResponse> fireCommand(String target, String commandId) {
      FireCommandRequest request = new FireCommandRequest();
      request.setAddress(getAddress());
      request.setTarget(target);
      request.setCommandId(commandId);
      
      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result, new Function<ClientEvent, FireCommandResponse>() {
         @Override
         public FireCommandResponse apply(ClientEvent response) {
            return new FireCommandResponse(response);
         }
      });
   }

   /* (non-Javadoc)
    * @see com.iris.client.service.SchedulerService#scheduleCommands(java.lang.String, java.util.List)
    */
   @Override
   public ClientFuture<ScheduleCommandsResponse> scheduleCommands(
         String target, String group, List<Map<String, Object>> commands) {
      ScheduleCommandsRequest request = new ScheduleCommandsRequest();
      request.setAddress(getAddress());
      request.setTarget(target);
      request.setGroup(group);
      request.setCommands(commands);
      
      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result, new Function<ClientEvent, ScheduleCommandsResponse>() {
         @Override
         public ScheduleCommandsResponse apply(ClientEvent response) {
            return new ScheduleCommandsResponse(response);
         }
      });
   }

   /* (non-Javadoc)
    * @see com.iris.client.service.SchedulerService#scheduleWeeklyCommand(java.lang.String, java.lang.String, java.util.Set, java.lang.String, java.lang.String, java.lang.Integer, java.lang.String, java.util.Map)
    */
   @Override
   public ClientFuture<ScheduleWeeklyCommandResponse> scheduleWeeklyCommand(
         String target, 
         String schedule, 
         Set<String> days, 
         String mode,
         String time, 
         Integer offsetMinutes, 
         String messageType,
         Map<String, Object> attributes
   ) {
      // can't use ScheduleWeeklyCommandRequest directly because of a clash on setAttributes
      ClientRequest request = new ClientRequest();
      request.setAddress(getAddress());
      request.setCommand(ScheduleWeeklyCommandRequest.NAME);
      request.setAttribute(ScheduleWeeklyCommandRequest.ATTR_TARGET, target);
      request.setAttribute(ScheduleWeeklyCommandRequest.ATTR_SCHEDULE, schedule);
      request.setAttribute(ScheduleWeeklyCommandRequest.ATTR_DAYS, days);
      request.setAttribute(ScheduleWeeklyCommandRequest.ATTR_MODE, mode);
      request.setAttribute(ScheduleWeeklyCommandRequest.ATTR_TIME, time);
      request.setAttribute(ScheduleWeeklyCommandRequest.ATTR_OFFSETMINUTES, offsetMinutes);
      request.setAttribute(ScheduleWeeklyCommandRequest.ATTR_MESSAGETYPE, messageType);
      request.setAttribute(ScheduleWeeklyCommandRequest.ATTR_ATTRIBUTES, attributes);
      
      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result, new Function<ClientEvent, ScheduleWeeklyCommandResponse>() {
         @Override
         public ScheduleWeeklyCommandResponse apply(ClientEvent response) {
            return new ScheduleWeeklyCommandResponse(response);
         }
      });
   }

   /* (non-Javadoc)
    * @see com.iris.client.service.SchedulerService#updateWeeklyCommand(java.lang.String, java.lang.String, java.lang.String, java.util.Set, java.lang.String, java.lang.String, java.lang.Integer, java.lang.String, java.util.Map)
    */
   @Override
   public ClientFuture<UpdateWeeklyCommandResponse> updateWeeklyCommand(
         String target, 
         String schedule, 
         String commandId, 
         Set<String> days,
         String mode, 
         String time, 
         Integer offsetMinutes, 
         String messageType,
         Map<String, Object> attributes
   ) {
      // can't use ScheduleWeeklyCommandRequest directly because of a clash on setAttributes
      ClientRequest request = new ClientRequest();
      request.setAddress(getAddress());
      request.setCommand(UpdateWeeklyCommandRequest.NAME);
      request.setAttribute(UpdateWeeklyCommandRequest.ATTR_TARGET, target);
      request.setAttribute(UpdateWeeklyCommandRequest.ATTR_SCHEDULE, schedule);
      request.setAttribute(UpdateWeeklyCommandRequest.ATTR_COMMANDID, commandId);
      request.setAttribute(UpdateWeeklyCommandRequest.ATTR_DAYS, days);
      request.setAttribute(UpdateWeeklyCommandRequest.ATTR_MODE, mode);
      request.setAttribute(UpdateWeeklyCommandRequest.ATTR_TIME, time);
      request.setAttribute(UpdateWeeklyCommandRequest.ATTR_OFFSETMINUTES, offsetMinutes);
      request.setAttribute(UpdateWeeklyCommandRequest.ATTR_MESSAGETYPE, messageType);
      request.setAttribute(UpdateWeeklyCommandRequest.ATTR_ATTRIBUTES, attributes);
      
      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result, new Function<ClientEvent, UpdateWeeklyCommandResponse>() {
         @Override
         public UpdateWeeklyCommandResponse apply(ClientEvent response) {
            return new UpdateWeeklyCommandResponse(response);
         }
      });
   }

   /* (non-Javadoc)
    * @see com.iris.client.service.SchedulerService#deleteCommand(java.lang.String, java.lang.String, java.lang.String)
    */
   @Override
   public ClientFuture<DeleteCommandResponse> deleteCommand(
         String target, String schedule, String commandId) {
      DeleteCommandRequest request = new DeleteCommandRequest();
      request.setAddress(getAddress());
      request.setTarget(target);
      request.setSchedule(schedule);
      request.setCommandId(commandId);
      
      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result, new Function<ClientEvent, DeleteCommandResponse>() {
         @Override
         public DeleteCommandResponse apply(ClientEvent response) {
            return new DeleteCommandResponse(response);
         }
      });
   }

}


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
package com.iris.oculus.modules.scheduler;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableMap;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.capability.definition.MethodDefinition;
import com.iris.capability.util.Addresses;
import com.iris.client.IrisClientFactory;
import com.iris.client.event.ClientFuture;
import com.iris.client.model.SchedulerModel;
import com.iris.client.service.SchedulerService;
import com.iris.client.service.SchedulerService.ListSchedulersResponse;
import com.iris.client.service.SchedulerService.ScheduleWeeklyCommandRequest;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.BaseController;
import com.iris.oculus.modules.capability.ux.CapabilityInputPrompt;

/**
 * 
 */
@Singleton
public class SchedulerController extends BaseController<SchedulerModel> {

   private MethodDefinition updateSchedule;
   
   @Inject
   public SchedulerController(DefinitionRegistry registry) {
      super(SchedulerModel.class);
      this.updateSchedule = 
            registry
               .getService(SchedulerService.NAMESPACE)
               .getMethods()
               .stream()
               .filter((method) -> SchedulerService.ScheduleWeeklyCommandRequest.NAME.endsWith(method.getName()))
               .findFirst()
               .get();
   }
   
   @Override
   protected ClientFuture<? extends Collection<Map<String, Object>>> doLoad() {
      return
         IrisClientFactory
            .getService(SchedulerService.class)
            .listSchedulers(getPlaceId(), true)
            .transform((response) -> ((ListSchedulersResponse) response).getSchedulers())
            ;
   }

   public void addScheduledEvent(String targetAddress) {
      CapabilityInputPrompt
         .prompt(updateSchedule, ImmutableMap.<String, Object>of(ScheduleWeeklyCommandRequest.ATTR_TARGET, targetAddress))
         .onSuccess((attributes) -> scheduleEvent(attributes))
         ;
      
   }
   
   protected void scheduleEvent(Map<String, Object> attributes) {
      ScheduleWeeklyCommandRequest request = new ScheduleWeeklyCommandRequest();
      request.setAddress(Addresses.toServiceAddress(SchedulerService.NAMESPACE));
      request.setAttributes(attributes);
      IrisClientFactory
         .getClient()
         .request(request)
         .onFailure((error) -> Oculus.showError("Error Scheduling Event", error))
         ;
   }

}


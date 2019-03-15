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
package com.iris.platform.subsystem.incident;

import java.util.concurrent.ExecutorService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.messaging.MessageListener;
import com.iris.core.platform.AbstractPlatformService;
import com.iris.core.platform.PlatformDispatcherFactory;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatcher;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.platform.subsystem.resolver.SubsystemExecutorResolver;

@Singleton
public class AlarmIncidentServiceImpl extends AbstractPlatformService {
   public static final String NAME_EXECUTOR_POOL = "threadpool.incident";
   public static final String PROP_REQUEST_HANDLERS = "service.alarm.incident.handlers";
   public static final Address SERVICE_ADDRESS = Address.platformService(AlarmIncidentCapability.NAMESPACE);
   public static final AddressMatcher OBJECT_ADDRESS = AddressMatchers.fromString("SERV:" + AlarmIncidentCapability.NAMESPACE + ":*");
   
   private MessageListener<PlatformMessage> dispatcher;
   
   @Inject
   public AlarmIncidentServiceImpl(
         PlatformMessageBus platformBus,
         @Named(NAME_EXECUTOR_POOL) ExecutorService executor,
         SubsystemExecutorResolver subsystemExecutorResolver,
         PlatformDispatcherFactory dispatcherFactory,
         GetAttributesHandler getAttributesHandler,
         ListIncidentHistoryEntriesHandler listIncidentHistory,
         MockAlarmIncidentService mockAlarmIncidentService,
         AlarmIncidentRequestHandler incidentRequestHandler,
         AlarmIncidentServiceDispatcher incidentService
   ) {
      super(platformBus, SERVICE_ADDRESS, executor);
            
      this.dispatcher = 
            dispatcherFactory
               .buildDispatcher()
               .addArgumentResolverFactory(subsystemExecutorResolver)
               .addAnnotatedHandler(getAttributesHandler)
               .addAnnotatedHandler(incidentService)
               .addAnnotatedHandler(listIncidentHistory)
               .addAnnotatedHandler(mockAlarmIncidentService)
               .addAnnotatedHandler(incidentRequestHandler)
               .build();
   }

   @Override
   protected void onStart() {
      super.onStart();
      addListeners(OBJECT_ADDRESS);
   }

   @Override
   protected void doHandleMessage(PlatformMessage message) {
      dispatcher.onMessage(message);
   }
   

}


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
package com.iris.platform.subsystem;


import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.common.subsystem.SubsystemExecutor;
import com.iris.common.subsystem.event.SubsystemResponseEvent;
import com.iris.core.messaging.MessageListener;
import com.iris.core.platform.AbstractPlatformService;
import com.iris.core.platform.PlatformDispatcherFactory;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatcher;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.event.AddressableEvent;
import com.iris.messages.event.Listener;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.service.SubsystemService;
import com.iris.platform.subsystem.handler.ListActivityIntervalHandler;
import com.iris.platform.subsystem.handler.ListDetailedActivityHandler;
import com.iris.platform.subsystem.handler.ListHistoryEntriesHandler;
import com.iris.platform.subsystem.handler.ListSubsystemsRequestHandler;
import com.iris.platform.subsystem.handler.ReloadRequestHandler;
import com.iris.platform.subsystem.handler.SubsystemRequestHandler;
import com.iris.platform.subsystem.resolver.SubsystemExecutorResolver;

@Singleton
public class SubsystemServiceImpl 
	extends AbstractPlatformService 
   implements Listener<AddressableEvent> 
{
   public static final Address SERVICE_ADDRESS = Address.platformService(SubsystemService.NAMESPACE);
   public static final String NAME_EXECUTOR = "SubsystemServiceImpl#executor";
   
   private static final Logger logger = LoggerFactory.getLogger(SubsystemServiceImpl.class);

   private final SubsystemCatalog catalog;
   private final SubsystemRegistry registry;
   private final MessageListener<PlatformMessage> serviceDispatcher;
   private final MessageListener<PlatformMessage> objectDispatcher;
   
   @Inject
   public SubsystemServiceImpl(
         PlatformMessageBus platformBus,
         @Named(NAME_EXECUTOR) ExecutorService executor, 
         SubsystemCatalog catalog,
         SubsystemRegistry registry,
         PlatformDispatcherFactory dispatcherFactory,
         SubsystemExecutorResolver executorResolver,
         ListSubsystemsRequestHandler listSubsystems,
         ListActivityIntervalHandler listActivity,
         ListDetailedActivityHandler listDetailedActivity,
         ListHistoryEntriesHandler listHistory,
         ReloadRequestHandler reload,
         SubsystemRequestHandler subsystemRequestHandler
   ) {
      super(platformBus, SERVICE_ADDRESS, executor);

      this.catalog = catalog;
      this.registry = registry;
      this.serviceDispatcher =
      		dispatcherFactory
      			.buildDispatcher()
      			.addArgumentResolverFactory(executorResolver)
      			.addAnnotatedHandler(listSubsystems)
      			.addAnnotatedHandler(reload)
      			.addUnsupportedFallbackRequestHandler()
      			.build();
      			
      this.objectDispatcher = 
      		dispatcherFactory
      			.buildDispatcher()
      			.addArgumentResolverFactory(executorResolver)
      			.addAnnotatedHandler(listActivity)
      			.addAnnotatedHandler(listDetailedActivity)
      			.addAnnotatedHandler(listHistory)
      			.addAnnotatedHandler(subsystemRequestHandler)
      			.build();                        
   }

   @Override
   protected void doHandleMessage(PlatformMessage message) {
      objectDispatcher.onMessage(message);
   }
   
   @Override
   protected void onStart() {
      super.onStart();
      logger.info("Adding listener for SERV:subs:");
      getMessageBus().addMessageListener(AddressMatchers.anyOf(SERVICE_ADDRESS), (message) -> executor().execute(() -> serviceDispatcher.onMessage(message)));
      
      Set<AddressMatcher> matchers = new HashSet<>();
      matchers.add(AddressMatchers.equals(Address.broadcastAddress()));
      catalog
         .getSubsystems()
         .forEach((subsystem) -> matchers.add(AddressMatchers.platformService(MessageConstants.SERVICE, subsystem.getNamespace())));

      addListeners(matchers);
   }

   @Override
   protected void onStop() {
   	// TODO just move this into the registry itself
      logger.info("Shutting down subsystems...");
      registry.clear();
   }

   @Override
   public void onEvent(AddressableEvent event) {
      if(event instanceof ScheduledEvent) {
         executor().execute(() -> handleScheduledEvent((ScheduledEvent) event));
      }
      else if(event instanceof SubsystemResponseEvent) {
         executor().execute(() -> handleResponseEvent((SubsystemResponseEvent) event));
      }
      throw new IllegalArgumentException("Only scheduled & response events may be invoked this way currently");
   }
   
   protected void handleScheduledEvent(ScheduledEvent event) {
      UUID placeId = (UUID) event.getAddress().getId();
      Optional<SubsystemExecutor> executor = registry.loadByPlace(placeId);
      if(executor.isPresent()) {
         executor.get().onScheduledEvent(event);
      }
      else {
         logger.debug("Dropping scheduled event [{}] -- no executor available for place [{}]", event, placeId);
      }
   }

   protected void handleResponseEvent(SubsystemResponseEvent event) {
      UUID placeId = (UUID) event.getAddress().getId();
      Optional<SubsystemExecutor> executor = registry.loadByPlace(placeId);
      if(executor.isPresent()) {
         executor.get().onSubystemResponse(event);
      }
      else {
         logger.debug("Dropping response event [{}] -- no executor available for place [{}]", event, placeId);
      }
   }

}


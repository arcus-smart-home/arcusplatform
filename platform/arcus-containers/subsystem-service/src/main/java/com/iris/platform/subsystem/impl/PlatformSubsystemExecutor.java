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
package com.iris.platform.subsystem.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.log4j.MDC;

import com.iris.common.subsystem.Subsystem;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemContext.ResponseAction;
import com.iris.common.subsystem.SubsystemExecutor;
import com.iris.common.subsystem.event.SubsystemAddedEvent;
import com.iris.common.subsystem.event.SubsystemLifecycleEvent;
import com.iris.common.subsystem.event.SubsystemRemovedEvent;
import com.iris.common.subsystem.event.SubsystemResponseEvent;
import com.iris.common.subsystem.util.PlaceMdcBinder;
import com.iris.core.messaging.SingleThreadDispatcher;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.Message;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.PlatformServiceAddress;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.SafetySubsystemCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.context.PlaceContext;
import com.iris.messages.errors.Errors;
import com.iris.messages.errors.InvalidRequestException;
import com.iris.messages.errors.NotFoundException;
import com.iris.messages.event.AddressableEvent;
import com.iris.messages.event.MessageReceivedEvent;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.SubsystemModel;
import com.iris.messages.service.SubsystemService.ListSubsystemsRequest;
import com.iris.messages.service.SubsystemService.ListSubsystemsResponse;
import com.iris.platform.subsystem.SubsystemFactory;
import com.iris.util.IrisCorrelator;
import com.iris.util.MdcContext;
import com.iris.util.MdcContext.MdcContextReference;
import com.iris.util.Result;

/**
 * This represents an event loop for all the subsystems associated with a place.
 */
public class PlatformSubsystemExecutor implements Consumer<AddressableEvent>, SubsystemExecutor {
   private static final PlatformServiceAddress ADDR_SUBSYSTEM_SERVICE = 
         Address.platformService(SubsystemCapability.NAMESPACE);
   
   private final Address placeAddress;
   private final PlatformMessageBus platformBus;
   private final IrisCorrelator<ResponseAction<?>> correlator;
   private final SubsystemFactory factory;
   private final PlaceContext rootContext;
   private final SingleThreadDispatcher<AddressableEvent> dispatcher;
   private final ConcurrentMap<Address, SubsystemAndContext<?>> subsystems;
   
   /**
    * 
    */
   public PlatformSubsystemExecutor(
         PlatformMessageBus platformBus,
         IrisCorrelator<ResponseAction<?>> correlator,
         SubsystemFactory factory,
         PlaceContext context,
         Collection<Subsystem<?>> subsystems,
         int maxQueueDepth
   ) {
      this.platformBus = platformBus;
      this.correlator = correlator;
      this.factory = factory;
      this.rootContext = context;
      this.dispatcher = new SingleThreadDispatcher<>(this, maxQueueDepth);
      // assume subsystems aren't added / removed very often
      this.subsystems = new ConcurrentHashMap<>(subsystems.size() + 1, 0.75f, 1);
      this.placeAddress = Address.platformService(context.getPlaceId(), PlaceCapability.NAMESPACE);
      for(Subsystem<?> subsystem: subsystems) {
         this.subsystems.put(
               Address.platformService(context.getPlaceId(), subsystem.getNamespace()),
               loadSubsystemContext(subsystem)
         );
      }
      
      // these change events skip the queue, so that the store is still in sync with when
      // the event was received
      context.models().addListener((event) -> accept(event));
   }

   /**
    * @return
    * @see com.iris.core.messaging.SingleThreadDispatcher#isRunning()
    */
   public boolean isRunning() {
      return dispatcher.isRunning();
   }

   /**
    * @return
    * @see com.iris.core.messaging.SingleThreadDispatcher#getDispatchThreadName()
    */
   public String getDispatchThreadName() {
      return dispatcher.getDispatchThreadName();
   }

   /* (non-Javadoc)
    * @see com.iris.common.subsystem.SubsystemExecutor#context()
    */
   @Override
   public PlaceContext context() {
      return rootContext;
   }

   /* (non-Javadoc)
    * @see com.iris.common.subsystem.SubsystemExecutor#start()
    */
   @Override
   public void start() {
      context().logger().debug("Starting subsystem executor");
      dispatchToAllSubsystems(SubsystemLifecycleEvent::started);
   }

   /* (non-Javadoc)
    * @see com.iris.common.subsystem.SubsystemExecutor#stop()
    */
   @Override
   public void stop() {
      context().logger().debug("Stopping subsystem executor");
      dispatchToAllSubsystems(SubsystemLifecycleEvent::stopped);
   }

   /* (non-Javadoc)
    * @see com.iris.common.subsystem.SubsystemExecutor#delete()
    */
   @Override
   public void delete() {
      context().logger().info("Place has been deleted, removing all subsystems");
      dispatchToAllSubsystems(SubsystemLifecycleEvent::removed);
   }

   /* (non-Javadoc)
    * @see com.iris.common.subsystem.SubsystemExecutor#onPlatformMessage(com.iris.messages.PlatformMessage)
    */
   @Override
   public void onPlatformMessage(PlatformMessage message) {
      try(MdcContextReference context = Message.captureAndInitializeContext(message)) {
         bindPlaceToMdc();
         this.dispatcher.dispatchOrQueue(new MessageReceivedEvent(message));
      }
   }


   private void bindPlaceToMdc() {
      PlaceContext context = context();
      try {
         Model place = context.models().getModelByAddress(placeAddress);
         PlaceMdcBinder placeMdcBinder = new PlaceMdcBinder(place);
         placeMdcBinder.bind();
      }
      catch (Exception e) {
         context.logger().warn("Exception trying to bind place to MDC.", e);
      }
   }

   /* (non-Javadoc)
    * @see com.iris.common.subsystem.SubsystemExecutor#onScheduledEvent(com.iris.messages.event.ScheduledEvent)
    */
   @Override
   public void onScheduledEvent(ScheduledEvent event) {
      try(MdcContextReference context = MdcContext.captureMdcContext()) {
         this.dispatcher.dispatchOrQueue(event);
      }
   }
   

   @Override
   public void onSubystemResponse(SubsystemResponseEvent event) {
      try(MdcContextReference context = MdcContext.captureMdcContext()) {
         this.dispatcher.dispatchOrQueue(event);
      }
   }

   /* (non-Javadoc)
    * @see com.iris.common.subsystem.SubsystemExecutor#add(com.iris.common.subsystem.Subsystem)
    */
   @Override
   public void add(Subsystem<?> subsystem) {
      // TODO Auto-generated method stub

   }

   /* (non-Javadoc)
    * @see com.iris.common.subsystem.SubsystemExecutor#get(com.iris.messages.address.Address)
    */
   @Override
   public Subsystem<?> get(Address address) {
      SubsystemAndContext<?> entry = subsystems.get(address);
      return entry != null ? entry.subsystem : null;
   }
   
   /* (non-Javadoc)
    * @see com.iris.common.subsystem.SubsystemExecutor#getContext(com.iris.messages.address.Address)
    */
   @Override
   public SubsystemContext<?> getContext(Address address) {
      SubsystemAndContext<?> entry = subsystems.get(address);
      return entry != null ? entry.context : null;
   }

   /* (non-Javadoc)
    * @see com.iris.common.subsystem.SubsystemExecutor#activate(com.iris.messages.address.Address)
    */
   @Override
   public void activate(Address address) {
      // TODO Auto-generated method stub

   }

   /* (non-Javadoc)
    * @see com.iris.common.subsystem.SubsystemExecutor#deactivate(com.iris.messages.address.Address)
    */
   @Override
   public void deactivate(Address address) {
      // TODO Auto-generated method stub

   }

   /* (non-Javadoc)
    * @see com.iris.common.subsystem.SubsystemExecutor#delete(com.iris.messages.address.Address)
    */
   @Override
   public void delete(Address address) {
      // TODO Auto-generated method stub

   }

   /* (non-Javadoc)
    * @see java.util.function.Consumer#accept(java.lang.Object)
    */
   @Override
   public void accept(AddressableEvent event) {
      try(MdcContextReference context = MdcContext.captureMdcContext()) {
         bindPlaceToMdc();
         if(event instanceof MessageReceivedEvent) {
            MessageReceivedEvent mrevent = (MessageReceivedEvent)event;
            PlatformMessage message = mrevent.getMessage();
            if(message.getDestination().isBroadcast()) {
               if(message.getSource().equals(placeAddress) && Capability.EVENT_DELETED.equals(message.getMessageType())) {
                  dispatchToAllSubsystems(SubsystemLifecycleEvent::removed);
               }
               else {
                  dispatchToAllSubsystems(mrevent);
                  context().models().update(message);
                  commitAllSubsystems();
               }
            }
            else if(ADDR_SUBSYSTEM_SERVICE.equals(message.getDestination())) {
               dispatchServiceMessage(mrevent);
            }
            else {
               dispatchToSubsystem(mrevent);
            }
         }
         else if(event instanceof ScheduledEvent || event instanceof SubsystemResponseEvent) {
            SubsystemAndContext<?> subsystem = subsystems.get(event.getAddress());
            if(subsystem == null) {
               context().logger().warn("Dropping event [{}] -- Unable to load subsystem", event);
            }
            else {
               subsystem.dispatch(event);
            }
         }
         else {
            dispatchToAllSubsystems(event);
         }
      }
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "PlatformSubsystemExecutor [place=" + rootContext.getPlaceId() + 
            ", thread=" + dispatcher.getDispatchThreadName() + 
            ", queueDepth=" + dispatcher.getQueuedMessageCount() + 
            "]";
   }
   
   protected void commitAllSubsystems(){
      for(SubsystemAndContext<?>subsystem:subsystems.values()){
         try{
            subsystem.context.commit();
         }
         catch(Exception e) {
            subsystem.context.logger().warn("Error commiting context model");
         }
      }
   }
   
   protected MessageBody handleServiceRequest(PlatformMessage message) {
      if(ListSubsystemsRequest.NAME.equals(message.getMessageType())) {
         MessageBody value = message.getValue();
         String placeId = ListSubsystemsRequest.getPlaceId(value);
         Errors.assertRequiredParam(placeId, "placeId");
         Errors.assertPlaceMatches(message, placeId);
         
         List<Map<String, Object>> subsystems = listSubsystems();
         return
               ListSubsystemsResponse
                  .builder()
                  .withSubsystems(subsystems)
                  .build();
      }
      throw new InvalidRequestException(message.getMessageType());
   }
   
   protected List<Map<String, Object>> listSubsystems() {
      return subsystems
               .values()
               .stream()
               .map(SubsystemAndContext::toMap)
               .collect(Collectors.toList())
               ;
               
   }

   private void dispatchToAllSubsystems(MessageReceivedEvent event) {
      subsystems.values().forEach((sac) -> sac.dispatch(event,correlator));
   }

   private void dispatchToAllSubsystems(AddressableEvent event) {
      subsystems.values().forEach((sac) -> sac.dispatch(event));
   }

   private void dispatchToAllSubsystems(Function<Address, AddressableEvent> producer) {
      subsystems.values().forEach((sac) -> sac.dispatch(producer.apply(sac.address)));
   }

   private void dispatchServiceMessage(MessageReceivedEvent event) {
      PlatformMessage message = event.getMessage();
      if(!message.isRequest()) {
         // ignore
         return;
      }
      
      platformBus.invokeAndSendResponse(message, () -> handleServiceRequest(message));
   }

   private void dispatchToSubsystem(MessageReceivedEvent event) {
      Message.captureAndInitializeContext(event.getMessage());
      Address destination = lookupDestination(event.getMessage()); 
      platformBus.invoke(event.getMessage(), () -> {
         Optional
            .ofNullable(subsystems.get(destination))
            .orElseThrow(() -> new NotFoundException(destination))
            .dispatch(event, correlator);
         return MessageBody.noResponse();
      });
   }

   private Address lookupDestination(PlatformMessage message) {
   	// special case for AlarmIncident
      if(AlarmIncidentCapability.NAMESPACE.equals(message.getDestination().getGroup()) && !message.getMessageType().startsWith("base:")) {
      	return Address.platformService(UUID.fromString(message.getPlaceId()), AlarmSubsystemCapability.NAMESPACE);
      }
      // special case for security / safety subsystem messages
   	String messageType = message.getMessageType();
   	if(
   			messageType.startsWith(SafetySubsystemCapability.NAMESPACE) ||
   			messageType.startsWith(SecuritySubsystemCapability.NAMESPACE)
		) {
   		Address alarmSubsystem = Address.platformService(message.getDestination().getId(), AlarmSubsystemCapability.NAMESPACE);
   		SubsystemAndContext<? extends SubsystemModel> sac = subsystems.get(alarmSubsystem);
   		if(sac != null && sac.context.model().isStateACTIVE()) {
   			sac.context.logger().debug("Redirecting [{}] request to alarm subsystem", messageType);
   			return alarmSubsystem;
   		}
   	}
   	return message.getDestination();
	}

	private <M extends SubsystemModel> SubsystemAndContext<M> loadSubsystemContext(Subsystem<M> subsystem) {
      SubsystemContext<M> context = factory.createContext(subsystem, (e) -> dispatcher.dispatchOrQueue(e), rootContext);
      return new SubsystemAndContext<>(subsystem, context);
   }

   private static final class SubsystemAndContext<M extends SubsystemModel> {
      private final Address address;
      private final Subsystem<M> subsystem;
      private final SubsystemContext<M> context;
      
      SubsystemAndContext(Subsystem<M> subsystem, SubsystemContext<M> context) {
         this.address = Address.platformService(context.getPlaceId(), subsystem.getNamespace());
         this.subsystem = subsystem;
         this.context = context;
      }
      
      public void dispatch(MessageReceivedEvent event, IrisCorrelator<ResponseAction<?>> correlator) {
         MDC.put(MdcContext.MDC_TARGET, address.getRepresentation());
         context.setActor(event.getMessage().getActor());
         
         if(!context.isPersisted()) {
            dispatchEvent(SubsystemLifecycleEvent.added(address));
         }

         if(context.isDeleted()) {
            context.logger().warn("Dropping event to deleted subsystem [{}]", event);
         }
         else {
            Result<ResponseAction<?>> result = correlator.correlate(event.getMessage());
            if(result != null) {
               if(result.isError()) {
                  context.logger().warn("Error correlating response [{}}", event.getMessage(), result.getError());
               }
               else {
                  dispatchEvent(SubsystemResponseEvent.response(address, result.getValue(), event.getMessage()));
               }
            }
            dispatchEvent(event);
            save(event);
         }
      }

      public void dispatch(AddressableEvent event) {
         MDC.put(MdcContext.MDC_TARGET, address.getRepresentation());

         if(!context.isPersisted() && !(event instanceof SubsystemAddedEvent)) {
            dispatchEvent(SubsystemLifecycleEvent.added(address));
         }
         if(context.isDeleted()) {
         	context.logger().warn("Dropping event to deleted subsystem [{}]", event);
         }
         else {
	         dispatchEvent(event);
	         save(event);
         }
      }
      
      protected void dispatchEvent(AddressableEvent event) {
         try {
            this.subsystem.onEvent(event, this.context);
         }
         catch(Exception e) {
            this.context.logger().warn("Error handling event {}", event, e);
         }
      }
      
      protected void save(AddressableEvent event) {
         try {
            if(event instanceof SubsystemRemovedEvent) {
               this.context.delete();
            }
            else {
               this.context.commit();
            }
         }
         catch(Exception e) {
            this.context.logger().warn("Error saving context", e);
         }
      }
      
      public Map<String, Object> toMap() {
         return context.model().toMap();
      }
   }
}


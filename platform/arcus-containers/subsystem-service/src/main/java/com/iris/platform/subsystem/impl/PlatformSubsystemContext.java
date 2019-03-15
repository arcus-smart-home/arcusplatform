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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.iris.common.scheduler.ScheduledTask;
import com.iris.common.scheduler.Scheduler;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.event.SubsystemResponseEvent;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.io.json.JSON;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.context.SimplePlaceContext;
import com.iris.messages.event.AddressableEvent;
import com.iris.messages.event.FireEventTask;
import com.iris.messages.event.Listener;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.ModelStore;
import com.iris.messages.model.subs.SubsystemModel;
import com.iris.platform.model.ModelEntity;
import com.iris.platform.subsystem.SubsystemDao;
import com.iris.type.LooselyTypedReference;
import com.iris.util.IrisCorrelator;
import com.iris.util.IrisCorrelator.Action;
import com.iris.util.IrisUUID;
import com.iris.util.Subscription;
import com.iris.util.TypeMarker;

/**
 * 
 */
// TODO push this down to arcus-subsystem?
public class PlatformSubsystemContext<M extends SubsystemModel>
   extends SimplePlaceContext
   implements SubsystemContext<M> {

   private static final String VAR_NAMESPACE = "_subvars:";
   
   public static PlatformSubsystemContext.Builder builder() {
      return new Builder();
   }

   private final ModelEntity entity;

   private final M model;
   private Address actor;
   private final SubsystemDao subsystemDao;
   private final PlatformMessageBus platformBus;
   private final IrisCorrelator<ResponseAction<?>> correlator;
   // TODO bind the scheduler and the listener together?
   private final Scheduler scheduler;
   // allows access to the event loop without holding onto a reference to "this"
   // FIXME should replace with a proper EventLoop with invoke, defer, schedule
   private final Listener<? super AddressableEvent> eventListener;
   private boolean deleted = false;
   private boolean added;
   private volatile TimeZone tz;
   private final List<Subscription> bindSubscriptions = new ArrayList<>();

   // See the commit() method for a description of what this is used for.
   private @Nullable Map<String,Object> failedToSave;
   
   private PlatformSubsystemContext(
         UUID placeId,
         String population,
         UUID accountId,
         Logger logger,
         ModelStore models,
         M model,
         ModelEntity entity,
         SubsystemDao subsystemDao,
         PlatformMessageBus platformBus,
         IrisCorrelator<ResponseAction<?>> correlator,
         Scheduler scheduler,
         Listener<? super AddressableEvent> eventListener,
         TimeZone tz
   ) {
      super(placeId, population, accountId, logger, models);
      this.entity = entity;
      this.model = model;
      this.subsystemDao = subsystemDao;
      this.platformBus = platformBus;
      this.correlator = correlator;
      this.scheduler = scheduler;
      this.eventListener = eventListener;
      this.tz=tz;
      this.added = entity.isPersisted();
   }

   /* (non-Javadoc)
    * @see com.iris.common.subsystem.SubsystemContext#model()
    */
   @Override
   public M model() {
      return model;
   }

   @Override
   public Calendar getLocalTime() {
      if(tz==null){
         tz=super.getLocalTime().getTimeZone();
      }
      return Calendar.getInstance(tz);
   }
   
   public void setTimeZone(TimeZone tz){
      this.tz=tz;
   }
   
   /* (non-Javadoc)
    * @see com.iris.common.subsystem.SubsystemContext#getVariable(java.lang.String)
    */
   @Override
   public LooselyTypedReference getVariable(String name) {
      Preconditions.checkArgument(StringUtils.isNotEmpty(name), "variable name may not be empty");
      String json = model.getAttribute(TypeMarker.string(), VAR_NAMESPACE + name, "null");
      return JSON.fromJson(json, LooselyTypedReference.class);
   }
   
   /* (non-Javadoc)
    * @see com.iris.common.subsystem.SubsystemContext#getAttribute(java.lang.String)
    */
   @Override
   public Object getAttribute(String name) {
   	return entity.getAttribute(name);
   }

	/* (non-Javadoc)
    * @see com.iris.common.subsystem.SubsystemContext#setVariable(java.lang.String, java.lang.Object)
    */
   @Override
   public void setVariable(String name, Object value) {
      Preconditions.checkArgument(StringUtils.isNotEmpty(name), "variable name may not be empty");
      if(value == null) {
         model.setAttribute(VAR_NAMESPACE + name, null);
      }
      else {
         model.setAttribute(VAR_NAMESPACE + name, JSON.toJson(value));
      }
   }

   /* (non-Javadoc)
    * @see com.iris.common.subsystem.SubsystemContext#broadcast(com.iris.messages.MessageBody)
    */
   @Override
   public void broadcast(MessageBody payload) {
      PlatformMessage message =
            buildMessage()
               .broadcast()
               .withPayload(payload)
               .create()
               ;
      
      platformBus.send(message);
   }

   /* (non-Javadoc)
    * @see com.iris.common.subsystem.SubsystemContext#send(com.iris.messages.address.Address, com.iris.messages.MessageBody)
    */
   @Override
   public void send(Address address, MessageBody payload) {
      // NOTE this differs from request in that it does NOT mark the message as a request
      //      however since it doesn't expose correlation id it can't be used for responses either...
      //      and broadcast should be preferred for broadcast messages...
      PlatformMessage message =
            buildMessage()
               .to(address)
               .withPayload(payload)
               .withCorrelationId(IrisUUID.toString(IrisUUID.randomUUID()))
               .create()
               ;
      
      platformBus.send(message);
   }

   @Override
   public void sendAndExpectResponse(Address address, MessageBody payload, long timeout, TimeUnit unit, ResponseAction<? super M> action) {
      PlatformMessage message =
            buildRequest(address)
               .withPayload(payload)
               .withCorrelationId(IrisUUID.toString(IrisUUID.randomUUID()))
               .withTimeToLive((int)unit.toMillis(timeout))
               .create()
               ;

      // don't use an anonymous class here to make sure we're not accidentally capturing additional state
      correlator.track(message, timeout, unit, new CorrelatorAction(eventListener, address, action));
      platformBus.send(message);
   }

   /* (non-Javadoc)
    * @see com.iris.common.subsystem.SubsystemContext#sendResponse(com.iris.messages.PlatformMessage, com.iris.messages.MessageBody)
    */
   @Override
   public void sendResponse(PlatformMessage request, MessageBody payload) {
      if (MessageBody.noResponse().equals(payload)) {
         return;
      }
      PlatformMessage message =
            PlatformMessage
               .respondTo(request)
               .withPlaceId(getPlaceId())
               .withPopulation(getPopulation())
               .withPayload(payload)
               .create()
               ;
      
      platformBus.send(message);
   }

   /* (non-Javadoc)
    * @see com.iris.common.subsystem.SubsystemContext#request(com.iris.messages.address.Address, com.iris.messages.MessageBody)
    */
   @Override
   public String request(Address address, MessageBody payload) {
      return request(address, payload, -1);
   }

   @Override
   public String request(Address address, MessageBody payload, int timeToLiveMs) {
      String correlationId = IrisUUID.timeUUID().toString();
      PlatformMessage message =
            buildRequest(address)
               .withCorrelationId(correlationId)
               .withTimeToLive(timeToLiveMs)
               .withPayload(payload)
               .create()
               ;
      
      platformBus.send(message);
      return correlationId;
   }

   /* (non-Javadoc)
    * @see com.iris.common.subsystem.SubsystemContext#wakeUpIn(long, java.util.concurrent.TimeUnit)
    */
   @Override
   public ScheduledTask wakeUpIn(long time, TimeUnit unit) {
      ScheduledEvent event = new ScheduledEvent(model().getAddress(), System.currentTimeMillis() + unit.toMillis(time));
      return scheduler.scheduleDelayed(FireEventTask.create(event, eventListener), time, unit);
   }

   /* (non-Javadoc)
    * @see com.iris.common.subsystem.SubsystemContext#wakeUpAt(java.util.Date)
    */
   @Override
   public ScheduledTask wakeUpAt(Date timestamp) {
      ScheduledEvent event = new ScheduledEvent(model().getAddress(), timestamp.getTime());
      return scheduler.scheduleAt(FireEventTask.create(event, eventListener), timestamp);
   }

   /* (non-Javadoc)
    * @see com.iris.common.subsystem.SubsystemContext#isPersisted()
    */
   @Override
   public boolean isPersisted() {
      return entity.isPersisted();
   }
   
   @Override
   public boolean isDeleted() {
      return this.deleted;
   }

   /* (non-Javadoc)
    * @see com.iris.common.subsystem.SubsystemContext#commit()
    */
   @Override
   public void commit() {
      // The consistency guarantees provided by this method are tightly coupled
      // with the flow between added/changed and save. We want to ensure that
      // we only send one ADDED / VALUE_CHANGED (as much as we can) and to
      // that end we use the following logic:
      //    * Ignore if deleted
      //    * If the entity is not persisted and we haven't sent an ADDED
      //      event for the model then send the added event and persist.
      //    * If the entity has already been persisted *or* if the entity
      //      has not been persisted but we have already sent the ADDED
      //      event for it then emit a VALUE_CHANGE and persist *if* the
      //      model is dirty.
      //
      // NOTE: The added sent but model not persisted case should only happen
      //       in the face of DB failures. In this case (and in the value
      //       change sent but not persisted case) our in-memory model and the
      //       persisted model have diverged with the rest of the system having
      //       observed state changes based on the in-memory model.
      //
      //       Since the in-memory model is the reference model for the rest of
      //       the system our only recourse is to continually attempt to
      //       re-converge the in-memory and persisted models by continually
      //       attempting the DB operation until it succeeds and/or until we
      //       fail (at which point the in-memory model and persisted model are
      //       converged by reloading the in-memory model from the persisted
      //       model with the result being possible inconsistencies seen by the
      //       rest of the system).
      //
      // NOTE: An alternative course of action here would be to ensure the DB
      //       operation is successful before sending the ADDED or
      //       VALUE_CHANGED event. This is essentially the classic consistency
      //       vs. availability trade off with the current choice favoring
      //       availability over consistency.
      //
      //       Furthermore, even if we chose consistency here it would be
      //       difficult to enforce consistency in the surrounding pieces of
      //       code.
      //
      // NOTE: The current implementation does not periodically re-attempt
      //       failed DB operations for places that are not receiving
      //       important messages regularly. We may want to implement that
      //       behavior at some point.
      if(deleted) {
         logger().debug("Ignoring commit on deleted model");
      } else if(!entity.isPersisted() && !added) {
         added();
         save(ImmutableMap.of());
      } else if(entity.isDirty()) {
         Map<String,Object> newValues = changed();
         try {
            save(newValues);
         } finally {
            // NOTE: This needs to be after the clearing of the dirty attributes
            // otherwise we get infinite recursive loop.
            List<ModelChangedEvent> changes = entity.commit();
            fireChangeEvents(changes);
         }
      }
      actor = null;
   }
   
   /* (non-Javadoc)
    * @see com.iris.common.subsystem.SubsystemContext#delete()
    */
   @Override
   public void delete() {
      this.deleted = true;
      failedToSave = null;
      subsystemDao.deleteByAddress(entity.getAddress());
      entity.setCreated(null);
      deleted();
   }
   
   public Address getActor() {
      return actor == null || actor.isBroadcast() ? model.getAddress() : actor;
   }
   
   @Override
   public void setActor(Address actor) {
      this.actor = actor == null || actor.isBroadcast() ? null : actor;
   }

   @Override
   public Subscription addBindSubscription(@NonNull Subscription subscription) {
      synchronized(bindSubscriptions) {
         bindSubscriptions.add(subscription);
         return subscription;
      }
   }

   @Override
   public void unbind() {
      synchronized(bindSubscriptions) {
         bindSubscriptions.forEach(Subscription::remove);
         bindSubscriptions.clear();
      }
   }

   private void save(Map<String, Object> changes) {
      try {
         // At this point we have already sent an ADDED or VALUE_CHANGE based
         // on the dirty attributes so we need to consume the dirty attributes
         // even if the DB operation fails.
         //
         // Failing to commit data into the DB because of a temporary DB failure
         // is also bad and retrying the failed operations can be done pretty
         // cheaply. We do that by catching any failure and tracking the attributes
         // that could not be persisted for another attempt later.
         Date modified = subsystemDao.save(entity, failedToSave);
         failedToSave = null;

         if (!entity.isPersisted()) {
            entity.setCreated(modified);
         }

         entity.setModified(modified);
      } catch (Throwable th) {
         if (failedToSave == null) {
            failedToSave = new HashMap<>();
         }

         failedToSave.putAll(changes);
         throw th;
      }
   }
   
   private PlatformMessage.Builder buildMessage() {
      return 
         PlatformMessage
            .builder()
            .from(model.getAddress())
            .withPlaceId(getPlaceId())
            .withPopulation(getPopulation())
            .withActor(getActor())
            ;
   }

   private PlatformMessage.Builder buildRequest(Address destination) {
      return 
         buildMessage()
            .isRequestMessage(true)
            .to(destination)
            ;
   }

   private void added() {
      Map<String, Object> attributes = filterVariables(entity.toMap());
      MessageBody added = MessageBody.buildMessage(Capability.EVENT_ADDED, attributes);
      broadcast(added);
      this.added = true;
   }
   
   private Map<String,Object> changed() {
      Map<String, Object> attributes = filterVariables(entity.getDirtyAttributes());
      if (attributes.isEmpty()) {
         return attributes;
      }

      MessageBody changed = MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, attributes);
      broadcast(changed);
      return attributes;
   }

   private void fireChangeEvents(List<ModelChangedEvent> changes) {
      if (changes == null || changes.isEmpty()) {
         return;
      }

      // Subsystems depend on getting model changed events for all of the
      // models in the model store. For all models except the subsystem
      // models this happens in the ModelStore.update(PlatformMessage) method
      // where any mutations described by the message are applied. 
      
      // For subsystem models, however, the PlatformSubystemModelStore
      // specifically ignores these platform messages for so that mutations
      // aren't applied twice, once directly and then another time after
      // reading the value change just generated above back off of the message
      // bus (this double application should be logically safe as it should be
      // an idempotent operation, but its unnecessary and a potentially
      // dangerous flow since the round trip to the messaging system could fail
      // leaving the two different models in different states).
      
      // Since value change mutations aren't being processed by the normal flow
      // we need to generate them somewhere and immediately after producing the
      // value change describing the mutations seems as good a place as any.
      //
      // NOTE: Correct operation of this depends on this method being called
      // before the dirty attributes are cleared in the model entity.
      for(ModelChangedEvent change: changes) {
      	if(change.getAttributeName().startsWith("_")) {
      		// skip variables
      		continue;
      	}
      	
      	((PlatformSubsystemModelStore) models()).fireModelEvent(change);
      }
   }
   
   private void deleted() {
      Map<String, Object> attributes = filterVariables(entity.toMap());
      MessageBody deleted = MessageBody.buildMessage(Capability.EVENT_DELETED, attributes);
      broadcast(deleted);
   }

   private Map<String, Object> filterVariables(Map<String, Object> map) {
      Iterator<String> names = map.keySet().iterator();
      while(names.hasNext()) {
         if(names.next().startsWith("_")) {
            names.remove();
         }
      }
      return map;
   }
   
   @Override
   public String toString() {
	   return "PlatformSubsystemContext [entity=" + entity + ", model=" + model
	         + ", actor=" + actor
	         + ", deleted=" + deleted + "]";
   }



	public static class Builder {
      private UUID placeId;
      private String population;
      private UUID accountId;
      private Logger logger;
      private ModelStore models;
      private SubsystemDao subsystemDao;
      private PlatformMessageBus platformBus;
      private IrisCorrelator<ResponseAction<?>> correlator;
      private Scheduler scheduler;
      private Listener<? super AddressableEvent> scheduledEventListener;
      private TimeZone tz;
      
      /**
       * @param placeId the placeId to set
       */
      public Builder withPlaceId(UUID placeId) {
         this.placeId = placeId;
         return this;
      }
      
      public Builder withPopulation(String population) {
      	this.population = population;
      	return this;
      }
      
      public Builder withAccountId(UUID accountId) {
         this.accountId = accountId;
         return this;
      }
      
      /**
       * @param logger the logger to set
       */
      public Builder withLogger(Logger logger) {
         this.logger = logger;
         return this;
      }
      
      /**
       * @param models the models to set
       */
      public Builder withModels(ModelStore models) {
         this.models = models;
         return this;
      }
      
      public Builder withSubsystemDao(SubsystemDao subsystemDao) {
         this.subsystemDao = subsystemDao;
         return this;
      }
      
      /**
       * @param platformBus the platformBus to set
       */
      public Builder withPlatformBus(PlatformMessageBus platformBus) {
         this.platformBus = platformBus;
         return this;
      }
      
      /**
       * @param correlator the message correlator to set
       */
      public Builder withCorrelator(IrisCorrelator correlator) {
         this.correlator = correlator;
         return this;
      }

      /**
       * @param scheduler the scheduler to set
       */
      public Builder withScheduler(Scheduler scheduler) {
         this.scheduler = scheduler;
         return this;
      }
      
      public Builder withTimezone(TimeZone tz) {
         this.tz = tz;
         return this;
      }
      /**
       * @param executor the executor to set
       */
      public Builder withScheduledEventListener(Listener<? super AddressableEvent> scheduledEventListener) {
         this.scheduledEventListener = scheduledEventListener;
         return this;
      }

      public <M extends SubsystemModel> PlatformSubsystemContext<M> build(Class<M> type, ModelEntity entity) throws Exception {
         M model = type.getConstructor(Model.class).newInstance(entity);
         return new PlatformSubsystemContext<M>(placeId, population, accountId, logger, models, model, entity, subsystemDao, platformBus, correlator, scheduler, scheduledEventListener,tz);
      }
   }
   
   private static class CorrelatorAction implements IrisCorrelator.Action<ResponseAction<?>> {
      private final Listener<? super AddressableEvent> eventLoop;
      private final Address address;
      private final ResponseAction<?> action;

      public CorrelatorAction(
            Listener<? super AddressableEvent> eventLoop,
            Address address,
            ResponseAction<?> action
      ) {
         this.eventLoop = eventLoop;
         this.address = address;
         this.action = action;
      }

      @Override
      public ResponseAction<?> onResponse(PlatformMessage response) {
         return action;
      }

      @Override
      public void onTimeout() {
         SubsystemResponseEvent event = SubsystemResponseEvent.timeout(address, action);
         eventLoop.onEvent(event);
      }

   }

}


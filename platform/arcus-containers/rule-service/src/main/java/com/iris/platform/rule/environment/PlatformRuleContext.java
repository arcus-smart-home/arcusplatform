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
package com.iris.platform.rule.environment;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;

import com.google.common.base.Preconditions;
import com.iris.common.rule.RuleContext;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.ScheduledEvent;
import com.iris.common.rule.event.ScheduledEventHandle;
import com.iris.common.rule.simple.BaseRuleContext;
import com.iris.common.rule.simple.NamespaceContext;
import com.iris.common.rule.simple.OverrideContext;
import com.iris.common.scheduler.ScheduledTask;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.model.serv.PlaceModel;
import com.iris.messages.type.Population;
import com.iris.model.type.AttributeTypes;

/**
 * 
 */
// TODO there's a lot of overlap between RuleContext and RuleExecutor...
public class PlatformRuleContext extends BaseRuleContext implements RuleContext {


   public static Builder builder() {
      return new Builder();
   }
   
   private final Address source;
   private final UUID placeId;
   private final Logger logger;
   private final PlatformMessageBus platformBus;
   private final RuleModelStore models;
   private final Map<String, Object> variables;
   
   private final TimeZone tz;
   
   private final PlaceExecutorEventLoop eventLoop;
   

   protected PlatformRuleContext(
         Address source, 
         UUID placeId,
         Logger logger, 
         PlatformMessageBus platformBus,
         RuleModelStore models,
         PlaceExecutorEventLoop eventLoop,
         TimeZone tz,
         Map<String,Object>variables
   ) { 
      Preconditions.checkNotNull(source, "source may not be null");
      Preconditions.checkNotNull(placeId, "placeId may not be null");
      Preconditions.checkNotNull(logger, "logger may not be null");
      Preconditions.checkNotNull(platformBus, "platformBus may not be null");
      Preconditions.checkNotNull(models, "models may not be null");
      Preconditions.checkNotNull(eventLoop, "eventLoop may not be null");
      Preconditions.checkNotNull(tz, "tz may not be null");
      this.source = source;
      this.placeId = placeId;
      this.logger = logger;
      this.platformBus = platformBus;
      this.models = models;
      this.eventLoop = eventLoop;
      this.tz = tz;
      this.variables=new HashMap<>(variables);
   }
   
   @Override
   public UUID getPlaceId() {
      return placeId;
   }
   
   
   
   @Override
	public String getPopulation() {
   	Model place = getModelByAddress(Address.platformService(getPlaceId(), PlaceCapability.NAMESPACE));
      if(place != null) {         
         return PlaceModel.getPopulation(place, Population.NAME_GENERAL);
      }
      logger.warn("Unable to load place for context [{}]", source);
      return Population.NAME_GENERAL;
	}

	@Override
   public boolean isPremium() {
      Model model = getModelByAddress(Address.platformService(getPlaceId(), PlaceCapability.NAMESPACE));
      if(model == null) {
         return false;
      }
      String serviceLevel = PlaceModel.getServiceLevel(model);
      return ServiceLevel.isPremiumOrPromon(serviceLevel);
   }

   @Override
   public String getServiceLevel() {
      Model place = getModelByAddress(Address.platformService(getPlaceId(), PlaceCapability.NAMESPACE));
      if(place == null) {
         logger.warn("Unable to load place for context [{}]", source);
         return "";
      }
      return PlaceModel.getServiceLevel(place, "");
   }

   @Override
   public Iterable<Model> getModels() {
      return models.getModels();
   }

   @Override
   @Nullable
   public Model getModelByAddress(Address address) {
      return models.getModelByAddress(address);
   }

   @Override
   @Nullable
   public Object getAttributeValue(Address address, String attributeName) {
      return models.getAttributeValue(address, attributeName);
   }

   @Override
   public Object getVariable(String name) {
      return variables.get(name);
   }

   @SuppressWarnings("unchecked")
   @Override
   public <T> T getVariable(String name, Class<T> type) {
      Preconditions.checkNotNull(name, "name may not be null");
      Preconditions.checkNotNull(type, "type may not be null");
      Object value = variables.get(name);
      if(value == null) {
    	  return null;
      }else if(type.isAssignableFrom(value.getClass())) {
         return (T) value;
      }
      return (T) AttributeTypes.fromJavaType(type).coerce(value);
   }

   @Override
   public Object setVariable(String name, Object value) {
      markDirtyVariable(name, value);
      if(value==null){
         return variables.remove(name);
      }
      else{
         return variables.put(name, value);
      }
   }

   @Override
   public Logger logger() {
      return logger;
   }
   
   @Override
   public Calendar getLocalTime() {
      return Calendar.getInstance(tz);
   }

   @Override
   public void broadcast(MessageBody payload) {
      PlatformMessage pm = 
            PlatformMessage
               .builder()
               .from(source)
               .withPlaceId(getPlaceId())
               .withPopulation(getPopulation())
               .withPayload(payload)
               .create();
      platformBus.send(pm);
   }

   @Override
   public void send(Address destination, MessageBody payload) {
      PlatformMessage pm = 
            PlatformMessage
               .builder()
               .to(destination)
               .from(source)
               .withPlaceId(getPlaceId())
               .withPopulation(getPopulation())
               .withPayload(payload)
               .withActor(source)
               .isRequestMessage(true)
               .create();
      platformBus.send(pm);
   }

   @Override
   public String request(Address destination, MessageBody payload) {
      String correlationId = UUID.randomUUID().toString();
      PlatformMessage pm = 
            PlatformMessage
               .request(destination)
               .from(source)
               .withPlaceId(getPlaceId())
               .withPopulation(getPopulation())
               .withCorrelationId(correlationId)
               .withPayload(payload)
               .withActor(source)
               .create();
      platformBus.send(pm);
      return correlationId;
   }

   @Override
   public ScheduledEventHandle wakeUpIn(long time, TimeUnit unit) {
      long timeMs = TimeUnit.MILLISECONDS.convert(time, unit);
      return scheduleWakeup(System.currentTimeMillis() + timeMs, timeMs);
   }

   @Override
   public ScheduledEventHandle wakeUpAt(Date timestamp) {
      Preconditions.checkNotNull(timestamp, "timestamp may not be null");
      return scheduleWakeup(timestamp.getTime(), timestamp.getTime() - System.currentTimeMillis());
   }
   
   @Override
   public Map<String, Object> getVariables() {
      return variables;
   }

   @Override
   public RuleContext override(Map<String, Object> variables) {
      return new OverrideContext(variables, this);
   }
   

   @Override
   public RuleContext override(String namespace) {
      return new NamespaceContext(namespace, this);
   }

   private ScheduledEventHandle scheduleWakeup(long timestamp, long deltaMs) {
      ScheduledEvent event = new ScheduledEvent(timestamp);
      ScheduledTask task = eventLoop.scheduleDelayed(placeId, event, deltaMs, TimeUnit.MILLISECONDS);
      return new ScheduledEventHandleImpl(task, event);
   }

   public static class Builder {
      private Address source;
      private UUID placeId;
      private Logger logger;
      private PlatformMessageBus platformBus;
      private RuleModelStore models;
      private Map<String, Object> variables = new HashMap<String, Object>();
      private PlaceExecutorEventLoop eventLoop;
      private TimeZone timeZone;
      
      protected Builder() { }
      
      /**
       * @return the source
       */
      public Address getSource() {
         return source;
      }
      
      /**
       * @param source the source to set
       */
      public Builder withSource(Address source) {
         this.source = source;
         return this;
      }
      
      public UUID getPlaceId() {
         return placeId;
      }
      
      public Builder withPlaceId(UUID placeId) {
         this.placeId = placeId;
         return this;
      }
      
      /**
       * @return the logger
       */
      public Logger getLogger() {
         return logger;
      }
      
      /**
       * @param logger the logger to set
       */
      public Builder withLogger(Logger logger) {
         this.logger = logger;
         return this;
      }
      
      /**
       * @return the platformBus
       */
      public PlatformMessageBus getPlatformBus() {
         return platformBus;
      }
      
      /**
       * @param platformBus the platformBus to set
       */
      public Builder withPlatformBus(PlatformMessageBus platformBus) {
         this.platformBus = platformBus;
         return this;
      }
      
      /**
       * @return the models
       */
      public RuleModelStore getModels() {
         return models;
      }
      
      /**
       * @param models the models to set
       */
      public Builder withModels(RuleModelStore models) {
         this.models = models;
         return this;
      }
      
      /**
       * @return the variables
       */
      public Map<String, Object> getVariables() {
         return variables;
      }
      
      /**
       * @param variables the variables to set
       */
      public Builder withVariables(Map<String, Object> variables) {
         this.variables = variables;
         return this;
      }
      
      public TimeZone getTimeZone() {
         return timeZone;
      }
      
      public Builder withTimeZone(TimeZone timeZone) {
         this.timeZone = timeZone;
         return this;
      }
      
      public Builder withEventLoop(PlaceExecutorEventLoop eventLoop) {
         this.eventLoop = eventLoop;
         return this;
      }
      
      public PlatformRuleContext build() {
         return new PlatformRuleContext(source, placeId, logger, platformBus, models, eventLoop, timeZone,variables);
      }
      
   }

   private class ScheduledEventHandleImpl implements ScheduledEventHandle {
      private final ScheduledTask task;
      private final ScheduledEvent event;

      ScheduledEventHandleImpl(ScheduledTask task, ScheduledEvent event) {
         this.task = task;
         this.event = event;
      }

      @Override
      public boolean isPending() {
         return task.isPending();
      }

      @Override
      public boolean cancel() {
         return task.cancel();
      }

      @Override
      public boolean isReferencedEvent(RuleEvent event) {
         return this.event == event;
      }



   }

}


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
package com.iris.common.rule.simple;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.iris.common.rule.RuleContext;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.ScheduledEvent;
import com.iris.common.rule.event.ScheduledEventHandle;
import com.iris.io.json.JSON;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.serv.PlaceModel;
import com.iris.messages.type.Population;
import com.iris.model.type.AttributeTypes;

/**
 * 
 */
public class SimpleContext extends BaseRuleContext implements RuleContext {

   private final UUID placeId;
   private final Address source;
   private final Logger logger;
   // LinkedHashMap because we iterate models a lot
   private final Map<String, Model> models = new LinkedHashMap<String, Model>();
   private final Collection<Model> unmodifiableModels = Collections.unmodifiableCollection(models.values());
   private final Map<String, Object> variables = new HashMap<String, Object>();
   private final Map<String, Object> unmodifiableVariables = Collections.unmodifiableMap(variables);
   private final BlockingQueue<PlatformMessage> messages = new ArrayBlockingQueue<PlatformMessage>(100);
   private final BlockingQueue<ScheduledEvent> events = new ArrayBlockingQueue<ScheduledEvent>(100);
   
   private volatile Calendar localTime;

   public SimpleContext(UUID placeId, Address source, Logger logger) {
      Preconditions.checkNotNull(placeId, "placeId may not be null");
      Preconditions.checkNotNull(source, "source may not be null");
      Preconditions.checkNotNull(logger, "logger may not be null");
      this.placeId = placeId;
      this.source = source;
      this.logger = logger;
   }
   
   // TODO could replace this with addMessageListener to make it push instead of poll
   public BlockingQueue<PlatformMessage> getMessages() {
      return messages;
   }
   
   public BlockingQueue<ScheduledEvent> getEvents() {
      return events;
   }
   
   public Model createModel(String type, Object id) {
      Preconditions.checkArgument(!StringUtils.isEmpty(type), "type may not be empty");
      Preconditions.checkNotNull(id, "id may not be null");
      SimpleModel model = new SimpleModel();
      model.setAttribute(Capability.ATTR_TYPE, type);
      model.setAttribute(Capability.ATTR_ID, id.toString());
      model.setAttribute(Capability.ATTR_ADDRESS, Address.platformService(id, type).getRepresentation());
      putModel(model);
      return model;
   }
   
   public void putModel(Model model) {
      Preconditions.checkNotNull(model, "model may not be null");
      String key = getKey(model.getAddress());
      Preconditions.checkArgument(key != null, "type and id must be set on the Model");
      models.put(key, model);
   }
   
   public void setLocalTime(@Nullable Calendar localTime) {
      if(localTime == null) {
         clearLocalTime();
      }
      this.localTime = (Calendar) localTime.clone();
   }
   
   public void clearLocalTime() {
      this.localTime = null;
   }
   
   protected void submit(PlatformMessage message) {
   	PlatformMessage normalized = JSON.fromJson(JSON.toJson(message), PlatformMessage.class);
      if(!this.messages.offer(normalized)) {
         logger().warn("Unable to send message [{}] due to queue backlog", message);
      }
   }
   
   @Override
   public UUID getPlaceId() {
      return placeId;
   }
   
   
   
   @Override
	public String getPopulation() {
   	Model model = getModelByAddress(Address.platformService(getPlaceId(), PlaceCapability.NAMESPACE));
      if(model != null) {
         return PlaceModel.getPopulation(model, Population.NAME_GENERAL);
      }
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
      return unmodifiableModels;
   }

   @Override
   @Nullable
   public Model getModelByAddress(Address address) {
      String key = getKey(address);
      return getByKey(key);
   }

   @Override
   @Nullable
   public Object getAttributeValue(Address address, String attributeName) {
      Preconditions.checkNotNull(attributeName, "attributeName may not be null");
      Model model = getModelByAddress(address);
      if(model == null) {
         return null;
      }
      return model.getAttribute(attributeName);
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
      if(value == null || type.isAssignableFrom(value.getClass())) {
         return (T) value;
      }
      return (T) AttributeTypes.fromJavaType(type).coerce(value);
   }

   @Override
   public Object setVariable(String name, Object value) {
      markDirtyVariable(name, value);
      return variables.put(name, value);
   }

   @Override
   public Logger logger() {
      return logger;
   }
   
   @Override
   public Calendar getLocalTime() {
      Calendar localTime = this.localTime;
      if(localTime == null) {
         return Calendar.getInstance();
      }
      return (Calendar) localTime.clone();
   }

   @Override
   public void broadcast(MessageBody payload) {
      PlatformMessage pm = 
            PlatformMessage
               .builder()
               .from(source)
               .withPayload(payload)
               .create();
      submit(pm);
   }

   @Override
   public void send(Address destination, MessageBody payload) {
      PlatformMessage pm = 
            PlatformMessage
               .builder()
               .to(destination)
               .from(source)
               .withPayload(payload)
               .isRequestMessage(true)
               .create();
      submit(pm);
   }

   @Override
   public String request(Address destination, MessageBody payload) {
      String correlationId = UUID.randomUUID().toString();
      PlatformMessage pm = 
            PlatformMessage
               .builder()
               .to(destination)
               .from(source)
               .withPayload(payload)
               .isRequestMessage(true)
               .withCorrelationId(correlationId)
               .create();
      submit(pm);
      return correlationId;
   }

   @Override
   public ScheduledEventHandle wakeUpIn(long time, TimeUnit unit) {
      return wakeUpAt(getLocalTime().getTimeInMillis() + unit.toMillis(time), this.events);
   }

   @Override
   public ScheduledEventHandle wakeUpAt(Date timestamp) {
      Preconditions.checkNotNull(timestamp, "timestamp may not be null");
      return wakeUpAt(timestamp.getTime(), this.events);
   }

   @Override
   public Map<String, Object> getVariables() {
      return unmodifiableVariables;
   }

   @Override
   public RuleContext override(Map<String, Object> variables) {
      return new OverrideContext(variables, this);
   }

   @Override
   public RuleContext override(String namespace) {
      return new NamespaceContext(namespace, this);
   }

   private @Nullable Model getByKey(@Nullable String key) {
      if(key == null) {
         return null;
      }
      return models.get(key);
   }
   
   private static @Nullable String getKey(@Nullable Address address) {
      if(address == null) {
         return null;
      }
      return address.getRepresentation();
   }
   
   private static ScheduledEventHandle wakeUpAt(long timestamp, final BlockingQueue<ScheduledEvent> events) {
      final ScheduledEvent event = new ScheduledEvent(timestamp);
      if(!events.offer(event)) {
         throw new IllegalStateException("Event queue at capacity");
      }
      return new ScheduledEventHandle() {
         
         @Override
         public boolean isPending() {
            return events.contains(event);
         }
         
         @Override
         public boolean cancel() {
            return events.remove(event);
         }
         
         @Override
         public boolean isReferencedEvent(RuleEvent other) {
            return other == event;
         }
      };
      
   }
}


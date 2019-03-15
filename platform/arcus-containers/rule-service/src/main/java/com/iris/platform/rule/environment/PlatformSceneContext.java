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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.iris.common.rule.action.ActionContext;
import com.iris.common.rule.event.ScheduledEventHandle;
import com.iris.common.scene.SceneContext;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.SceneTemplateCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.PersistentModel;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.model.serv.PlaceModel;
import com.iris.messages.model.serv.SceneModel;
import com.iris.messages.type.Population;
import com.iris.model.type.AttributeTypes;
import com.iris.platform.scene.SceneDao;
import com.iris.platform.scene.catalog.SceneTemplate;

/**
 * 
 */
public class PlatformSceneContext implements SceneContext {
   

   public static Builder builder() {
      return new Builder();
   }
   
   private final UUID placeId;
   private final Logger logger;
   private final PlatformMessageBus platformBus;
   private final SceneDao sceneDao;
   private final RuleModelStore models;
   private final Map<String, Object> variables = new HashMap<String, Object>();
   private final Map<String, Object> unmodifiableVariables = Collections.unmodifiableMap(variables);
   
   private final TimeZone tz;
   
   private PersistentModel model;
   private boolean deleted = false;
   private Address actor;
   
   protected PlatformSceneContext(
         PersistentModel model, 
         UUID placeId,
         Logger logger, 
         PlatformMessageBus platformBus,
         SceneDao sceneDao,
         RuleModelStore models,
         TimeZone tz
   ) { 
      Preconditions.checkNotNull(model, "model may not be null");
      Preconditions.checkNotNull(placeId, "placeId may not be null");
      Preconditions.checkNotNull(logger, "logger may not be null");
      Preconditions.checkNotNull(platformBus, "platformBus may not be null");
      Preconditions.checkNotNull(sceneDao, "sceneDao may not be null");
      Preconditions.checkNotNull(models, "models may not be null");
      Preconditions.checkNotNull(tz, "tz may not be null");
      this.model = model;
      this.placeId = placeId;
      this.logger = logger;
      this.platformBus = platformBus;
      this.sceneDao = sceneDao;
      this.models = models;
      this.tz = tz;
   }
   
   public boolean isPersisted() {
      return model.isPersisted();
   }
   
   public boolean isDeleted() {
      return deleted;
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
      logger.warn("Unable to load place for context [{}]", placeId);
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
         logger.warn("Unable to load place for context [{}]", model.getAddress());
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
      if(value == null || type.isAssignableFrom(value.getClass())) {
         return (T) value;
      }
      return (T) AttributeTypes.fromJavaType(type).coerce(value);
   }

   @Override
   public Object setVariable(String name, Object value) {
      return variables.put(name, value);
   }

	@Override
	public void setActor(Address actor) {
		this.actor = actor;
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
               .from(model.getAddress())
               .withPlaceId(getPlaceId())
               .withPopulation(getPopulation())
               .withActor(actor)
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
               .from(model.getAddress())
               .withPlaceId(getPlaceId())
               .withPopulation(getPopulation())
               .withPayload(payload)
               .withActor(model.getAddress())
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
               .from(model.getAddress())
               .withPlaceId(getPlaceId())
               .withPopulation(getPopulation())
               .withCorrelationId(correlationId)
               .withPayload(payload)
               .withActor(model.getAddress())
               .create();
      platformBus.send(pm);
      return correlationId;
   }

   @Override
   public Map<String, Object> getVariables() {
      return unmodifiableVariables;
   }

   /* (non-Javadoc)
    * @see com.iris.common.rule.action.ActionContext#override(java.util.Map)
    */
   @Override
   public ActionContext override(Map<String, Object> variables) {
      throw new UnsupportedOperationException("Not implemented");
   }

   @Override
   public ActionContext override(String namespace) {
      throw new UnsupportedOperationException("Not implemented");
   }

   /* (non-Javadoc)
    * @see com.iris.common.scene.SceneContext#model()
    */
   @Override
   public PersistentModel model() {
      return model;
   }
   
   @Override
   public ScheduledEventHandle wakeUpIn(long time, TimeUnit unit) {
      throw new UnsupportedOperationException("SceneContext does not currenlty support wake up");
   }

   @Override
   public ScheduledEventHandle wakeUpAt(Date timestamp) {
      throw new UnsupportedOperationException("SceneContext does not currenlty support wake up");
   }
   
   @Override
   public void delete() {
      deleted = true;
   }

   /* (non-Javadoc)
    * @see com.iris.common.scene.SceneContext#commit()
    */
   @Override
   public void commit() {
	   try{
	      String messageType;
	      Map<String, Object> attributes;
	      if(deleted) {
	         if(sceneDao.delete( model.getAddress() )) {
	            MessageBody message = MessageBody.buildMessage(Capability.EVENT_DELETED, ImmutableMap.of());
	            broadcast(message);
	         }
	         String templateId = SceneModel.getTemplate(model);
	         if(templateId != null && !SceneTemplate.CUSTOM_TEMPLATE.equals(templateId)) {
	            MessageBody message = MessageBody.buildMessage(
	                  Capability.EVENT_VALUE_CHANGE, 
	                  ImmutableMap.of(SceneTemplateCapability.ATTR_AVAILABLE, true)
	            );
	            PlatformMessage pm = 
	                  PlatformMessage
	                     .builder()
	                     .from(Address.platformService(templateId, SceneTemplateCapability.NAMESPACE))
	                     .withPlaceId(getPlaceId())
	                     .withPopulation(getPopulation())
	                     .withPayload(message)
	                     .create();
	            platformBus.send(pm);
	         }
	         return;
	      }
	      
	      if(model.isPersisted() && !model.isDirty()) {
	         // no-op
	         return;
	      }
	      
	      
	      if(model.isPersisted()) {
	         messageType = Capability.EVENT_VALUE_CHANGE;
	         attributes = model.getDirtyAttributes();
	         /* add scene name to attributes for broadcast
	          * since it is always removed by the model.getDirtyAttributes()
	          * unless the scene name changed
	          */
	      }
	      else {
	         messageType = Capability.EVENT_ADDED;
	         attributes = model.toMap();
	      }
	      model = sceneDao.save(model);
	      
	      MessageBody message = MessageBody.buildMessage(messageType, attributes);
	      broadcast(message);
	   } finally {
		   actor = null;
	   }
   }

   public static class Builder {
      private PersistentModel model;
      private UUID placeId;
      private Logger logger;
      private PlatformMessageBus platformBus;
      private RuleModelStore models;
      private Map<String, Object> variables = new HashMap<String, Object>();
      private TimeZone timeZone;
      private SceneDao sceneDao;
      
      protected Builder() { }
      
      /**
       * @return the model
       */
      public PersistentModel getModel() {
         return model;
      }
      
      /**
       * @param model the model to set
       */
      public Builder withModel(PersistentModel model) {
         this.model = model;
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
      
      public SceneDao getSceneDao() {
         return sceneDao;
      }
      
      public Builder withSceneDao(SceneDao sceneDao) {
         this.sceneDao = sceneDao;
         return this;
      }
      
      public PlatformSceneContext build() {
         return new PlatformSceneContext(model, placeId, logger, platformBus, sceneDao, models, timeZone);
      }
      
   }

}


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
package com.iris.platform.rule.service.handler.scenetmpl;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.io.json.JSON;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.SceneTemplateCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Place;
import com.iris.platform.rule.environment.ContextLoader;
import com.iris.platform.rule.environment.PlaceExecutorRegistry;
import com.iris.platform.rule.environment.SceneActionBuilder;
import com.iris.platform.scene.SceneDao;
import com.iris.platform.scene.SceneDefinition;
import com.iris.platform.scene.SceneTemplateEntity;

/**
 * 
 */
@Singleton
public class CreateRequestHandler implements ContextualRequestMessageHandler<SceneTemplateEntity> {
   private final ContextLoader loader;
   private final PlatformMessageBus platformBus;
   private final PlaceExecutorRegistry registry;
   private final SceneDao sceneDao;
   private final PlaceDAO placeDao;
   private final BeanAttributesTransformer<SceneDefinition> transformer;

   @Inject
   public CreateRequestHandler(
   		ContextLoader loader,
         PlatformMessageBus platformBus,
         PlaceExecutorRegistry registry,
         PlaceDAO placeDao, 
         SceneDao sceneDao,
         BeanAttributesTransformer<SceneDefinition> transformer
   ) {
   	this.loader = loader;
      this.platformBus = platformBus;
      this.registry = registry;
      this.placeDao = placeDao;
      this.sceneDao = sceneDao;
      this.transformer = transformer;
   }

   /* (non-Javadoc)
    * @see com.iris.core.platform.PlatformRequestMessageHandler#getMessageType()
    */
   @Override
   public String getMessageType() {
      return SceneTemplateCapability.CreateRequest.NAME;
   }

   /* (non-Javadoc)
    * @see com.iris.core.platform.ContextualRequestMessageHandler#handleRequest(java.lang.Object, com.iris.messages.PlatformMessage)
    */
   @Override
   public MessageBody handleRequest(SceneTemplateEntity template, PlatformMessage message) {
      if(!template.isAvailable()) {
         return Errors.invalidRequest("A scene has already been created from this template");
      }
      
      MessageBody request = message.getValue();
      UUID placeId = UUID.fromString( SceneTemplateCapability.CreateRequest.getPlaceId(request) );
      Errors.assertPlaceMatches(message, placeId);
      
      String name = SceneTemplateCapability.CreateRequest.getName(request);
      List<Map<String, Object>> actions = SceneTemplateCapability.CreateRequest.getActions(request);
      
      Place place = placeDao.findById(placeId);
      SceneDefinition definition = new SceneDefinition();
      definition.setPlaceId(placeId);
      definition.setName( Optional.fromNullable(name).or(template.getName()) );
      definition.setTemplate(template.getId());
      if(actions != null && !actions.isEmpty()) {
      	// validate actions
      	SceneActionBuilder
      		.builder(placeId)
      		.withContext(loader.load(placeId))
      		.withTemplateId(template.getId())
      		.withActions(actions)
      		.buildStrict();
      	
      	// assign actions
      	definition.setAction(JSON.toJson(actions).getBytes());
      }
      
      sceneDao.create(place, definition);
      broadcastAdded(definition, message, place.getPopulation());
      if(!template.isCustom()) {
         broadcastAvailableValueChange(definition.getPlaceId(), place.getPopulation(), template.getId(), message);
      }
      registry.reload(placeId);
      
      return 
            SceneTemplateCapability.CreateResponse
               .builder()
               .withAddress(definition.getAddress())
               .build();
   }

   private void broadcastAdded(SceneDefinition definition, PlatformMessage context, String population) {
      Address address = Address.fromString(definition.getAddress());
      PlatformMessage message =
            PlatformMessage
               // copy all the headers properly
               .broadcast(context)
               .from(address)
               .withPlaceId(definition.getPlaceId())
               .withPopulation(population)
               .withPayload(Capability.EVENT_ADDED, transformer.transform(definition))
               .create();
      platformBus.send(message);
   }

   private void broadcastAvailableValueChange(UUID placeId, String population, String templateId, PlatformMessage context) {
      Address address = Address.platformService(templateId, SceneTemplateCapability.NAMESPACE);
      PlatformMessage message =
            PlatformMessage
               // copy all the headers properly
               .broadcast(context)
               .from(address)
               .withPlaceId(placeId)
               .withPopulation(population)
               .withPayload(
                     Capability.EVENT_VALUE_CHANGE, 
                     ImmutableMap.of(SceneTemplateCapability.ATTR_AVAILABLE, false)
               )
               .create();
      platformBus.send(message);
   }
   
}


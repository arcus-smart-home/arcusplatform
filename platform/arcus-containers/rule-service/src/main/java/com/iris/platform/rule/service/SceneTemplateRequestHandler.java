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
package com.iris.platform.rule.service;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.platform.AbstractPlatformService;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.platform.RequestHandlers;
import com.iris.core.platform.handlers.GetAttributesModelRequestHandler;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.capability.SceneTemplateCapability;
import com.iris.messages.errors.NotFoundException;
import com.iris.messages.model.PersistentModel;
import com.iris.platform.model.ModelEntity;
import com.iris.platform.rule.service.handler.scenetmpl.CreateRequestHandler;
import com.iris.platform.rule.service.handler.scenetmpl.ResolveActionsRequestHandler;
import com.iris.platform.scene.SceneTemplateEntity;
import com.iris.platform.scene.SceneTemplateManager;
import com.iris.platform.scene.catalog.SceneTemplate;

/**
 * 
 */
@Singleton
public class SceneTemplateRequestHandler extends AbstractPlatformService {
   public static final String PROP_THREADPOOL = "service.scenetmpl.threadpool";

   private final SceneTemplateManager sceneTemplateManager;
   private final BeanAttributesTransformer<SceneTemplateEntity> transformer;
   private final Function<PlatformMessage, SceneTemplate> templateLoader = (message) -> loadSceneTemplate(message);
   private final Function<PlatformMessage, SceneTemplateEntity> templateEntityLoader = (message) -> loadSceneTemplateEntity(message);
   private final Function<PlatformMessage, PersistentModel> templateModelLoader = (message) -> loadSceneTemplateModel(message);
   
   private final Consumer<PlatformMessage> dispatcher;
   
   @Inject
   protected SceneTemplateRequestHandler(
         @Named(PROP_THREADPOOL) Executor executor,
         PlatformMessageBus platformBus,
         SceneTemplateManager sceneTemplateDao,
         BeanAttributesTransformer<SceneTemplateEntity> transformer,
         GetAttributesModelRequestHandler getAttributesRequestHandler,
         CreateRequestHandler createHandler,
         ResolveActionsRequestHandler resolveActionsHandler
   ) {
      super(platformBus, SceneTemplateCapability.NAMESPACE, executor);
      this.sceneTemplateManager = sceneTemplateDao;
      this.transformer = transformer;
      // TODO change to a builder pattern?
      this.dispatcher = 
            RequestHandlers
               .toDispatcher(
                     platformBus,
                     RequestHandlers.toRequestHandler(templateModelLoader, getAttributesRequestHandler),
                     RequestHandlers.toRequestHandler(templateEntityLoader, createHandler),
                     RequestHandlers.toRequestHandler(templateLoader, resolveActionsHandler)
               );
   }

   /* (non-Javadoc)
    * @see com.iris.core.platform.AbstractPlatformMessageListener#onStart()
    */
   @Override
   protected void onStart() {
      super.onStart();
      addListeners(AddressMatchers.platformService(MessageConstants.SERVICE, SceneTemplateCapability.NAMESPACE));
   }

   /* (non-Javadoc)
    * @see com.iris.core.platform.AbstractPlatformMessageListener#handleRequestAndSendResponse(com.iris.messages.PlatformMessage)
    */
   @Override
   protected void handleRequestAndSendResponse(PlatformMessage message) {
      dispatcher.accept(message);
   }

   private SceneTemplate loadSceneTemplate(PlatformMessage message) {
      UUID placeId = UUID.fromString( message.getPlaceId() );
      String templateId = (String) message.getDestination().getId();
      SceneTemplate template = sceneTemplateManager.findByPlaceAndId(placeId, templateId);
      if(template == null) {
         throw new NotFoundException(message.getDestination());
      }
      return template;
   }
   
   private SceneTemplateEntity loadSceneTemplateEntity(PlatformMessage message) {
      UUID placeId = UUID.fromString( message.getPlaceId() );
      String templateId = (String) message.getDestination().getId();
      SceneTemplateEntity template = sceneTemplateManager.findEntityByPlaceAndId(placeId, templateId);
      if(template == null) {
         throw new NotFoundException(message.getDestination());
      }
      return template;
   }
   
   private PersistentModel loadSceneTemplateModel(PlatformMessage message) {
      SceneTemplateEntity template = loadSceneTemplateEntity(message);
      ModelEntity model = new ModelEntity( transformer.transform(template) );
      model.setCreated(template.getCreated());
      model.setModified(template.getModified());
      return model;
   }

}


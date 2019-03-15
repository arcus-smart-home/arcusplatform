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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.common.rule.action.ActionContext;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.SceneTemplateCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Model;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.model.serv.PlaceModel;
import com.iris.messages.type.ActionTemplate;
import com.iris.platform.rule.environment.ContextLoader;
import com.iris.platform.scene.catalog.SceneTemplate;
import com.iris.platform.scene.resolver.ActionResolver;

/**
 * 
 */
@Singleton
public class ResolveActionsRequestHandler implements ContextualRequestMessageHandler<SceneTemplate> {
   private ContextLoader loader;

   @Inject
   public ResolveActionsRequestHandler(
         ContextLoader factory
   ) {
      this.loader = factory;
   }

   /* (non-Javadoc)
    * @see com.iris.core.platform.PlatformRequestMessageHandler#getMessageType()
    */
   @Override
   public String getMessageType() {
      return SceneTemplateCapability.ResolveActionsRequest.NAME;
   }

   /* (non-Javadoc)
    * @see com.iris.core.platform.ContextualRequestMessageHandler#handleRequest(java.lang.Object, com.iris.messages.PlatformMessage)
    */
   @Override
   public MessageBody handleRequest(SceneTemplate template, PlatformMessage message) {
      MessageBody request = message.getValue();
      UUID placeId = UUID.fromString( SceneTemplateCapability.ResolveActionsRequest.getPlaceId(request) );
      Errors.assertPlaceMatches(message, placeId);
      
      ActionContext context = loader.load(placeId);
      Model placeModel = context.getModelByAddress(Address.platformService(placeId, PlaceCapability.NAMESPACE));
      String curServiceLevel = PlaceModel.getServiceLevel(placeModel);
      boolean isPremium = ServiceLevel.isPremiumOrPromon(curServiceLevel);
      List<Map<String, Object>> actions = new ArrayList<>();
      for(ActionResolver resolver: template.getActions()) {
         ActionTemplate resolved = resolver.resolve(context);
         if(!Boolean.TRUE.equals(resolved.getPremium()) || isPremium) {
        	 actions.add(resolved.toMap());
         }else{
        	 resolved.setSatisfiable(false);
        	 actions.add(resolved.toMap());
         }
      }
      
      return 
            SceneTemplateCapability.ResolveActionsResponse
               .builder()
               .withActions(actions)
               .build();
   }

}


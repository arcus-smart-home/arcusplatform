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
package com.iris.platform.rule.environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.platform.AnalyticsMessageBus;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.platform.handlers.AddTagsModelRequestHandler;
import com.iris.core.platform.handlers.GetAttributesModelRequestHandler;
import com.iris.core.platform.handlers.RemoveTagsModelRequestHandler;
import com.iris.core.platform.handlers.SetAttributesModelRequestHandler;
import com.iris.platform.model.ModelEntity;
import com.iris.platform.rule.RuleEnvironment;
import com.iris.platform.rule.service.SceneCatalogLoader;
import com.iris.platform.scene.SceneDao;
import com.iris.platform.scene.SceneDefinition;

@Singleton
public class SceneHandlerFactory {
   private final PlatformMessageBus platformBus;
   private final AnalyticsMessageBus analyticsBus;
   private final SceneDao sceneDao;
   private final BeanAttributesTransformer<SceneDefinition> transformer;
   private final GetAttributesModelRequestHandler getAttributesHandler;
   private final SetAttributesModelRequestHandler setAttributesHandler;
   private final AddTagsModelRequestHandler addTagsHandler;
   private final RemoveTagsModelRequestHandler removeTagsHandler;
   private final SceneCatalogLoader sceneCatalogLoader;

   @Inject
   public SceneHandlerFactory(
         PlatformMessageBus platformBus,
         AnalyticsMessageBus analyticsBus,
         SceneDao sceneDao,
         BeanAttributesTransformer<SceneDefinition> transformer,
         GetAttributesModelRequestHandler getAttributesHandler,
         SceneSetAttributesHandler setAttributesHandler,
         AddTagsModelRequestHandler addTagsHandler,
         RemoveTagsModelRequestHandler removeTagsHandler,
         SceneCatalogLoader sceneCatalogLoader
   ) { 
      this.platformBus = platformBus;
      this.analyticsBus = analyticsBus;
      this.sceneDao = sceneDao;
      this.transformer = transformer;
      this.getAttributesHandler = getAttributesHandler;
      this.setAttributesHandler = setAttributesHandler;
      this.addTagsHandler = addTagsHandler;
      this.removeTagsHandler = removeTagsHandler;
      this.sceneCatalogLoader = sceneCatalogLoader;
   }

   public List<SceneHandler> create(RuleEnvironment environment, RuleModelStore models) {
      PlatformSceneContext.Builder builder =
            PlatformSceneContext
               .builder()
               .withModels(models)
               .withPlaceId(environment.getPlaceId())
               .withPlatformBus(platformBus)
               .withSceneDao(sceneDao)
               // TODO load proper timezone
               .withTimeZone(TimeZone.getDefault())
               ;
      
      List<SceneHandler> handlers = new ArrayList<>();
      for(SceneDefinition definition: environment.getScenes()) {
         Logger logger = LoggerFactory.getLogger("scene." + definition.getTemplate());
         builder.withLogger(logger);
         
         Map<String, Object> attributes = transformer.transform(definition);
         ModelEntity model = new ModelEntity(attributes);
         model.setCreated(definition.getCreated());
         model.setModified(definition.getModified());
         builder.withModel(model);
         
         try {
            // TODO replace this with assisted injection
            handlers.add(new SceneHandler(
                  builder.build(), 
                  getAttributesHandler,
                  setAttributesHandler,
                  addTagsHandler,
                  removeTagsHandler,
                  analyticsBus,
                  sceneCatalogLoader.getCatalogForPlace(environment.getPlaceId())
            ));
         }
         catch(Exception e) {
            logger.warn("Unable to deserialize scene [{}]", definition.getAddress(), e);
         }
      }
      return handlers;
   }

}


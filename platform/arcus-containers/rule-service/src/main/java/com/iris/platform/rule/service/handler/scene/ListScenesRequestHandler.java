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
package com.iris.platform.rule.service.handler.scene;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.inject.Inject;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.capability.attribute.transform.BeanListTransformer;
import com.iris.core.platform.PlatformRequestMessageHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.errors.Errors;
import com.iris.messages.service.SceneService;
import com.iris.messages.service.SceneService.ListScenesRequest;
import com.iris.messages.service.SceneService.ListScenesResponse;
import com.iris.platform.scene.SceneDao;
import com.iris.platform.scene.SceneDefinition;

/**
 * 
 */
public class ListScenesRequestHandler implements PlatformRequestMessageHandler {
   private final SceneDao sceneDao;
   private final BeanListTransformer<SceneDefinition> sceneTransformer;

   /**
    * 
    */
   @Inject
   public ListScenesRequestHandler(
         SceneDao sceneDao,
         BeanAttributesTransformer<SceneDefinition> transformer
   ) {
      this.sceneDao = sceneDao;
      this.sceneTransformer = new BeanListTransformer<>(transformer);
   }

   @Override
   public String getMessageType() {
      return SceneService.ListScenesRequest.NAME;
   }

   @Override
   public MessageBody handleMessage(PlatformMessage message) throws Exception {
      String placeId = ListScenesRequest.getPlaceId(message.getValue());
      Errors.assertRequiredParam(placeId, ListScenesRequest.ATTR_PLACEID);
      Errors.assertPlaceMatches(message, placeId);
      
      List<SceneDefinition> scenes = sceneDao.listByPlace(UUID.fromString(placeId));
      List<Map<String, Object>> models = sceneTransformer.convertListToAttributes(scenes);
      return
            ListScenesResponse
               .builder()
               .withScenes(models)
               .build();
   }

}


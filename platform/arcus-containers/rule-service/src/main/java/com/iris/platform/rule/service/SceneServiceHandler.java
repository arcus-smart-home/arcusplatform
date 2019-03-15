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

import java.util.concurrent.Executor;
import java.util.function.Consumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.platform.AbstractPlatformService;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.platform.RequestHandlers;
import com.iris.messages.PlatformMessage;
import com.iris.messages.service.SceneService;
import com.iris.platform.rule.service.handler.scene.ListSceneTemplateRequestHandler;
import com.iris.platform.rule.service.handler.scene.ListScenesRequestHandler;

/**
 * 
 */
@Singleton
public class SceneServiceHandler extends AbstractPlatformService {
   public static final String PROP_THREADPOOL = "service.scene.threadpool";
   
   private final Consumer<PlatformMessage> dispatcher;

   @Inject
   public SceneServiceHandler(
         @Named(PROP_THREADPOOL) Executor executor,
         PlatformMessageBus platformBus,
         ListScenesRequestHandler listScenes,
         ListSceneTemplateRequestHandler listSceneTemplates
   ) {
      super(platformBus, SceneService.NAMESPACE, executor);
      this.dispatcher = RequestHandlers.toDispatcher(platformBus, listScenes, listSceneTemplates);
   }

   /* (non-Javadoc)
    * @see com.iris.core.platform.AbstractPlatformMessageListener#onStart()
    */
   @Override
   protected void onStart() {
      super.onStart();
      addListeners(getAddress());
   }

   /* (non-Javadoc)
    * @see com.iris.core.platform.AbstractPlatformMessageListener#handleRequestAndSendResponse(com.iris.messages.PlatformMessage)
    */
   @Override
   protected void handleRequestAndSendResponse(PlatformMessage message) {
      this.dispatcher.accept(message);
   }


}


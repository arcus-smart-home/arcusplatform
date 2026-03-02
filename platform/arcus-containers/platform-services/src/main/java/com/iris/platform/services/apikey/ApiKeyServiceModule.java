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
package com.iris.platform.services.apikey;

import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.messages.model.Place;
import com.iris.platform.services.apikey.handlers.CreateApiKeyHandler;
import com.iris.platform.services.apikey.handlers.DeleteApiKeyHandler;
import com.iris.platform.services.apikey.handlers.ListApiKeysHandler;
import com.iris.platform.services.apikey.handlers.RevokeApiKeyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiKeyServiceModule extends AbstractIrisModule {

   private static final Logger logger = LoggerFactory.getLogger(ApiKeyServiceModule.class);

   @Inject(optional = true) @Named("apikey.service.enabled")
   private boolean enabled = true;

   @Override
   protected void configure() {
      if (!enabled) {
         logger.info("API key service is disabled");
         return;
      }
      Multibinder<ContextualRequestMessageHandler<Place>> handlerBinder =
            bindSetOf(new TypeLiteral<ContextualRequestMessageHandler<Place>>() {});
      handlerBinder.addBinding().to(CreateApiKeyHandler.class);
      handlerBinder.addBinding().to(ListApiKeysHandler.class);
      handlerBinder.addBinding().to(RevokeApiKeyHandler.class);
      handlerBinder.addBinding().to(DeleteApiKeyHandler.class);
   }
}

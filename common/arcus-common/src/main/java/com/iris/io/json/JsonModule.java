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
package com.iris.io.json;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.iris.bootstrap.guice.AbstractIrisModule;

/**
 * Guice integration for JSON.
 * 
 * This is a stub for configuring the JSON object, another module
 * which provides implementations of JsonSerializer and JsonDeserializer
 * should be registered and include this module for publishing the interfaces.
 */
public class JsonModule extends AbstractIrisModule {
   private static final Logger logger = LoggerFactory.getLogger(JsonModule.class);

   @Override
   protected void configure() {
      bind(JsonConfigurer.class).asEagerSingleton();
   }

   private static class JsonConfigurer {
      @Inject
      JsonConfigurer(JsonSerializer serializer, JsonDeserializer deserializer) {
         logger.info("Configuring json serialization...");
         JSON.setSerializer(serializer);
         JSON.setDeserializer(deserializer);
      }
   }
}


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
package com.iris.resource.config;

import java.util.Set;

import javax.annotation.PostConstruct;

import com.google.inject.Inject;
import com.iris.resource.ResourceFactory;
import com.iris.resource.Resources;

/**
 * An injection based handler to allow ResourceFactory's to
 * be added.
 */
public class ResourceConfigurer {
   private final ResourceFactory defaultFactory;
   private final Set<ResourceFactory> factories;
   
   @Inject
   ResourceConfigurer(ResourceFactory defaultFactory, Set<ResourceFactory> factories) {
      this.defaultFactory = defaultFactory;
      this.factories = factories;
   }
   
   @PostConstruct
   public void init() {
      Resources.registerDefaultFactory(defaultFactory);
      for(ResourceFactory factory: factories) {
         Resources.registerFactory(factory);
      }
   }
}


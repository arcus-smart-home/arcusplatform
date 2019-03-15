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
package com.iris.platform.rule.catalog;

import java.util.Map;

import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.io.Deserializer;
import com.iris.platform.rule.RuleConfig;
import com.iris.platform.rule.catalog.serializer.RuleCatalogDeserializer;
import com.iris.resource.Resource;
import com.iris.resource.Resources;

/**
 *
 */
public class RuleCatalogModule extends AbstractIrisModule {



   @Override
   protected void configure() {
      bind(RuleCatalogManager.class);
      bind(new Key<Deserializer<Map<String,RuleCatalog>>>() {})
         .to(RuleCatalogDeserializer.class);
   }

   @Provides @Named("rule.catalog.resource")
   public Resource ruleCatalogResource(RuleConfig config) {
      return Resources.getResource(config.getCatalogPath());
   }

}


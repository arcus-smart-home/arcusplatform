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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.io.Deserializer;
import com.iris.messages.type.Population;
import com.iris.resource.Resource;
import com.netflix.governator.annotations.WarmUp;

/**
 *
 */
@Singleton
public class RuleCatalogManager {
   private static final Logger logger = LoggerFactory.getLogger(RuleCatalogManager.class);

   private final Resource catalogResource;
   private final Deserializer<Map<String,RuleCatalog>> deserializer;
   private final AtomicReference<Map<String,RuleCatalog>> catalogRef = new AtomicReference<>();

   @Inject
   public RuleCatalogManager(
         @Named("rule.catalog.resource") Resource catalogResource,
         Deserializer<Map<String,RuleCatalog>> deserializer
   ) {
      this.catalogResource = catalogResource;
      this.deserializer = deserializer;
   }

   @WarmUp
   public void init() {
      if(this.catalogResource.isWatchable()) {
         this.catalogResource.addWatch(() -> this.loadCatalog());
      }
      this.loadCatalog();
   }

   public RuleCatalog getCatalog(String population) {
      if(catalogRef.get() == null) {
         loadCatalog();
      }
      if(StringUtils.isEmpty(population)) {
      	population = Population.NAME_GENERAL;
      }
      return catalogRef.get().get(population);
   }

   // synchronized b/c we don't want a slow load to finish and replace the
   // file after a newer one has run
   private synchronized void loadCatalog() {
      logger.info("Reloading rule catalog [{}]", catalogResource);
      try(InputStream is = catalogResource.open()) {
         Map<String,RuleCatalog> catalogs = deserializer.deserialize(is);
         catalogRef.set(catalogs);
      }
      catch(Exception e) {
         logger.warn("Unable to load rule catalog from [{}]", catalogResource, e);
      }
   }



}


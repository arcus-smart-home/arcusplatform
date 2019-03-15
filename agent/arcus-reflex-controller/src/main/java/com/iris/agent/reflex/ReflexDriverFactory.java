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
package com.iris.agent.reflex;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.iris.driver.reflex.ReflexDriver;
import com.iris.model.Version;

public class ReflexDriverFactory {
   private static final Logger log = LoggerFactory.getLogger(ReflexDriverFactory.class);

   private static final Cache<String,ReflexDriver> driverCache = CacheBuilder.newBuilder()
      .weakValues()
      .build();

   public static @Nullable ReflexDriver create(String driver, Version version, com.iris.driver.reflex.ReflexDriverDefinition drv) {
      String key = driver  + "-" + version.getRepresentation() + "-" + drv.getHash();
      try {
         return driverCache.get(key, () -> ReflexDriver.create(driver, version, drv.getHash(), drv.getCapabilities(), drv.getReflexes(), drv.getDfa()));
      } catch (Exception ex) {
         log.warn("could not create reflex driver for {} {}:", driver, version, ex);
         return null;
      }
   }
}


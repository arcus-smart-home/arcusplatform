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
package com.iris.driver.registry;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.iris.driver.DeviceDriver;
import com.iris.driver.groovy.GroovyDriverFactory;
import com.iris.driver.service.DriverConfig;
import com.iris.driver.service.registry.DriverScriptInfo;
import com.iris.driver.service.registry.FilesystemDriverRegistry;
import com.iris.messages.model.DriverId;
import com.iris.validators.ValidationException;
import com.netflix.governator.annotations.WarmUp;

public class GroovyDriverRegistry extends FilesystemDriverRegistry {
   private final static Logger logger = LoggerFactory.getLogger(GroovyDriverRegistry.class);
   private final DriverConfig driverConfig;
   private GroovyDriverFactory factory;
   private Pattern filterPattern = null;

   @Inject
   public GroovyDriverRegistry(DriverConfig driverConfig, GroovyDriverFactory factory) {
      this.driverConfig = driverConfig;
      this.factory = factory;
   }

   @WarmUp
   public void start() {
	   if(StringUtils.isNotBlank(driverConfig.getDriverFilterPattern())) {
		   filterPattern = Pattern.compile(driverConfig.getDriverFilterPattern());
	   }
      load();
   }
   
   @Override
   protected String getDirectoryPath() {
      return driverConfig.evaluateAbsoluteDriverDirectory();
   }

   @Override
   protected Map<DriverId, DeviceDriver> getScriptedDrivers(File driverDir, List<DriverScriptInfo> driversInfo) {
      Map<DriverId, DeviceDriver> newDrivers = new HashMap<>();
      File[] files = driverDir.listFiles();

      long loaded = 0;
      long total = 0;
      long start = System.nanoTime();
      for (File driver : files) {
         if (driver.isFile() && driver.canRead() && (filterPattern == null || filterPattern.matcher(driver.getName()).matches())) {
            total++;
            try {
               DeviceDriver deviceDriver = factory.load(driver.getName());
               newDrivers.put(deviceDriver.getDriverId(), deviceDriver);
               driversInfo.add(new DriverScriptInfo(driver.getName(), deviceDriver.getDriverId()));
               logger.debug("Loaded driver [{}] from driver directory", deviceDriver.getDriverId().getName());
               loaded++;
            } catch (ValidationException e) {
               driversInfo.add(new DriverScriptInfo(driver.getName(), e));
               if (logger.isDebugEnabled()) {
                  logger.error("Driver [{}] failed to validate [{}]", driver.getName(), e.getMessage(), e);
               } else {
                  logger.error("Driver [{}] failed to validate [{}]", driver.getName(), e.getMessage());
               }
            }
         }
      }
      long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
      logger.info("loaded {} of {} drivers in {}ms", loaded, total, elapsed);

      return newDrivers;
   }
}


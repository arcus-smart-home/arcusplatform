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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.google.inject.Inject;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.DeviceDriver;
import com.iris.driver.LightweightDeviceDriver;
import com.iris.driver.groovy.GroovyDriverFactory;
import com.iris.driver.service.DriverConfig;
import com.iris.driver.service.registry.DriverScriptInfo;
import com.iris.driver.service.registry.FilesystemDriverRegistry;
import com.iris.messages.model.DriverId;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.validators.ValidationException;
import com.iris.bootstrap.annotations.WarmUp;

public class GroovyDriverRegistry extends FilesystemDriverRegistry {
   private final static Logger logger = LoggerFactory.getLogger(GroovyDriverRegistry.class);
   private final DriverConfig driverConfig;
   private GroovyDriverFactory factory;
   private Pattern filterPattern = null;

   // Lazy loading state
   private final ConcurrentHashMap<DriverId, DeviceDriver> compiledDriverCache = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<DriverId, String> driverSources = new ConcurrentHashMap<>();
   private final Counter lazyCompileCount;

   @Inject
   public GroovyDriverRegistry(DriverConfig driverConfig, GroovyDriverFactory factory) {
      this.driverConfig = driverConfig;
      this.factory = factory;

      IrisMetricSet metrics = IrisMetrics.metrics("drivers.registry");
      metrics.gauge("registered", (Gauge<Integer>) () -> {
         Collection<DeviceDriver> drivers = listDrivers();
         return drivers != null ? drivers.size() : 0;
      });
      metrics.gauge("compiled", (Gauge<Integer>) () -> {
         if(!driverConfig.isLazyLoading()) {
            Collection<DeviceDriver> drivers = listDrivers();
            return drivers != null ? drivers.size() : 0;
         }
         return compiledDriverCache.size();
      });
      metrics.gauge("lightweight", (Gauge<Integer>) () -> {
         if(!driverConfig.isLazyLoading()) {
            return 0;
         }
         Collection<DeviceDriver> drivers = listDrivers();
         int total = drivers != null ? drivers.size() : 0;
         return total - compiledDriverCache.size();
      });
      lazyCompileCount = metrics.counter("lazy.compiles");
   }

   @WarmUp
   public void start() {
	   if(StringUtils.isNotBlank(driverConfig.getDriverFilterPattern())) {
		   filterPattern = Pattern.compile(driverConfig.getDriverFilterPattern());
	   }
      load();
   }

   @Override
   protected void invalidate() {
      compiledDriverCache.clear();
      super.invalidate();
   }

   @Override
   protected String getDirectoryPath() {
      return driverConfig.evaluateAbsoluteDriverDirectory();
   }

   @Override
   public DeviceDriver loadDriverById(DriverId driverId) {
      if(!driverConfig.isLazyLoading()) {
         return super.loadDriverById(driverId);
      }

      if(driverId == null) {
         return null;
      }

      // Check compiled cache first
      DeviceDriver compiled = compiledDriverCache.get(driverId);
      if(compiled != null) {
         return compiled;
      }

      // Fall back to the DriverMap (may be lightweight)
      DeviceDriver driver = super.loadDriverById(driverId);
      if(driver == null) {
         return null;
      }

      // If it's already a full driver, return it
      if(!(driver instanceof LightweightDeviceDriver)) {
         return driver;
      }

      // Compile on demand
      return compileAndCache(driverId);
   }

   @Override
   public DeviceDriver findDriverFor(String population, AttributeMap attributes, Integer maxReflexVersion) {
      DeviceDriver driver = super.findDriverFor(population, attributes, maxReflexVersion);
      if(driverConfig.isLazyLoading() && driver instanceof LightweightDeviceDriver) {
         DeviceDriver compiled = compileAndCache(driver.getDriverId());
         if(compiled != null) {
            return compiled;
         }
      }
      return driver;
   }

   @Override
   public DeviceDriver loadDriverByName(String population, String driverName, Integer maxReflexVersion) {
      DeviceDriver driver = super.loadDriverByName(population, driverName, maxReflexVersion);
      if(driverConfig.isLazyLoading() && driver instanceof LightweightDeviceDriver) {
         DeviceDriver compiled = compileAndCache(driver.getDriverId());
         if(compiled != null) {
            return compiled;
         }
      }
      return driver;
   }

   private DeviceDriver compileAndCache(DriverId driverId) {
      return compiledDriverCache.computeIfAbsent(driverId, id -> {
         String source = driverSources.get(id);
         if(source == null) {
            logger.warn("No source file recorded for driver [{}], cannot compile on demand", id);
            return null;
         }
         try {
            long start = System.nanoTime();
            DeviceDriver fullDriver = factory.load(source);
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            logger.info("Lazy-compiled driver [{}] from [{}] in {}ms", id, source, elapsed);
            lazyCompileCount.inc();
            return fullDriver;
         } catch(ValidationException e) {
            logger.error("Failed to lazy-compile driver [{}]: {}", id, e.getMessage(), e);
            return null;
         }
      });
   }

   @Override
   protected Map<DriverId, DeviceDriver> getScriptedDrivers(File driverDir, List<DriverScriptInfo> driversInfo) {
      if(driverConfig.isLazyLoading()) {
         return getScriptedDriversLazy(driverDir, driversInfo);
      }
      return getScriptedDriversEager(driverDir, driversInfo);
   }

   private Map<DriverId, DeviceDriver> getScriptedDriversEager(File driverDir, List<DriverScriptInfo> driversInfo) {
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

   private Map<DriverId, DeviceDriver> getScriptedDriversLazy(File driverDir, List<DriverScriptInfo> driversInfo) {
      Map<DriverId, DeviceDriver> newDrivers = new HashMap<>();
      Map<DriverId, String> newSources = new HashMap<>();
      File[] files = driverDir.listFiles();

      long loaded = 0;
      long total = 0;
      long start = System.nanoTime();

      for (File driver : files) {
         if (driver.isFile() && driver.canRead() && (filterPattern == null || filterPattern.matcher(driver.getName()).matches())) {
            total++;
            try {
               LightweightDeviceDriver lightweight = factory.loadLightweight(driver.getName());
               newDrivers.put(lightweight.getDriverId(), lightweight);
               newSources.put(lightweight.getDriverId(), driver.getName());
               driversInfo.add(new DriverScriptInfo(driver.getName(), lightweight.getDriverId()));
               logger.debug("Loaded lightweight driver [{}] from driver directory", lightweight.getDriverId().getName());
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

      // Clear and rebuild source mappings
      driverSources.clear();
      driverSources.putAll(newSources);
      compiledDriverCache.clear();

      long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
      logger.info("loaded {} of {} drivers as lightweight in {}ms", loaded, total, elapsed);

      return newDrivers;
   }
}

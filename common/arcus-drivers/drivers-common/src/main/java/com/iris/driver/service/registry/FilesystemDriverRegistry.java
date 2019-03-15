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
package com.iris.driver.service.registry;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.driver.DeviceDriver;
import com.iris.messages.model.DriverId;
import com.netflix.governator.annotations.WarmUp;

public abstract class FilesystemDriverRegistry extends AbstractSingleDriverRegistry {
   private static final Logger logger = LoggerFactory.getLogger(FilesystemDriverRegistry.class);

   private DriverWatcher watcher;

   public FilesystemDriverRegistry() {
   }

   @WarmUp
   public void warmUp() {
      watcher = new DriverWatcher(getDirectoryPath());
      watcher.addListener(new DriverWatcherListener() {
         @Override
         public void onChange() {
            invalidate();
         }
      });
      watcher.watch();
   }

   @PreDestroy
   public void shutdown() {
      watcher.shutdown();
   }

   protected abstract String getDirectoryPath();

   protected abstract Map<DriverId, DeviceDriver> getScriptedDrivers(File driverDir, List<DriverScriptInfo> driversInfo);

   @Override
   protected Collection<DeviceDriver> loadDrivers() {
      logger.debug("Reloading Scripted Drivers");
      List<DriverScriptInfo> driversInfo = new ArrayList<>();
      File driverDir = new File(getDirectoryPath());
      Collection<DeviceDriver> drivers = null;
      if (driverDir.exists() && driverDir.isDirectory()) {
         drivers = getScriptedDrivers(driverDir, driversInfo).values();
      }
      else {
         logger.warn("Invalid driver directory: {}", driverDir);
      }
      logger.debug("Finished reloading Scripted Drivers");
      fireOnDriversLoaded(new DriversLoadedEvent(driversInfo));
      return drivers != null ? drivers : Collections.<DeviceDriver>emptyList();
   }

}


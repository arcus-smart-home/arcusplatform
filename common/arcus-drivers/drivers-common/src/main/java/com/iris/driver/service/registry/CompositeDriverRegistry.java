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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.iris.device.attributes.AttributeMap;
import com.iris.driver.DeviceDriver;
import com.iris.messages.model.DriverId;
import com.iris.model.Version;

public class CompositeDriverRegistry extends AbstractDriverRegistry {
   private final List<DriverRegistry> registries;

   public CompositeDriverRegistry(DriverRegistry... registries) {
      this.registries = Arrays.asList(registries);
   }

   protected List<DriverRegistry> getRegistries() {
      return registries;
   }

   @Override
   public Collection<DeviceDriver> listDrivers() {
      List<DriverRegistry> registries = getRegistries();
      if(registries.isEmpty()) {
         return Collections.emptyList();
      }
      if(registries.size() == 1) {
         return registries.get(0).listDrivers();
      }
      // just in case the same driver is in two registries....
      Set<DeviceDriver> drivers = new HashSet<>();
      for(DriverRegistry registry: registries) {
         drivers.addAll(registry.listDrivers());
      }
      return drivers;
   }

   @Override
   public DeviceDriver findDriverFor(String population, AttributeMap attributes, Integer maxReflexVersion) {
      List<DriverRegistry> registries = getRegistries();
      List<DeviceDriver> drivers = new ArrayList<>();
      for (DriverRegistry registry : registries) {
         DeviceDriver driver = registry.findDriverFor(population, attributes, maxReflexVersion);
         if (driver != null) {
            drivers.add(driver);
         }
      }
      if (drivers.size() == 0) {
         return null;
      }
      if (drivers.size() == 1) {
         return drivers.get(0);
      }
      DeviceDriver driver = findBestDeviceDriver(drivers);
      return driver == null ? loadDriverById(FALLBACK_DRIVERID) : driver;
   }

   @Override
   public DeviceDriver loadDriverByName(String population, String driverName, Integer maxReflexVersion) {
      List<DriverRegistry> registries = getRegistries();
      List<DeviceDriver> drivers = new ArrayList<>();
      for (DriverRegistry registry : registries) {
         DeviceDriver driver = registry.loadDriverByName(population, driverName, maxReflexVersion);
         if (driver != null) {
            drivers.add(driver);
         }
      }
      if (drivers.size() == 0) {
         return null;
      }
      if (drivers.size() == 1) {
         return drivers.get(0);
      }
      return findBestDeviceDriver(drivers);
   }

   @Override
   public DeviceDriver loadDriverById(DriverId driverId) {
      if(driverId == null) {
         return null;
      }
      List<DriverRegistry> registries = getRegistries();
      for (DriverRegistry registry : registries) {
         DeviceDriver driver = registry.loadDriverById(driverId);
         if (driver != null) {
            return driver;
         }
      }
      return null;
   }

   @Override
   public DeviceDriver loadDriverById(String driverName, Version version) {
      return loadDriverById(new DriverId(driverName, version));
   }

   private DeviceDriver findBestDeviceDriver(List<DeviceDriver> drivers) {
      DeviceDriver bestDriver = null;
      for (DeviceDriver driver : drivers) {
         if (bestDriver == null || driver.getDriverId().getVersion().compareTo(bestDriver.getDriverId().getVersion()) < 0) {
            bestDriver = driver;
         }
      }
      return bestDriver;
   }

}


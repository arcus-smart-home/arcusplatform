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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.device.attributes.AttributeMap;
import com.iris.driver.DeviceDriver;
import com.iris.driver.reflex.ReflexRunMode;
import com.iris.driver.service.matcher.DiscoveryAlgorithm;
import com.iris.driver.service.matcher.DiscoveryAlgorithmFactory;
import com.iris.driver.service.matcher.SortedDiscoveryAlgorithmFactory;
import com.iris.messages.model.DriverId;
import com.iris.model.Version;

public abstract class AbstractSingleDriverRegistry extends AbstractDriverRegistry {
   private static final Logger logger = LoggerFactory.getLogger(AbstractSingleDriverRegistry.class);

   private final AtomicReference<DriverMap> driverRef =
         new AtomicReference<>();
   // TODO inject this
   private final DiscoveryAlgorithmFactory selectorFactory =
         new SortedDiscoveryAlgorithmFactory();

   public AbstractSingleDriverRegistry() {

   }

   public AbstractSingleDriverRegistry(Collection<DeviceDriver> drivers) {
      update(drivers);
   }

   private DriverMap getDrivers() {
      DriverMap drivers = driverRef.get();
      if(drivers != null) {
         return drivers;
      }

      // only do one re-build at a time
      synchronized(driverRef) {
         drivers = driverRef.get();
         if(drivers == null) {
            drivers = rebuild(loadDrivers());
            driverRef.set(drivers);
         }
      }
      return drivers;
   }

   private DriverMap rebuild(Collection<DeviceDriver> drivers) {
      if(drivers == null || drivers.isEmpty()) {
         return new DriverMap(
               Collections.<DriverId,DeviceDriver>emptyMap(),
               new TreeSet<DriverId>(),
               selectorFactory.create(Collections.<DeviceDriver>emptyList())
         );
      }
      else {
         Map<DriverId, DeviceDriver> result = new HashMap<>(drivers.size());
         NavigableSet<DriverId> ids = new TreeSet<>();
         for(DeviceDriver driver: drivers) {
            DriverId id = driver.getDriverId();
            result.put(id, driver);
            if(!id.getVersion().hasQualifier()) {
               ids.add(id);
            }
         }
         DiscoveryAlgorithm algorithm = selectorFactory.create(drivers);
         return new DriverMap(result, ids, algorithm);
      }
   }
   
   protected void load() {
      getDrivers();
   }

   protected void invalidate() {
      driverRef.set(null);
      fireOnInvalidated();
   }

   protected abstract Collection<DeviceDriver> loadDrivers();

   // TODO should this be removed? just use invalidate / loadDrivers?
   protected final void update(Collection<DeviceDriver> drivers) {
      driverRef.set(rebuild(drivers));
   }

   @Override
   public DeviceDriver findDriverFor(String population, AttributeMap attributes, Integer maxReflexVersion) {
      DeviceDriver driver = getDrivers().getByAttributes(population, attributes, maxReflexVersion);
      return driver == null ? loadDriverById(FALLBACK_DRIVERID) : driver;
    }

   @Override
   public DeviceDriver loadDriverByName(String population, String driverName, Integer maxReflexVersion) {
      if(driverName == null) {
         return null;
      }

      DeviceDriver drv = getDrivers().getByName(population, driverName, maxReflexVersion);
      if (validateMaxReflexVersion(drv, maxReflexVersion)) {
         return null;
      }

      return drv;
   }

   @Override
   public DeviceDriver loadDriverById(DriverId driverId) {
      if(driverId == null) {
         return null;
      }

      return getDrivers().getById(driverId);
   }

   @Override
   public DeviceDriver loadDriverById(String driverName, Version version) {
      return loadDriverById(new DriverId(driverName, version));
   }

   @Override
   public Collection<DeviceDriver> listDrivers() {
      return getDrivers().values();
   }

   @Override
   public String toString() {
      DriverMap drivers = driverRef.get();
      if(drivers == null) {
         return getClass().getSimpleName() + " [drivers=<not loaded>]";
      }
      else {
         return getClass().getSimpleName() + " [drivers=" + drivers.drivers.keySet() + "]";
      }
   }

   private static boolean validateMaxReflexVersion(DeviceDriver drv, Integer maxReflexVersion) {
      return drv != null && drv.getDefinition().getMinimumRequiredReflexVersion() > getMaxReflexVersion(maxReflexVersion);
   }

   private static int getMaxReflexVersion(Integer maxReflexVersion) {
      return (maxReflexVersion == null) ? 0 : maxReflexVersion;
   }

   private static class DriverMap {
      private final Map<DriverId, DeviceDriver> drivers;
      private final NavigableSet<DriverId> ids;
      private final DiscoveryAlgorithm selector;

      DriverMap(Map<DriverId, DeviceDriver> drivers, NavigableSet<DriverId> ids, DiscoveryAlgorithm selector) {
         this.drivers = drivers;
         this.ids = ids;
         this.selector = selector;
      }

      public DeviceDriver getById(DriverId id) {
         return drivers.get(id);
      }

      public DeviceDriver getByName(String population, String name, Integer maxReflexVersion) {
         if(name == null) {
            return null;
         }

         DriverId minId = new DriverId(name, new Version(Integer.MAX_VALUE, Integer.MAX_VALUE));
         DriverId maxId = new DriverId(name, new Version(0, 0, ""));
         SortedSet<DriverId> available = ids.subSet(minId, true, maxId, true);
         for (DriverId id : available) {
            DeviceDriver drv = drivers.get(id);
            if (drv == null) {
               // driver not found
               continue;
            }

            if (population != null && !drv.getDefinition().getPopulations().contains(population)) {
               // driver doesn't support the requested population
               continue;
            }

            if(drv.getDefinition().getReflexes().getMode() != ReflexRunMode.PLATFORM) {
               // only enforce reflex versions if this driver is allowed to actually run on the hub
               int reqReflexVersion = drv.getDefinition().getMinimumRequiredReflexVersion();
               if (drv.getDefinition().getReflexes().getMode() != ReflexRunMode.PLATFORM && reqReflexVersion > maxReflexVersion) {
                  // place doesn't support the required reflex version
                  continue;
               }
            }

            // Driver supports the required population and reflex version
            return drv;
         }

         return null;
      }

      public DeviceDriver getByAttributes(String population, AttributeMap attributes, Integer maxReflexVersion) {
         DriverId id = selector.discover(population, attributes, maxReflexVersion);
         if(id == null) {
            return null;
         }
         return getById(id);
      }

      public Collection<DeviceDriver> values() {
         return drivers.values();
      }
   }

}


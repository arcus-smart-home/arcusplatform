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
import java.util.List;

import com.iris.driver.DeviceDriver;
import com.iris.util.Subscription;

public abstract class AbstractDriverRegistry implements DriverRegistry {
   private final List<DriverRegistryListener> listeners = new ArrayList<>();

   @Override
   public Subscription addRegistryListener(final DriverRegistryListener registryListener) {
      listeners.add(registryListener);
      return new Subscription() {
         @Override
         public void remove() {
            listeners.remove(registryListener);
         }
      };
   }
   
   /* (non-Javadoc)
    * @see com.iris.driver.service.registry.DriverRegistry#getFallback()
    */
   @Override
   public DeviceDriver getFallback() {
      return loadDriverById(FALLBACK_DRIVERID);
   }

   protected void fireOnInvalidated() {
      for (DriverRegistryListener listener : listeners) {
         listener.onInvalidated();
      }
   }
   
   protected void fireOnDriversLoaded(DriversLoadedEvent event) {
      for (DriverRegistryListener listener : listeners) {
         listener.onDriversLoaded(event);
      }
   }
}


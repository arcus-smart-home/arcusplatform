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
package com.iris.driver.service.registry;

import java.util.Collection;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.driver.DeviceDriver;
import com.iris.messages.model.DriverId;

/**
 * A simple registry implementation backed by a Map.
 */
@Singleton
public class MapDriverRegistry extends AbstractSingleDriverRegistry {

   @Inject
   public MapDriverRegistry(Map<DriverId, DeviceDriver> drivers) {
      super(drivers.values());
   }

   @Override
   protected Collection<DeviceDriver> loadDrivers() {
      throw new UnsupportedOperationException();
   }

}


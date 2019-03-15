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

import com.iris.device.attributes.AttributeMap;
import com.iris.driver.DeviceDriver;
import com.iris.messages.model.DriverId;
import com.iris.model.Version;
import com.iris.util.Subscription;


/**
 * Allows a driver to be loaded by name.
 */
public interface DriverRegistry {

   public static final DriverId FALLBACK_DRIVERID = new DriverId("Fallback", new Version(1));

   // TODO should this just be a stream or an Iterable?
   public Collection<DeviceDriver> listDrivers();

   /**
    * Driver used when the desired driver can't be loaded or
    * when no driver is applicable to the device.
    * @return
    */
   public DeviceDriver getFallback();
   
   /**
    * Finds the "best" driver that can support the given device.
    * Current definition of "best" is most highly versioned instance
    * of a driver.
    * @param device
    * @param attributes
    * @return
    */
   public DeviceDriver findDriverFor(String population, AttributeMap attributes, Integer maxReflexVersion);

	/**
	 * Loads the most recent version of the given driver.
	 * @param driverName
	 * 	The name of the driver to load.
	 * @return
	 * 	The given driver or {@code null} if no such driver exists.
	 */
	public DeviceDriver loadDriverByName(String population, String driverName, Integer maxReflexVersion);

	public DeviceDriver loadDriverById(String driverName, Version version);

	public DeviceDriver loadDriverById(DriverId driverId);

	public Subscription addRegistryListener(DriverRegistryListener registryListener);
}


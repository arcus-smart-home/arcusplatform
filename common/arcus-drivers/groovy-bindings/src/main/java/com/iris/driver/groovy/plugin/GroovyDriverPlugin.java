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
package com.iris.driver.groovy.plugin;

import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverBuilder;
import com.iris.driver.capability.Capability;
import com.iris.driver.groovy.DriverBinding;
import com.iris.driver.groovy.GroovyBuilder;
import com.iris.driver.groovy.GroovyContextObject;
import com.iris.driver.groovy.binding.CapabilityEnvironmentBinding;
import com.iris.driver.groovy.binding.EnvironmentBinding;

/**
 * Represents an object that may enhance the groovy drivers.  This gets three chances
 * to update the system.
 * 1) Before the script is loaded global environment variables may be added to the {@code bindings}
 * object in {@link #enhanceEnvironment(DriverBinding)}.
 * 2) After the script has been run, but before the handler chain has been built, the builder may
 * be modified in {@link #enhanceBuilder(DriverBinding, DeviceDriverBuilder)}.
 * 3) Finally after the driver has been created but before it has been exposed additional properties
 * may be added to the bindings in {@link #enhanceDriver(DriverBinding, DeviceDriver)}.  This is
 * generally used to expose {@link GroovyContextObject} aware objects, to the handler functions.
 */
public interface GroovyDriverPlugin {

   /**
    * Call for drivers and capabilities before the source is parsed. In most cases the logic will
    * be the same regardless of whether this is a Driver or a Capability script.
    * @param binding
    *    Either a {@link DriverBinding} for drivers or a {@link CapabilityEnvironmentBinding} for
    *    capabilities.
    */
   public void enhanceEnvironment(EnvironmentBinding binding);

   /**
    * Call after the driver script has been executed that enables the builder to be
    * enhanced before the final driver is created.
    * @param binding
    *    The binding for the driver or capability, any post-processing should be done here
    *    before the full driver / capability is built
    */
   public void postProcessEnvironment(EnvironmentBinding binding);

   /**
    * Only called for drivers. This is after the script has been run, properties added
    * at this point will only be available to event handler functions.
    * @param bindings
    * @param driver
    */
   public void enhanceDriver(DriverBinding bindings, DeviceDriver driver);

   /**
    * Only called for capabilities. This is after the script has been run, properties added
    * at this point will only be available to event handler functions.
    * @param bindings
    * @param driver
    */
   public void enhanceCapability(CapabilityEnvironmentBinding bindings, Capability capability);
}


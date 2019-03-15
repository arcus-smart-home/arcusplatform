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
package com.iris.driver.groovy;

import org.junit.Test;

import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.DeviceDriver;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.DevicePowerCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.util.IrisCollections;

/**
 *
 */
public class TestGroovyDriverAttributes extends GroovyDriverTestCase {

   /**
    * These are attributes populated from headers to generate the
    * constant values for DeviceBase and DeviceAdvanced capabilities.
    * @throws Exception
    */
   @Test
   public void testHeaderAttributes() throws Exception {
      DeviceDriver driver = factory.load("Metadata.driver");
      AttributeMap attributes = driver.getBaseAttributes();
      assertEquals("Iris", attributes.get(DeviceCapability.KEY_VENDOR));
      assertEquals("nifty-001", attributes.get(DeviceCapability.KEY_MODEL));
      assertEquals("switch", attributes.get(DeviceCapability.KEY_DEVTYPEHINT));
      assertEquals(
            IrisCollections.setOf(DeviceCapability.NAMESPACE, DeviceAdvancedCapability.NAMESPACE, DeviceConnectionCapability.NAMESPACE, DevicePowerCapability.NAMESPACE, SwitchCapability.NAMESPACE, Capability.NAMESPACE),
            attributes.get(Capability.KEY_CAPS)
      );
      assertEquals("Iris Nifty Switch", attributes.get(DeviceAdvancedCapability.KEY_DRIVERNAME));
      assertEquals("1.0", attributes.get(DeviceAdvancedCapability.KEY_DRIVERVERSION));
      assertEquals("Z-Wave", attributes.get(DeviceAdvancedCapability.KEY_PROTOCOL));
   }

   @Test
   public void testCapabilityConfiguredViaStrings() throws Exception {
      DeviceDriver driver = factory.load("CapabilityStrings.driver");
      AttributeMap attributes = driver.getBaseAttributes();

      assertTrue(DevicePowerCapability.SOURCE_BATTERY.equalsIgnoreCase(attributes.get(DevicePowerCapability.KEY_SOURCE)));
      assertEquals(false, attributes.get(DevicePowerCapability.KEY_LINECAPABLE));
      assertEquals("deadbolt", attributes.get(AttributeKey.create("doorlock:type", String.class)));
   }

   @Test
   public void testCapabilityConfiguredViaObjects() throws Exception {
      DeviceDriver driver = factory.load("CapabilityObjects.driver");
      AttributeMap attributes = driver.getBaseAttributes();

      assertEquals(DevicePowerCapability.SOURCE_BATTERY, attributes.get(DevicePowerCapability.KEY_SOURCE));
      assertEquals(false, attributes.get(DevicePowerCapability.KEY_LINECAPABLE));
      assertEquals("deadbolt", attributes.get(AttributeKey.create("doorlock:type", String.class)));
   }

   // TODO more error case testing...
}


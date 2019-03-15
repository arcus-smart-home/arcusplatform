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
package com.iris.protocol.ipcd;

import static com.iris.Utils.assertTrue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;

import com.iris.protocol.ipcd.message.model.Device;

public class TestIpcdDeviceTypeRegistry {

   @Test
   public void TestGetV1DeviceTypeSwann() {
      List<Device> devices = IpcdDeviceTypeRegistry.INSTANCE.createDeviceForV1Type("Other", "123456");
      assertNotNull(devices);
      assertThat(devices.isEmpty(), is(false));
      assertThat(devices.size(), is(1));
      assertThat(devices.get(0).getVendor(), is("Swann"));
   }

   @Test
   public void TestGetV1DeviceTypeAoSmith() {
      List<Device> devices = IpcdDeviceTypeRegistry.INSTANCE.createDeviceForV1Type(IpcdProtocol.V1_DEVICE_TYPE_AOSMITH_WATER_HEATER, "123456");
      assertNotNull(devices);
      assertThat(devices.isEmpty(), is(false));
      assertThat(devices.size(), is(2));
      assertThat(devices.get(0).getVendor(), is("A.O. Smith"));
      boolean b1Found = false;
      boolean b2Found = false;
      for(Device d : devices) {
         if(d.getModel().equals("B1.00")) {
            b1Found = true;
         }
         if(d.getModel().equals("B2.00")) {
            b2Found = true;
         }
      }

      assertTrue(b1Found, "B1.00 should have been found.");
      assertTrue(b2Found, "B2.00 should have been found.");
      assertThat(devices.get(1).getVendor(), is("A.O. Smith"));
   }

   @Test
   public void TestGetV1DeviceTypeNotFound() {
      List<Device> devices = IpcdDeviceTypeRegistry.INSTANCE.createDeviceForV1Type("Jabberwocky", "123456");
      assertNotNull(devices);
      assertThat(devices.isEmpty(), is(true));
   }
}


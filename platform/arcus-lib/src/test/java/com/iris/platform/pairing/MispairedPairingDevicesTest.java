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
package com.iris.platform.pairing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.PairingDeviceCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.serv.PairingDeviceModel;

public class MispairedPairingDevicesTest {

   private static final String REAL_DRIVER = "ZWTestDriver";
   private static final ImmutableMap<String, String> PAIRING_FAILED =
         ImmutableMap.of(DeviceAdvancedCapability.PairingFailedException.CODE_PAIRING_FAILED, "Driver could not be determined.");

   @Test
   public void testStaleWhenDeviceRecovered() {
      // the error was removed, leaving an empty map
      assertTrue(MispairedPairingDevices.isStale(mispaired(), device(DeviceAdvancedCapability.DRIVERSTATE_ACTIVE, REAL_DRIVER, ImmutableMap.of())));
   }

   @Test
   public void testStaleWhenDeviceHasNoErrors() {
      assertTrue(MispairedPairingDevices.isStale(mispaired(), device(DeviceAdvancedCapability.DRIVERSTATE_ACTIVE, REAL_DRIVER, null)));
   }

   @Test
   public void testStaleWithLowerCaseDriverState() {
      // the devices table historically stores the driver state in lower case
      assertTrue(MispairedPairingDevices.isStale(mispaired(), device("active", REAL_DRIVER, ImmutableMap.of())));
   }

   @Test
   public void testStaleWithUnrelatedErrors() {
      assertTrue(MispairedPairingDevices.isStale(mispaired(), device(DeviceAdvancedCapability.DRIVERSTATE_ACTIVE, REAL_DRIVER, ImmutableMap.of("other.error", "Something else"))));
   }

   @Test
   public void testNotStaleWhilePairingFailed() {
      assertFalse(MispairedPairingDevices.isStale(mispaired(), device(DeviceAdvancedCapability.DRIVERSTATE_ACTIVE, REAL_DRIVER, PAIRING_FAILED)));
   }

   @Test
   public void testNotStaleWhenUnsupported() {
      assertFalse(MispairedPairingDevices.isStale(mispaired(), device(DeviceAdvancedCapability.DRIVERSTATE_UNSUPPORTED, REAL_DRIVER, null)));
   }

   @Test
   public void testNotStaleWhileProvisioning() {
      assertFalse(MispairedPairingDevices.isStale(mispaired(), device(DeviceAdvancedCapability.DRIVERSTATE_PROVISIONING, REAL_DRIVER, null)));
   }

   @Test
   public void testNotStaleWhenLost() {
      assertFalse(MispairedPairingDevices.isStale(mispaired(), device(DeviceAdvancedCapability.DRIVERSTATE_UNRECOVERABLE, REAL_DRIVER, null)));
   }

   @Test
   public void testNotStaleOnFallbackDriver() {
      assertFalse(MispairedPairingDevices.isStale(mispaired(), device(DeviceAdvancedCapability.DRIVERSTATE_ACTIVE, "_ZW_Fallback", null)));
   }

   @Test
   public void testNotStaleWithoutDriverName() {
      assertFalse(MispairedPairingDevices.isStale(mispaired(), device(DeviceAdvancedCapability.DRIVERSTATE_ACTIVE, null, null)));
   }

   @Test
   public void testNotStaleWithoutDevice() {
      assertFalse(MispairedPairingDevices.isStale(mispaired(), null));
   }

   @Test
   public void testNotStaleUnlessMispaired() {
      Model recovered = device(DeviceAdvancedCapability.DRIVERSTATE_ACTIVE, REAL_DRIVER, ImmutableMap.of());
      assertFalse(MispairedPairingDevices.isStale(pairingDevice(PairingDeviceCapability.PAIRINGSTATE_PAIRED, PairingDeviceCapability.PAIRINGPHASE_PAIRED), recovered));
      assertFalse(MispairedPairingDevices.isStale(pairingDevice(PairingDeviceCapability.PAIRINGSTATE_MISCONFIGURED, PairingDeviceCapability.PAIRINGPHASE_FAILED), recovered));
      assertFalse(MispairedPairingDevices.isStale(pairingDevice(PairingDeviceCapability.PAIRINGSTATE_PAIRING, PairingDeviceCapability.PAIRINGPHASE_CONFIGURE), recovered));
   }

   private PairingDevice mispaired() {
      return pairingDevice(PairingDeviceCapability.PAIRINGSTATE_MISPAIRED, PairingDeviceCapability.PAIRINGPHASE_FAILED);
   }

   private PairingDevice pairingDevice(String pairingState, String pairingPhase) {
      PairingDevice model = new PairingDevice();
      model.setId(UUID.randomUUID(), 1);
      PairingDeviceModel.setPairingState(model, pairingState);
      PairingDeviceModel.setPairingPhase(model, pairingPhase);
      return model;
   }

   private Model device(String driverState, String driverName, ImmutableMap<String, String> errors) {
      SimpleModel model = new SimpleModel();
      model.setAttribute(DeviceAdvancedCapability.ATTR_DRIVERSTATE, driverState);
      if(driverName != null) {
         model.setAttribute(DeviceAdvancedCapability.ATTR_DRIVERNAME, driverName);
      }
      if(errors != null) {
         model.setAttribute(DeviceAdvancedCapability.ATTR_ERRORS, errors);
      }
      return model;
   }
}

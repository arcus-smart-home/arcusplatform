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

import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.PairingDeviceCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.DeviceAdvancedModel;
import com.iris.messages.model.serv.PairingDeviceModel;

/**
 * Finds pairing devices left at [MISPAIRED, FAILED] after their device recovered.
 * <p>
 * A pairing device follows its device's {@code pairing.failed} error through value
 * changes. If the error was cleared while [MISPAIRED, FAILED] was still treated as
 * terminal, the pairing device ignored it, and the device has nothing left to send
 * that would move it on. It stays "improperly paired" for a working device.
 */
public final class MispairedPairingDevices {

   private MispairedPairingDevices() {
   }

   public static boolean isMispaired(Model pairingDevice) {
      return
            PairingDeviceCapability.PAIRINGPHASE_FAILED.equals(PairingDeviceModel.getPairingPhase(pairingDevice)) &&
            PairingDeviceCapability.PAIRINGSTATE_MISPAIRED.equals(PairingDeviceModel.getPairingState(pairingDevice));
   }

   /**
    * True when the device is active on a real driver without {@code pairing.failed}.
    * Only the fallback drivers set UNSUPPORTED or {@code pairing.failed}.
    */
   public static boolean hasRecovered(@Nullable Model device) {
      if(device == null) {
         return false;
      }
      if(!DeviceAdvancedCapability.DRIVERSTATE_ACTIVE.equalsIgnoreCase(DeviceAdvancedModel.getDriverstate(device))) {
         return false;
      }
      String driverName = DeviceAdvancedModel.getDrivername(device);
      if(driverName == null || driverName.contains("Fallback")) {
         return false;
      }
      Map<String, String> errors = DeviceAdvancedModel.getErrors(device);
      return errors == null || !errors.containsKey(DeviceAdvancedCapability.PairingFailedException.CODE_PAIRING_FAILED);
   }

   /**
    * True when the pairing device is [MISPAIRED, FAILED] but its device has recovered.
    */
   public static boolean isStale(Model pairingDevice, @Nullable Model device) {
      return isMispaired(pairingDevice) && hasRecovered(device);
   }
}

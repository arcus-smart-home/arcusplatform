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
package com.iris.driver.groovy.devicesettings;

import java.util.List;
import java.util.Map;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.devicesettings.DeviceSettingsContext;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.capability.DeviceSettingsCapability;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;
import com.iris.protocol.zwave.model.ZWaveNode;

/**
 * Shared utility methods for DeviceSettings handlers.
 */
class DeviceSettingsUtil {

   /**
    * Extracts the Z-Wave node from the device driver context.
    */
   static ZWaveNode extractNode(DeviceDriverContext context) {
      try {
         DeviceProtocolAddress address = (DeviceProtocolAddress) context.getProtocolAddress();
         if (address == null) {
            throw new IllegalStateException("Protocol address is not configured, can't send Z-Wave message");
         }
         ProtocolDeviceId deviceId = address.getProtocolDeviceId();
         return new ZWaveNode(deviceId.getBytes()[0]);
      } catch (ClassCastException | ArrayIndexOutOfBoundsException e) {
         throw new IllegalStateException("Protocol address [" + context.getProtocolAddress()
               + "] is not a Z-Wave address, can't send message", e);
      }
   }

   /**
    * Updates the devsettings:params attribute on the context.
    * The framework will emit a value change event on commit.
    */
   @SuppressWarnings("unchecked")
   static void emitParamsValueChange(DeviceDriverContext context, DeviceSettingsContext settingsContext) {
      List<Map<String, Object>> params = settingsContext.buildParamsAttribute(key -> {
         try {
            return context.getVariable(key);
         } catch (Exception e) {
            return null;
         }
      });
      context.setAttributeValue(
            (com.iris.device.attributes.AttributeKey) DeviceSettingsCapability.KEY_PARAMS,
            params);
   }

   /**
    * Builds a ZWaveCommand with only the specified send variables — matching
    * the Groovy DSL's approach of sending exactly the bytes needed, no trailing zeros.
    */
   static ZWaveCommand buildCommand(ZWaveCommandClass commandClass, String commandName, byte... sendVars) {
      ZWaveCommand template = commandClass.commandsByName.get(commandName);
      ZWaveCommand command = new ZWaveCommand();
      command.commandClass = commandClass.number;
      command.commandNumber = template.commandNumber;
      command.commandName = template.commandName;
      for (int i = 0; i < sendVars.length; i++) {
         String name = "send" + i;
         command.addSendVariable(name);
         command.setSend(name, sendVars[i]);
      }
      return command;
   }

   private DeviceSettingsUtil() {}
}

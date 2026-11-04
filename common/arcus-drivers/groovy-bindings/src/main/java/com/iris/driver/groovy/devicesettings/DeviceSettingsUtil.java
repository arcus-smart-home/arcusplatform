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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.device.attributes.AttributeMap;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.devicesettings.DeviceSettingsContext;
import com.iris.driver.devicesettings.DeviceSettingsParamDefinition;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.capability.DeviceSettingsCapability;
import com.iris.protocol.constants.ZwaveConstants;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;
import com.iris.protocol.zwave.model.ZWaveNode;

/**
 * Shared utility methods for DeviceSettings handlers.
 */
class DeviceSettingsUtil {
   private static final Logger log = LoggerFactory.getLogger(DeviceSettingsUtil.class);

   private static final ZWaveCommandClass CONFIGURATION_CC = ZWaveAllCommandClasses.getClass("configuration");

   /**
    * Narrows the driver's settings to the ones that exist on this device's product.
    * Only reads the device's protocol attributes when some parameter is product specific.
    */
   static DeviceSettingsContext forDevice(DeviceDriverContext context, DeviceSettingsContext settingsContext) {
      if (!settingsContext.hasProductFilters()) {
         return settingsContext;
      }
      Map<String, Object> attrs = null;
      try {
         AttributeMap protocolAttrs = context.getProtocolAttributes();
         attrs = protocolAttrs != null ? protocolAttrs.toMap() : null;
      } catch (Exception e) {
         log.warn("Could not read protocol attributes of {}; showing only settings common to every product",
               context.getDriverAddress(), e);
      }
      return settingsContext.forProduct(
            intAttr(attrs, ZwaveConstants.ATTR_PRODUCTTYPE),
            intAttr(attrs, ZwaveConstants.ATTR_PRODUCTID));
   }

   /**
    * Publishes the current values and asks the device for every parameter it has.
    */
   static void readAll(DeviceDriverContext context, DeviceSettingsContext settingsContext) {
      DeviceSettingsContext settings = forDevice(context, settingsContext);
      log.debug("Reading {} device settings params", settings.getParams().size());

      // Populate devsettings:params now so the attribute is visible before
      // config reports arrive from the device
      emitParamsValueChange(context, settingsContext);

      for (DeviceSettingsParamDefinition param : settings.getParams()) {
         sendConfigurationGet(context, param.getZwaveParamNo());
      }
   }

   static void sendConfigurationGet(DeviceDriverContext context, int paramNo) {
      ZWaveCommand command = buildCommand(CONFIGURATION_CC, "get", (byte) (paramNo & 0xFF));
      ZWaveCommandMessage message = new ZWaveCommandMessage();
      message.setDevice(extractNode(context));
      message.setCommand(command);
      context.sendToDevice(ZWaveProtocol.INSTANCE, message, -1);
   }

   private static Integer intAttr(Map<String, Object> attrs, String name) {
      Object value = attrs != null ? attrs.get(name) : null;
      return value instanceof Number ? ((Number) value).intValue() : null;
   }

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
      List<Map<String, Object>> params = forDevice(context, settingsContext).buildParamsAttribute(key -> {
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

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.devicesettings.DeviceSettingsContext;
import com.iris.driver.devicesettings.DeviceSettingsParamDefinition;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.DeviceSettingsCapability;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;

/**
 * Handles devsettings:SetParam requests.
 * Validates the value, sends Z-Wave configuration.set, then configuration.get for readback.
 */
public class DeviceSettingsSetParamHandler implements ContextualEventHandler<Object> {
   private static final Logger log = LoggerFactory.getLogger(DeviceSettingsSetParamHandler.class);

   private static final ZWaveCommandClass CONFIGURATION_CC = ZWaveAllCommandClasses.getClass("configuration");

   private final DeviceSettingsContext settingsContext;

   public DeviceSettingsSetParamHandler(DeviceSettingsContext settingsContext) {
      this.settingsContext = settingsContext;
   }

   @Override
   public boolean handleEvent(DeviceDriverContext context, Object event) throws Exception {
      MessageBody request;
      if (event instanceof PlatformMessage) {
         request = ((PlatformMessage) event).getValue();
      } else if (event instanceof MessageBody) {
         request = (MessageBody) event;
      } else {
         return false;
      }

      String paramId = DeviceSettingsCapability.SetParamRequest.getParamId(request);
      String value = DeviceSettingsCapability.SetParamRequest.getValue(request);

      DeviceSettingsParamDefinition param = settingsContext.getByParamId(paramId);
      if (param == null) {
         context.respondToPlatform(DeviceSettingsCapability.SetParamResponse.builder()
               .withStatus("INVALID_PARAM")
               .build());
         return true;
      }

      String validationError = param.validateValue(value);
      if (validationError != null) {
         log.debug("SetParam validation failed for {}: {}", paramId, validationError);
         context.respondToPlatform(DeviceSettingsCapability.SetParamResponse.builder()
               .withStatus("INVALID_VALUE")
               .build());
         return true;
      }

      long longValue = Long.parseLong(value);
      int paramNo = param.getZwaveParamNo();
      int paramSize = param.getZwaveParamSize();

      // Store pending value in driver variables
      context.setVariable(param.getVariableKey(), longValue);

      // Send Z-Wave configuration.set
      sendConfigurationSet(context, paramNo, paramSize, longValue);

      // Send Z-Wave configuration.get for readback
      sendConfigurationGet(context, paramNo);

      // Emit value change
      DeviceSettingsUtil.emitParamsValueChange(context, settingsContext);

      context.respondToPlatform(DeviceSettingsCapability.SetParamResponse.builder()
            .withStatus("PENDING")
            .build());
      return true;
   }

   private void sendConfigurationSet(DeviceDriverContext context, int paramNo, int paramSize, long value) {
      // Build payload: [paramNo, size, value bytes...] — matching the Groovy DSL's
      // ZWave.configuration.set() which sends only the exact bytes needed.
      byte[] sendVars = new byte[2 + paramSize];
      sendVars[0] = (byte) (paramNo & 0xFF);
      sendVars[1] = (byte) (paramSize & 0x07);
      for (int i = paramSize - 1; i >= 0; i--) {
         sendVars[2 + i] = (byte) (value & 0xFF);
         value >>= 8;
      }

      ZWaveCommand command = DeviceSettingsUtil.buildCommand(
            CONFIGURATION_CC, "set", sendVars);

      ZWaveCommandMessage message = new ZWaveCommandMessage();
      message.setDevice(DeviceSettingsUtil.extractNode(context));
      message.setCommand(command);
      context.sendToDevice(ZWaveProtocol.INSTANCE, message, -1);
   }

   private void sendConfigurationGet(DeviceDriverContext context, int paramNo) {
      ZWaveCommand command = DeviceSettingsUtil.buildCommand(
            CONFIGURATION_CC, "get", (byte) (paramNo & 0xFF));

      ZWaveCommandMessage message = new ZWaveCommandMessage();
      message.setDevice(DeviceSettingsUtil.extractNode(context));
      message.setCommand(command);
      context.sendToDevice(ZWaveProtocol.INSTANCE, message, -1);
   }
}

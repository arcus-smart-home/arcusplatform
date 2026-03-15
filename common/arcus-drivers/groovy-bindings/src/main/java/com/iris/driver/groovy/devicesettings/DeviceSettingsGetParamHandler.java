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
 * Handles devsettings:GetParam requests.
 * Issues a Z-Wave configuration.get and returns the last known value from driver variables.
 */
public class DeviceSettingsGetParamHandler implements ContextualEventHandler<Object> {
   private static final Logger log = LoggerFactory.getLogger(DeviceSettingsGetParamHandler.class);

   private static final ZWaveCommandClass CONFIGURATION_CC = ZWaveAllCommandClasses.getClass("configuration");

   private final DeviceSettingsContext settingsContext;

   public DeviceSettingsGetParamHandler(DeviceSettingsContext settingsContext) {
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

      String paramId = DeviceSettingsCapability.GetParamRequest.getParamId(request);

      DeviceSettingsParamDefinition param = settingsContext.getByParamId(paramId);
      if (param == null) {
         context.respondToPlatform(DeviceSettingsCapability.GetParamResponse.builder()
               .withStatus("INVALID_PARAM")
               .build());
         return true;
      }

      // Send Z-Wave configuration.get to refresh the value
      sendConfigurationGet(context, param.getZwaveParamNo());

      // Return last known value
      Object currentValue = null;
      try {
         currentValue = context.getVariable(param.getVariableKey());
      } catch (Exception e) {
         // variable not set yet
      }

      DeviceSettingsCapability.GetParamResponse.Builder response = DeviceSettingsCapability.GetParamResponse.builder()
            .withStatus("PENDING");
      if (currentValue != null) {
         response.withValue(String.valueOf(currentValue));
      }

      context.respondToPlatform(response.build());
      return true;
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

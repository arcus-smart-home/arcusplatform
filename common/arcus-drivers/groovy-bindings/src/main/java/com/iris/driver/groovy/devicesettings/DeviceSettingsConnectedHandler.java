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
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;

/**
 * On device connect, issues Z-Wave configuration.get for every declared
 * parameter to sync platform state with actual device configuration.
 *
 * Returns false so that the driver's own onConnected handler also runs.
 */
public class DeviceSettingsConnectedHandler implements ContextualEventHandler<Object> {
   private static final Logger log = LoggerFactory.getLogger(DeviceSettingsConnectedHandler.class);

   private static final ZWaveCommandClass CONFIGURATION_CC = ZWaveAllCommandClasses.getClass("configuration");

   private final DeviceSettingsContext settingsContext;

   public DeviceSettingsConnectedHandler(DeviceSettingsContext settingsContext) {
      this.settingsContext = settingsContext;
   }

   @Override
   public boolean handleEvent(DeviceDriverContext context, Object event) throws Exception {
      log.debug("Device connected, reading {} device settings params",
            settingsContext.getParams().size());

      // Populate devsettings:params immediately with defaults so the attribute
      // is visible before config reports arrive from the device
      DeviceSettingsUtil.emitParamsValueChange(context, settingsContext);

      for (DeviceSettingsParamDefinition param : settingsContext.getParams()) {
         sendConfigurationGet(context, param.getZwaveParamNo());
      }

      // Return false to allow driver's own onConnected handler to also run
      return false;
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

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
import com.iris.driver.handler.ContextualEventHandler;

/**
 * On device connect, issues Z-Wave configuration.get for every declared
 * parameter to sync platform state with actual device configuration, unless the
 * driver declared {@code deviceSettings(readOnConnect: false)} and reads them itself.
 *
 * Returns false so that the driver's own onConnected handler also runs.
 */
public class DeviceSettingsConnectedHandler implements ContextualEventHandler<Object> {
   private static final Logger log = LoggerFactory.getLogger(DeviceSettingsConnectedHandler.class);

   private final DeviceSettingsContext settingsContext;

   public DeviceSettingsConnectedHandler(DeviceSettingsContext settingsContext) {
      this.settingsContext = settingsContext;
   }

   @Override
   public boolean handleEvent(DeviceDriverContext context, Object event) throws Exception {
      if (settingsContext.isReadOnConnect()) {
         DeviceSettingsUtil.readAll(context, settingsContext);
      } else {
         // the driver calls deviceSettings.readAll() when the device can take it
         log.debug("Device connected, settings will be read by the driver");
         DeviceSettingsUtil.emitParamsValueChange(context, settingsContext);
      }

      // Return false to allow driver's own onConnected handler to also run
      return false;
   }
}

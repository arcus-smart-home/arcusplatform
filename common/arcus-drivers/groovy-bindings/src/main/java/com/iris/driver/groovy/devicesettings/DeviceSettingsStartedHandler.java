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

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.devicesettings.DeviceSettingsContext;
import com.iris.driver.handler.ContextualEventHandler;

/**
 * Rebuilds devsettings:params from the driver each time driver-services loads the driver for
 * a device, as it does at the device's first message after a restart or deploy. The saved
 * attribute otherwise keeps the labels, units, descriptions and parameters of the driver that
 * last saved it until the device connects or reports a setting, since a driver change without
 * a version bump is no upgrade. Nothing is sent to the device: like any onStarted handler, what
 * this sets is saved with the device's next commit, and only if it differs from the saved list.
 * A hot reload of driver files doesn't restart drivers already loaded.
 *
 * A setting the device hasn't reported shows its default, as everywhere else.
 */
public class DeviceSettingsStartedHandler implements ContextualEventHandler<Object> {

   private final DeviceSettingsContext settingsContext;

   public DeviceSettingsStartedHandler(DeviceSettingsContext settingsContext) {
      this.settingsContext = settingsContext;
   }

   @Override
   public boolean handleEvent(DeviceDriverContext context, Object event) throws Exception {
      DeviceSettingsUtil.emitParamsValueChange(context, settingsContext);
      // never claims the event from the driver's own handlers
      return false;
   }
}

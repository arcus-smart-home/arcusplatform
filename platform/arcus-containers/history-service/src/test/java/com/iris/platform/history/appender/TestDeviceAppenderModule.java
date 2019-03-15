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
package com.iris.platform.history.appender;

import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.platform.history.appender.devvc.DeviceButtonAppender;
import com.iris.platform.history.appender.devvc.DeviceCOAppender;
import com.iris.platform.history.appender.devvc.DeviceContactAppender;
import com.iris.platform.history.appender.devvc.DeviceDimmerAppender;
import com.iris.platform.history.appender.devvc.DeviceDoorLockAppender;
import com.iris.platform.history.appender.devvc.DeviceFanAppender;
import com.iris.platform.history.appender.devvc.DeviceGasLeakAppender;
import com.iris.platform.history.appender.devvc.DeviceGlassAppender;
import com.iris.platform.history.appender.devvc.DeviceMotionAppender;
import com.iris.platform.history.appender.devvc.DeviceMotorizedDoorAppender;
import com.iris.platform.history.appender.devvc.DevicePetDoorAppender;
import com.iris.platform.history.appender.devvc.DevicePowerAppender;
import com.iris.platform.history.appender.devvc.DeviceSmokeAppender;
import com.iris.platform.history.appender.devvc.DeviceSwitchAppender;
import com.iris.platform.history.appender.devvc.DeviceThermostatAppender;
import com.iris.platform.history.appender.devvc.DeviceTiltAppender;
import com.iris.platform.history.appender.devvc.DeviceValveAppender;
import com.iris.platform.history.appender.devvc.DeviceWaterLeakAppender;
import com.iris.platform.history.appender.devvc.DeviceWaterSoftenerAppender;

public class TestDeviceAppenderModule extends AbstractIrisModule {

	@Override
   protected void configure() {
	   bind(DeviceButtonAppender.class);
	   bind(DeviceCOAppender.class);
	   bind(DeviceContactAppender.class);
	   bind(DeviceDimmerAppender.class);
	   bind(DeviceDoorLockAppender.class);
	   bind(DevicePetDoorAppender.class);
	   bind(DeviceFanAppender.class);
	   bind(DeviceGasLeakAppender.class);
	   bind(DeviceGlassAppender.class);
	   bind(DeviceMotionAppender.class);
	   bind(DeviceMotorizedDoorAppender.class);
	   bind(DevicePowerAppender.class);
	   bind(DeviceSmokeAppender.class);
	   bind(DeviceSwitchAppender.class);
	   bind(DeviceTiltAppender.class);
	   bind(DeviceValveAppender.class);
	   bind(DeviceWaterLeakAppender.class);
	   bind(DeviceWaterSoftenerAppender.class);
	   bind(DeviceThermostatAppender.class);
   }

}


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
/**
 *
 */
package com.iris.platform.history.appender;

import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.platform.history.appender.devvc.DeviceButtonAppender;
import com.iris.platform.history.appender.devvc.DeviceCOAppender;
import com.iris.platform.history.appender.devvc.DeviceContactAppender;
import com.iris.platform.history.appender.devvc.DeviceDimmerAppender;
import com.iris.platform.history.appender.devvc.DeviceDoorLockAppender;
import com.iris.platform.history.appender.devvc.DeviceFanAppender;
import com.iris.platform.history.appender.devvc.DeviceGlassAppender;
import com.iris.platform.history.appender.devvc.DeviceMotionAppender;
import com.iris.platform.history.appender.devvc.DeviceMotorizedDoorAppender;
import com.iris.platform.history.appender.devvc.DevicePetDoorAppender;
import com.iris.platform.history.appender.devvc.DevicePowerAppender;
import com.iris.platform.history.appender.devvc.DeviceSmokeAppender;
import com.iris.platform.history.appender.devvc.DeviceSpaceHeaterAppender;
import com.iris.platform.history.appender.devvc.DeviceSwitchAppender;
import com.iris.platform.history.appender.devvc.DeviceThermostatAppender;
import com.iris.platform.history.appender.devvc.DeviceTamperAppender;
import com.iris.platform.history.appender.devvc.DeviceTiltAppender;
import com.iris.platform.history.appender.devvc.DeviceValveAppender;
import com.iris.platform.history.appender.devvc.DeviceVentAppender;
import com.iris.platform.history.appender.devvc.DeviceWaterLeakAppender;
import com.iris.platform.history.appender.devvc.DeviceWaterSoftenerAppender;
import com.iris.platform.history.appender.hub.HubConnectionAppender;
import com.iris.platform.history.appender.incident.AlertEventAppender;
import com.iris.platform.history.appender.incident.HistoryAddedAppender;
import com.iris.platform.history.appender.person.InvitationDeclinedAppender;
import com.iris.platform.history.appender.person.PersonAddedAppender;
import com.iris.platform.history.appender.person.PersonRemovedAppender;
import com.iris.platform.history.appender.scene.SceneFiredAppender;
import com.iris.platform.history.appender.subsys.CareSubsystemActivityAppender;
import com.iris.platform.history.appender.subsys.CareSubsystemEventsAppender;
import com.iris.platform.history.appender.subsys.DoorsNLocksSubsystemAppender;
import com.iris.platform.history.appender.subsys.LawnNGardenSubsystemAppender;
import com.iris.platform.history.appender.subsys.PlaceMonitorSubsystemAppender;
import com.iris.platform.history.appender.subsys.PresenceSubsystemAppender;
import com.iris.platform.history.appender.subsys.SafetySubsystemAppender;
import com.iris.platform.history.appender.subsys.SecuritySubsystemEventsAppender;
import com.iris.platform.history.appender.subsys.WaterSubsystemEventsAppender;
import com.iris.platform.history.appender.video.VideoAddedAppender;

/**
 *
 */
public class HistoryAppenderModule extends AbstractIrisModule {

   /* (non-Javadoc)
    * @see com.google.inject.AbstractModule#configure()
    */
   @Override
   protected void configure() {
      bindListToInstancesOf(HistoryAppender.class);

      bind(DeviceEventAppender.class);
      bind(DevicePowerAppender.class);

      bind(DeviceButtonAppender.class);
      bind(DeviceCOAppender.class);
      bind(DeviceContactAppender.class);
      bind(DeviceDimmerAppender.class);
      bind(DeviceDoorLockAppender.class);
      bind(DevicePetDoorAppender.class);
      bind(DeviceFanAppender.class);
      bind(DeviceGlassAppender.class);
      bind(DeviceMotionAppender.class);
      bind(DeviceMotorizedDoorAppender.class);
      bind(DeviceSmokeAppender.class);
      bind(DeviceSwitchAppender.class);
      bind(DeviceThermostatAppender.class);
      bind(DeviceTamperAppender.class);
      bind(DeviceTiltAppender.class);
      bind(DeviceValveAppender.class);
      bind(DeviceVentAppender.class);
      bind(DeviceWaterLeakAppender.class);
      bind(DeviceWaterSoftenerAppender.class);
      
//      bind(HubBatteryAppender.class);
      bind(HubConnectionAppender.class);
//      bind(HubConnectionTypeAppender.class);

      bind(SecuritySubsystemEventsAppender.class);
      bind(SafetySubsystemAppender.class);
      bind(DoorsNLocksSubsystemAppender.class);
      bind(PresenceSubsystemAppender.class);
      bind(CareSubsystemActivityAppender.class);
      bind(CareSubsystemEventsAppender.class);
      bind(LawnNGardenSubsystemAppender.class);
      bind(WaterSubsystemEventsAppender.class);
      bind(PlaceMonitorSubsystemAppender.class);
      bind(SceneFiredAppender.class);
      bind(DeviceSpaceHeaterAppender.class);
      
      bind(AlertEventAppender.class);
      bind(HistoryAddedAppender.class);
      
      bind(VideoAddedAppender.class);
      bind(PersonAddedAppender.class);
      bind(PersonRemovedAppender.class);
      bind(InvitationDeclinedAppender.class);
      
   }


}


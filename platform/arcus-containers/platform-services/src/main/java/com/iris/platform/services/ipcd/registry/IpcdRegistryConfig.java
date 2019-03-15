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
package com.iris.platform.services.ipcd.registry;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class IpcdRegistryConfig {

   @Inject(optional = true)
   @Named("ipcd.service.add.device.timeout.secs")
   private int addDeviceTimeoutSecs = 30;

   @Inject(optional = true)
   @Named("ipcd.offline.timeout.min")
   private int offlineTimeoutMin = 10;

   @Inject(optional = true)
   @Named("ipcd.timeout.interval.sec")
   private int timeoutIntervalSec = 30;

   public int getAddDeviceTimeoutSecs() {
      return addDeviceTimeoutSecs;
   }

   public void setAddDeviceTimeoutSecs(int addDeviceTimeoutSecs) {
      this.addDeviceTimeoutSecs = addDeviceTimeoutSecs;
   }

   public int getOfflineTimeoutMin() {
      return offlineTimeoutMin;
   }

   public void setOfflineTimeoutMin(int offlineTimeoutMin) {
      this.offlineTimeoutMin = offlineTimeoutMin;
   }

   public int getTimeoutIntervalSec() {
      return timeoutIntervalSec;
   }

   public void setTimeoutIntervalSec(int timeoutIntervalSec) {
      this.timeoutIntervalSec = timeoutIntervalSec;
   }
}


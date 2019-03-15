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
package com.iris.firmware.ota;

import java.util.ArrayList;
import java.util.List;

public class DeviceOTAFirmware {
   private List<DeviceOTAFirmwareItem> firmwares = new ArrayList<DeviceOTAFirmwareItem>();

   private Integer retryIntervalMins;
   private Integer retryAttemptsMax;

 
   public Integer getRetryIntervalMins() {
      return retryIntervalMins;
   }

   public void setRetryIntervalMins(Integer retryIntervalMins) {
      this.retryIntervalMins = retryIntervalMins;
   }

   public Integer getRetryAttemptsMax() {
      return retryAttemptsMax;
   }

   public void setRetryAttemptsMax(Integer retryAttemptsMax) {
      this.retryAttemptsMax = retryAttemptsMax;
   }

   public List<DeviceOTAFirmwareItem> getFirmwares() {
      return firmwares;
   }

   public void setFirmwares(List<DeviceOTAFirmwareItem> firmwares) {
      this.firmwares = firmwares;
   }
}


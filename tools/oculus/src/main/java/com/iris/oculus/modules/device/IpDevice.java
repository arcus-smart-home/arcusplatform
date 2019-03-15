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
package com.iris.oculus.modules.device;

public class IpDevice {
   private final String deviceType;
   private final String sn;
   private final String modelCode;
   private final String serialCode;
   
   public IpDevice(String deviceType, String sn, String modelCode, String serialCode) {
      this.deviceType = deviceType;
      this.sn = sn;
      this.modelCode = modelCode;
      this.serialCode = serialCode;
   }

   public String getDeviceType() {
      return deviceType;
   }

   public String getSn() {
      return sn;
   }
   
   public String getModelCode() {
      return modelCode;
   }
   
   public String getSerialCode() {
      return serialCode;
   }
}


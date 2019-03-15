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

public class DeviceOTAFirmwareResponse {

   private boolean upgrade;
   private String targetVersion;
   private String targetImage;
   private int retryAttempts;
   private int retryIntervalMinutes;
   private String md5;

   
   public DeviceOTAFirmwareResponse(){
      
   }
   public DeviceOTAFirmwareResponse(boolean upgrade) {
      this.upgrade = upgrade;
   }

   public DeviceOTAFirmwareResponse(boolean upgrade, String targetVersion, String targetImage,int retryAttempts, int retryIntervalMinutes) {
      this(upgrade, targetVersion, targetImage, retryAttempts, retryIntervalMinutes, null);
   }
   
   public DeviceOTAFirmwareResponse(boolean upgrade, String targetVersion, String targetImage,int retryAttempts, int retryIntervalMinutes, String md5) {
      this.upgrade = upgrade;
      this.targetVersion = targetVersion;
      this.targetImage = targetImage;
      this.retryAttempts=retryAttempts;
      this.retryIntervalMinutes=retryIntervalMinutes;
      this.md5 = md5;
   }



   public void setRetryAttempts(int retryAttempts) {
      this.retryAttempts = retryAttempts;
   }
   public void setRetryIntervalMinutes(int retryIntervalMinutes) {
      this.retryIntervalMinutes = retryIntervalMinutes;
   }
   public int getRetryAttempts() {
      return retryAttempts;
   }
   public int getRetryIntervalMinutes() {
      return retryIntervalMinutes;
   }
   public boolean isUpgrade() {
      return upgrade;
   }

   public String getTargetVersion() {
      return targetVersion;
   }

   public String getTargetImage() {
      return targetImage;
   }

   public String getMd5() {
      return md5;
   }
}


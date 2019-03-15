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
package com.iris.alexa.message.v2.error;


abstract class FirmwareOutdatedErrorPayload implements ErrorPayload {

   private String minimumFirmwareVersion;
   private String currentFirmwareVersion;

   public String getMinimumFirmwareVersion() {
      return minimumFirmwareVersion;
   }

   public void setMinimumFirmwareVersion(String minimumFirmwareVersion) {
      this.minimumFirmwareVersion = minimumFirmwareVersion;
   }

   public String getCurrentFirmwareVersion() {
      return currentFirmwareVersion;
   }

   public void setCurrentFirmwareVersion(String currentFirmwareVersion) {
      this.currentFirmwareVersion = currentFirmwareVersion;
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() + " [minimumFirmwareVersion="
            + minimumFirmwareVersion + ", currentFirmwareVersion="
            + currentFirmwareVersion + ']';
   }


}


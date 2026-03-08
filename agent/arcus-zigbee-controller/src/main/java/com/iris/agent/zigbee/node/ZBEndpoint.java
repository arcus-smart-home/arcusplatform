/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Arcus Project
 *
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

package com.iris.agent.zigbee.node;

public class ZBEndpoint {
   private long id;
   private final long profileDbId;
   private final int endpointId;

   // From ZigBee Simple Descriptor
   private int deviceId;
   private int deviceVersion;

   // From ZigBee Basic Cluster
   private int zclVersion;
   private int appVersion;
   private int stkVersion;
   private int hwVersion;
   private String manufacturerName;
   private String modelIdentifier;
   private String dateCode;
   private int powerSource;

   public ZBEndpoint(long id, long profileDbId, int endpointId,
                     int deviceId, int deviceVersion,
                     int zclVersion, int appVersion, int stkVersion, int hwVersion,
                     String manufacturerName, String modelIdentifier, String dateCode,
                     int powerSource) {
      this.id = id;
      this.profileDbId = profileDbId;
      this.endpointId = endpointId;
      this.deviceId = deviceId;
      this.deviceVersion = deviceVersion;
      this.zclVersion = zclVersion;
      this.appVersion = appVersion;
      this.stkVersion = stkVersion;
      this.hwVersion = hwVersion;
      this.manufacturerName = manufacturerName;
      this.modelIdentifier = modelIdentifier;
      this.dateCode = dateCode;
      this.powerSource = powerSource;
   }

   public ZBEndpoint(long profileDbId, int endpointId) {
      this(0, profileDbId, endpointId, 0, 0, 0, 0, 0, 0, null, null, null, 0);
   }

   public long getId() {
      return id;
   }

   public void setId(long id) {
      this.id = id;
   }

   public long getProfileDbId() {
      return profileDbId;
   }

   public int getEndpointId() {
      return endpointId;
   }

   public int getDeviceId() {
      return deviceId;
   }

   public void setDeviceId(int deviceId) {
      this.deviceId = deviceId;
   }

   public int getDeviceVersion() {
      return deviceVersion;
   }

   public void setDeviceVersion(int deviceVersion) {
      this.deviceVersion = deviceVersion;
   }

   public int getZclVersion() {
      return zclVersion;
   }

   public void setZclVersion(int zclVersion) {
      this.zclVersion = zclVersion;
   }

   public int getAppVersion() {
      return appVersion;
   }

   public void setAppVersion(int appVersion) {
      this.appVersion = appVersion;
   }

   public int getStkVersion() {
      return stkVersion;
   }

   public void setStkVersion(int stkVersion) {
      this.stkVersion = stkVersion;
   }

   public int getHwVersion() {
      return hwVersion;
   }

   public void setHwVersion(int hwVersion) {
      this.hwVersion = hwVersion;
   }

   public String getManufacturerName() {
      return manufacturerName;
   }

   public void setManufacturerName(String manufacturerName) {
      this.manufacturerName = manufacturerName;
   }

   public String getModelIdentifier() {
      return modelIdentifier;
   }

   public void setModelIdentifier(String modelIdentifier) {
      this.modelIdentifier = modelIdentifier;
   }

   public String getDateCode() {
      return dateCode;
   }

   public void setDateCode(String dateCode) {
      this.dateCode = dateCode;
   }

   public int getPowerSource() {
      return powerSource;
   }

   public void setPowerSource(int powerSource) {
      this.powerSource = powerSource;
   }
}

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
package com.iris.protocol.ipcd;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import com.iris.messages.model.Copyable;

public class LegacyIpDevice implements Copyable<LegacyIpDevice>{
   private String deviceText;
   private String deviceType;
   private UUID accountId;
   private UUID placeId;
   private String v1DeviceId;
   private Date created;
   private Map<String,String> attributes;
   
   public String getDeviceText() {
      return deviceText;
   }
   public void setDeviceText(String deviceText) {
      this.deviceText = deviceText;
   }
   public String getDeviceType() {
      return deviceType;
   }
   public void setDeviceType(String deviceType) {
      this.deviceType = deviceType;
   }
   public UUID getAccountId() {
      return accountId;
   }
   public void setAccountId(UUID accountId) {
      this.accountId = accountId;
   }
   public UUID getPlaceId() {
      return placeId;
   }
   public void setPlaceId(UUID placeId) {
      this.placeId = placeId;
   }
   public String getV1DeviceId() {
      return v1DeviceId;
   }
   public void setV1DeviceId(String v1DeviceId) {
      this.v1DeviceId = v1DeviceId;
   }
   public Date getCreated() {
      return created;
   }
   public void setCreated(Date created) {
      this.created = created;
   }
   public Map<String, String> getAttributes() {
      return attributes;
   }
   public void setAttributes(Map<String, String> attributes) {
      this.attributes = attributes;
   }
   
   @Override
   public LegacyIpDevice copy() {
      try {
         return (LegacyIpDevice)clone();
      }
      catch (CloneNotSupportedException e) {
         throw new RuntimeException(e);
      }
   }
}


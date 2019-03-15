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
package com.iris.agent.backup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.protoc.runtime.ProtocUtil;

public class V1MigrationReport {
   private static final Logger log = LoggerFactory.getLogger(V1MigrationReport.class);

   public static final String SUCCESS = "device migrated successfully";
   public static final String FAILURE = "device failed to migrated: ";
   public static final String UNKNOWN = "unknown device";

   private final Map<Long,Entry> report;

   public V1MigrationReport(Map<Long,Map<String,String>> devices) {
      this.report = new LinkedHashMap<>();
      for (Map.Entry<Long,Map<String,String>> device : devices.entrySet()) {
         Long key = device.getKey();
         Map<String,String> props = device.getValue();

         String type = props.get("type");
         String v1Type = props.get("devType");
         String online = props.get("online");
         String name = props.get("name");
         String model = props.get("model");


         Entry entry = new Entry(v1Type, model, type, name, FAILURE + UNKNOWN, !Boolean.valueOf(online), true);
         log.debug("adding initial entry into migration report: {} => {}", key, entry);

         Object old = this.report.put(key, entry);
         if (old != null) {
            log.error("!!! overwrote migration entry: {}", old);
         }
      }
   }

   public List<Map<String,Object>> getReportAsList() {
      List<Map<String,Object>> result = new ArrayList<>(report.size());
      for (Entry entry : report.values()) {
         result.add(entry.getReportAsMap());
      }

      return result;
   }

   public void successfulZWave(Long key, int homeId, byte nodeId, boolean online) {
      success(key, id(homeId, nodeId), online, "zwave");
   }

   public void failedZWave(Long key, int homeId, byte nodeId, String msg) {
      failure(key, id(homeId, nodeId), msg, "zwave");
   }

   public void successfulZigbee(Long key, long eui64, boolean online) {
      success(key, id(eui64), online, "zigbee");
   }

   public void failedZigbee(Long key, long eui64, String msg) {
      failure(key, id(eui64), msg, "zigbee");
   }

   public void successfulSercomm(Long key, long mac, boolean online) {
      success(key, mac(mac), online, "sercomm");
   }

   public void failedSercomm(Long key, long mac, String msg) {
      failure(key, mac(mac), msg, "sercomm");
   }

   private void success(Long key, String id, boolean online, String type) {
      Entry entry = report.get(key);
      if (entry == null) {
         log.warn("attempt to update unknown {} device during migration, dropping: {}", type, key);
         return;
      }

      entry.deviceId = id;
      entry.message = SUCCESS;
      entry.offline = !online;
      entry.failed = false;

      log.debug("updating migration report entry: {} => {}", key, entry);
   }

   private void failure(Long key, String id, String msg, String type) {
      Entry entry = report.get(key);
      if (entry == null) {
         log.warn("attempt to update unknown {} device during migration, dropping: {}", type, key);
         return;
      }

      entry.deviceId = id;
      entry.message = FAILURE + msg;
      entry.offline = true;
      entry.failed = true;

      log.debug("updating migration report entry: {} => {}", key, entry);
   }

   private static String id(int homeId, byte nodeId) {
      return ProtocUtil.toHexString(homeId) + ":" + ProtocUtil.toHexString(nodeId);
   }

   private static String id(long eui64) {
      return ProtocUtil.toHexString(eui64);
   }

   private static String mac(long mac) {
      return ProtocUtil.toHexString(mac & 0xFFFFFFFFFFFFL).substring(4);
   }

   @Override
   public String toString() {
      return "V1MigrationReport [report=" + report + "]";
   }

   public static final class Entry {
      private final String v1Type;
      private final String v1Model;
      private final String deviceType;
      private final String deviceName;

      private String message;
      private boolean offline;
      private boolean failed;
      private String deviceId;

      public Entry(String v1Type, String v1Model, String deviceType, String deviceName, String message, boolean offline, boolean failed) {
         this.v1Type = v1Type;
         this.v1Model = v1Model;
         this.deviceType = deviceType;
         this.deviceId = "unknown";
         this.deviceName = deviceName;
         this.message = message;
         this.offline = offline;
         this.failed = failed;
      }

      public Map<String,Object> getReportAsMap() {
         Map<String,Object> result = new HashMap<>();
         result.put("type", v1Type);
         result.put("model", v1Model);
         result.put("protocol", deviceType);
         result.put("id", deviceId);
         result.put("name", deviceName);
         result.put("message", message);
         result.put("offline", offline);
         result.put("failed", failed);

         return result;
      }

      public String getDeviceName() {
         return deviceName;
      }

      public String getDeviceType() {
         return deviceType;
      }

      public String getDeviceId() {
         return deviceId;
      }

      public String getMessage() {
         return message;
      }

      public boolean isOffline() {
         return offline;
      }

      public boolean isFailed() {
         return failed;
      }

      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + ((deviceId == null) ? 0 : deviceId.hashCode());
         result = prime * result + ((deviceName == null) ? 0 : deviceName.hashCode());
         result = prime * result + ((deviceType == null) ? 0 : deviceType.hashCode());
         result = prime * result + (failed ? 1231 : 1237);
         result = prime * result + ((message == null) ? 0 : message.hashCode());
         result = prime * result + (offline ? 1231 : 1237);
         result = prime * result + ((v1Model == null) ? 0 : v1Model.hashCode());
         result = prime * result + ((v1Type == null) ? 0 : v1Type.hashCode());
         return result;
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (getClass() != obj.getClass())
            return false;
         Entry other = (Entry) obj;
         if (deviceId == null) {
            if (other.deviceId != null)
               return false;
         } else if (!deviceId.equals(other.deviceId))
            return false;
         if (deviceName == null) {
            if (other.deviceName != null)
               return false;
         } else if (!deviceName.equals(other.deviceName))
            return false;
         if (deviceType == null) {
            if (other.deviceType != null)
               return false;
         } else if (!deviceType.equals(other.deviceType))
            return false;
         if (failed != other.failed)
            return false;
         if (message == null) {
            if (other.message != null)
               return false;
         } else if (!message.equals(other.message))
            return false;
         if (offline != other.offline)
            return false;
         if (v1Model == null) {
            if (other.v1Model != null)
               return false;
         } else if (!v1Model.equals(other.v1Model))
            return false;
         if (v1Type == null) {
            if (other.v1Type != null)
               return false;
         } else if (!v1Type.equals(other.v1Type))
            return false;
         return true;
      }

      @Override
      public String toString() {
         return "Entry [v1Type=" + v1Type + ", v1Model=" + v1Model + ", deviceType=" + deviceType + ", deviceName=" + deviceName + ", message=" + message + ", offline=" + offline + ", failed=" + failed + ", deviceId=" + deviceId + "]";
      }
   }
}


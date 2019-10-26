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
package com.iris.agent.zwave.db;

import java.util.HashMap;
import java.util.Map;

import com.iris.agent.zwave.ZWNetwork;

public final class NetworkRecord {
   public static final String CONFIG_HOMEID = "hubzip:homeid";
   public static final String CONFIG_NWKKEYMSB = "hubzip:nwkkeymsb";
   public static final String CONFIG_NWKKEYLSB = "hubzip:nwkkeylsb";

   private long homeId;
   private long networkKeyMsb;
   private long networkKeyLsb; 
   
   private boolean needsUpdate = false;

   NetworkRecord(long homeId, long networkKeyMsb, long networkKeyLsb) {
      this.homeId = homeId;
      this.networkKeyMsb = networkKeyMsb;
      this.networkKeyLsb = networkKeyLsb;      
   }
   
   public long getHomeId() {
      return this.homeId;
   }
   
   public long getNetworkKeyMsb() {
      return this.networkKeyMsb;
   }
   
   public long getNetworkKeyLsb() {
      return this.networkKeyLsb;
   }
   
   public void setHomeId(long homeId) {
      if (this.homeId != homeId) {
         needsUpdate = true;
      }
      this.homeId = homeId;
   }
   
   public void updated() {
      needsUpdate = false;
   }
   
   public boolean needsUpdate() {
      return needsUpdate;
   }
   
   public void invalidate() {
      this.homeId = ZWNetwork.NO_HOME_ID;
   }

   public boolean hasHomeId() {
      return this.homeId != ZWNetwork.NO_HOME_ID;
   }

   public boolean hasNetworkKey() {
      return this.networkKeyMsb != ZWNetwork.NO_NETWORK_KEY_MSB &&
             this.networkKeyLsb != ZWNetwork.NO_NETWORK_KEY_LSB;
   }

   public Map<String, Object> asAttributeMap() {
      Map<String,Object> attributes = new HashMap<>();
      attributes.put(CONFIG_HOMEID, homeId);
      return attributes;
   }
}



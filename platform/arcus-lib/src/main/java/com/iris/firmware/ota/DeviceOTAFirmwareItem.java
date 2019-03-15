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

import com.google.common.collect.ImmutableList;



public class DeviceOTAFirmwareItem{
      private String populations;
      private String productId;
      private String version;
      private String path;
      private Integer retryIntervalMins;
      private Integer retryAttemptsMax;
      private String md5;
      

      private List<DeviceOTAFirmwareFromVersion> fromVersions;

      public DeviceOTAFirmwareItem(String populations, String productId, String version, String path, Integer retryIntervalMins, Integer retryAttemptsMax, String md5, List<DeviceOTAFirmwareFromVersion> fromVersions) {
         super();
         this.populations = populations;
         this.productId = productId;
         this.version = version;
         this.path = path;
         this.retryIntervalMins=retryIntervalMins;
         this.retryAttemptsMax=retryAttemptsMax;
         this.fromVersions = fromVersions;
         this.md5 = md5;
      }      
      
      @Override
      public String toString() {
         return "DeviceOTAFirmware [populations=" + populations + ", productId=" + productId + ", version=" + version + ", path=" + path + ", md5=" + md5 + "]";
      }
      
      public List<String> productKeys(){
         List<String>keys = new ArrayList<String>();
         for(String population:populations.split(",")){
            keys.add(createProductKey(population, productId));
         }
         return ImmutableList.<String>copyOf(keys);
      }

      public static String createProductKey(String population, String productId){
         return population+"-"+productId;
      }

      
      public String getPopulation() {
         return populations;
      }
      public String getProductId() {
         return productId;
      }
      public String getVersion() {
         return version;
      }
      public String getPath() {
         return path;
      }
      public Integer getRetryIntervalMins() {
         return retryIntervalMins;
      }
      public Integer getRetryAttemptsMax() {
         return retryAttemptsMax;
      }

   public String getMd5() {
      return md5;
   }

   public boolean isVersionUpgradable(String currentVersion) {
         if (currentVersion == null) return false;
         if (version.equals(currentVersion)) return false; 
         if (fromVersions == null) return true;
         if (fromVersions.isEmpty()) return true;
         
         for (DeviceOTAFirmwareFromVersion fromVersion : fromVersions ) {
            if (fromVersion.matches(currentVersion)) return true;
         }
         return false;
      }
      
   }


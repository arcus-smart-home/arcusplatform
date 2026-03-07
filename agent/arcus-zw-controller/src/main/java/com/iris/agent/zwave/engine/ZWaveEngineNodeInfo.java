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
package com.iris.agent.zwave.engine;

/**
 * Cached node protocol information obtained from the Z-Wave controller
 * via FUNC_ID_ZW_GET_NODE_PROTOCOL_INFO.
 */
public class ZWaveEngineNodeInfo {
   boolean listening;
   boolean frequentListening;
   boolean beaming;
   boolean routing;
   boolean security;
   long maxBaudRate;
   int version;
   int securityByte;
   int basicType;
   int genericType;
   int specificType;

   public boolean isListening() {
      return listening;
   }

   public boolean isFrequentListening() {
      return frequentListening;
   }

   public boolean isBeaming() {
      return beaming;
   }

   public boolean isRouting() {
      return routing;
   }

   public boolean isSecurity() {
      return security;
   }

   public long getMaxBaudRate() {
      return maxBaudRate;
   }

   public int getVersion() {
      return version;
   }

   public int getSecurityByte() {
      return securityByte;
   }

   public int getBasicType() {
      return basicType;
   }

   public int getGenericType() {
      return genericType;
   }

   public int getSpecificType() {
      return specificType;
   }
}

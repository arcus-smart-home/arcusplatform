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
package com.iris.driver.reflex;

import org.eclipse.jdt.annotation.Nullable;

public final class ReflexMatchZigbeeIasZoneStatus implements ReflexMatch {
   public enum Type { ATTR, NOTIFICATION, BOTH }

   private final Type type;
   private final int profile;
   private final int endpoint;
   private final int cluster;
   private final @Nullable Integer manufacturer;
   private final @Nullable Integer flags;
   private final int setMask;
   private final int clrMask;
   private final int maxChangeDelay;

   public ReflexMatchZigbeeIasZoneStatus(Type type, int profile, int endpoint, int cluster, int setMask, int clrMask, int maxChangeDelay, @Nullable Integer manufacturer, @Nullable Integer flags) {
      this.type = type;
      this.profile = profile;
      this.endpoint = endpoint;
      this.cluster = cluster;
      this.setMask = setMask;
      this.clrMask = clrMask;
      this.maxChangeDelay = maxChangeDelay;
      this.manufacturer = manufacturer;
      this.flags = flags;
   }

   public Type getType() {
      return type;
   }

   public int getProfile() {
      return profile;
   }

   public int getEndpoint() {
      return endpoint;
   }

   public int getCluster() {
      return cluster;
   }

   public @Nullable Integer getManufacturer() {
      return manufacturer;
   }

   public @Nullable Integer getFlags() {
      return flags;
   }

   public int getSetMask() {
      return setMask;
   }

   public int getClrMask() {
      return clrMask;
   }

   public int getMaxChangeDelay() {
      return maxChangeDelay;
   }

   @Override
   public String toString() {
      return "ReflexMatchZigbeeIasZoneStatus [" + 
         "type=" + type + 
         ",profile=" + profile + 
         ",endpoint=" + endpoint + 
         ",cluster=" + cluster + 
         ",manuf=" + manufacturer + 
         ",flags=" + flags + 
         ",set=" + setMask +
         ",clr=" + clrMask + 
         ",maxDelay=" + maxChangeDelay +
      "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + clrMask;
      result = prime * result + cluster;
      result = prime * result + endpoint;
      result = prime * result + ((flags == null) ? 0 : flags.hashCode());
      result = prime * result + ((manufacturer == null) ? 0 : manufacturer.hashCode());
      result = prime * result + maxChangeDelay;
      result = prime * result + profile;
      result = prime * result + setMask;
      result = prime * result + ((type == null) ? 0 : type.hashCode());
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
      ReflexMatchZigbeeIasZoneStatus other = (ReflexMatchZigbeeIasZoneStatus) obj;
      if (clrMask != other.clrMask)
         return false;
      if (cluster != other.cluster)
         return false;
      if (endpoint != other.endpoint)
         return false;
      if (flags == null) {
         if (other.flags != null)
            return false;
      } else if (!flags.equals(other.flags))
         return false;
      if (manufacturer == null) {
         if (other.manufacturer != null)
            return false;
      } else if (!manufacturer.equals(other.manufacturer))
         return false;
      if (maxChangeDelay != other.maxChangeDelay)
         return false;
      if (profile != other.profile)
         return false;
      if (setMask != other.setMask)
         return false;
      if (type != other.type)
         return false;
      return true;
   }
}


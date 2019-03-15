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

import com.iris.protocol.zigbee.ZclData;

public final class ReflexMatchZigbeeAttribute implements ReflexMatch {
   public enum Type { READ, REPORT, BOTH }

   private final Type type;
   private final int profile;
   private final int endpoint;
   private final int cluster;
   private final @Nullable Integer manufacturer;
   private final @Nullable Integer flags;
   private final int attr;
   private final @Nullable ZclData value;

   public ReflexMatchZigbeeAttribute(Type type, int profile, int endpoint, int cluster, int attr, @Nullable ZclData value, @Nullable Integer manufacturer, @Nullable Integer flags) {
      this.type = type;
      this.profile = profile;
      this.endpoint = endpoint;
      this.cluster = cluster;
      this.attr = attr;
      this.value = value;
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

   public int getAttr() {
      return attr;
   }

   public @Nullable ZclData getValue() {
      return value;
   }

   @Override
   public String toString() {
      return "ReflexMatchZigbeeAttribute [" + 
         "type=" + type + 
         ",profile=" + profile + 
         ",endpoint=" + endpoint + 
         ",cluster=" + cluster + 
         ",manuf=" + manufacturer + 
         ",flags=" + flags + 
         ",attr=" + attr + 
         ",value=" + value + 
      "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + attr;
      result = prime * result + cluster;
      result = prime * result + endpoint;
      result = prime * result + ((flags == null) ? 0 : flags.hashCode());
      result = prime * result + ((manufacturer == null) ? 0 : manufacturer.hashCode());
      result = prime * result + profile;
      result = prime * result + ((type == null) ? 0 : type.hashCode());
      result = prime * result + ((value == null) ? 0 : value.hashCode());
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
      ReflexMatchZigbeeAttribute other = (ReflexMatchZigbeeAttribute) obj;
      if (attr != other.attr)
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
      if (profile != other.profile)
         return false;
      if (type != other.type)
         return false;
      if (value == null) {
         if (other.value != null)
            return false;
      } else if (!value.equals(other.value))
         return false;
      return true;
   }
}


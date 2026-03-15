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
package com.iris.driver.devicesettings;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable definition of a single device configuration parameter.
 * Built from the driver DSL {@code deviceSettings { param(...) }} block.
 */
public class DeviceSettingsParamDefinition {

   public enum ParamType {
      ENUM,
      RANGE,
      BOOLEAN
   }

   private final String paramId;
   private final String description;
   private final ParamType type;
   private final int zwaveParamNo;
   private final int zwaveParamSize;
   private final long min;
   private final long max;
   private final Map<String, String> enumValues;
   private final long defaultValue;

   private DeviceSettingsParamDefinition(Builder builder) {
      this.paramId = builder.paramId;
      this.description = builder.description;
      this.type = builder.type;
      this.zwaveParamNo = builder.zwaveParamNo;
      this.zwaveParamSize = builder.zwaveParamSize;
      this.min = builder.min;
      this.max = builder.max;
      this.enumValues = builder.enumValues != null
            ? Collections.unmodifiableMap(new LinkedHashMap<>(builder.enumValues))
            : Collections.emptyMap();
      this.defaultValue = builder.defaultValue;
   }

   public String getParamId() { return paramId; }
   public String getDescription() { return description; }
   public ParamType getType() { return type; }
   public int getZwaveParamNo() { return zwaveParamNo; }
   public int getZwaveParamSize() { return zwaveParamSize; }
   public long getMin() { return min; }
   public long getMax() { return max; }
   public Map<String, String> getEnumValues() { return enumValues; }
   public long getDefaultValue() { return defaultValue; }

   /**
    * Returns the variable key used to store this parameter's current value
    * in the driver context variables.
    */
   public String getVariableKey() {
      return "devsettings:" + paramId;
   }

   /**
    * Produces the client-facing map representation of this parameter's metadata.
    * Does NOT include the current value — that must be merged by the caller.
    */
   public Map<String, Object> toMap() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("paramId", paramId);
      map.put("name", paramId);
      map.put("description", description);
      map.put("type", type.name().toLowerCase());
      if (type == ParamType.ENUM) {
         map.put("values", enumValues);
      }
      if (type == ParamType.RANGE) {
         map.put("min", min);
         map.put("max", max);
      }
      map.put("dflt", defaultValue);
      map.put("zwaveParamNo", zwaveParamNo);
      map.put("zwaveParamSize", zwaveParamSize);
      return map;
   }

   /**
    * Validates a string value against this parameter's constraints.
    * @return null if valid, or an error message if invalid
    */
   public String validateValue(String value) {
      if (value == null) {
         return "value is required";
      }
      switch (type) {
         case BOOLEAN:
            if (!"0".equals(value) && !"1".equals(value)) {
               return "boolean parameter must be 0 or 1";
            }
            return null;
         case ENUM:
            if (!enumValues.containsKey(value)) {
               return "invalid enum value '" + value + "', valid values: " + enumValues.keySet();
            }
            return null;
         case RANGE:
            try {
               long v = Long.parseLong(value);
               if (v < min || v > max) {
                  return "value " + v + " out of range [" + min + ", " + max + "]";
               }
            } catch (NumberFormatException e) {
               return "value must be a number";
            }
            return null;
         default:
            return "unknown parameter type";
      }
   }

   public static Builder builder() {
      return new Builder();
   }

   public static class Builder {
      private String paramId;
      private String description = "";
      private ParamType type = ParamType.RANGE;
      private int zwaveParamNo;
      private int zwaveParamSize = 1;
      private long min = 0;
      private long max = 255;
      private Map<String, String> enumValues;
      private long defaultValue = 0;

      public Builder withParamId(String paramId) { this.paramId = paramId; return this; }
      public Builder withDescription(String description) { this.description = description; return this; }
      public Builder withType(ParamType type) { this.type = type; return this; }
      public Builder withZwaveParamNo(int no) { this.zwaveParamNo = no; return this; }
      public Builder withZwaveParamSize(int size) { this.zwaveParamSize = size; return this; }
      public Builder withMin(long min) { this.min = min; return this; }
      public Builder withMax(long max) { this.max = max; return this; }
      public Builder withEnumValues(Map<String, String> values) { this.enumValues = values; return this; }
      public Builder withDefaultValue(long dflt) { this.defaultValue = dflt; return this; }

      public DeviceSettingsParamDefinition build() {
         if (paramId == null || paramId.isEmpty()) {
            throw new IllegalArgumentException("paramId is required");
         }
         if (zwaveParamNo < 0) {
            throw new IllegalArgumentException("zwaveParamNo must be non-negative");
         }
         if (zwaveParamSize < 1 || zwaveParamSize > 4) {
            throw new IllegalArgumentException("zwaveParamSize must be 1-4");
         }
         return new DeviceSettingsParamDefinition(this);
      }
   }
}

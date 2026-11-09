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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
   // Short name for UIs, e.g. "Ramp rate on"; null when the driver gives none
   private final String label;
   private final String description;
   // Unit of a range value, e.g. "s" or "%"; null when the value has none
   private final String unit;
   private final ParamType type;
   private final int zwaveParamNo;
   private final int zwaveParamSize;
   private final long min;
   private final long max;
   private final Map<String, String> enumValues;
   private final long defaultValue;
   // Z-Wave (productType << 16 | productId) values this param applies to; empty = every product
   private final Set<Integer> products;
   private final boolean signed;
   private final boolean driverOnly;

   private DeviceSettingsParamDefinition(Builder builder) {
      this.paramId = builder.paramId;
      this.label = builder.label;
      this.description = builder.description;
      this.unit = builder.unit;
      this.type = builder.type;
      this.zwaveParamNo = builder.zwaveParamNo;
      this.zwaveParamSize = builder.zwaveParamSize;
      this.min = builder.min;
      this.max = builder.max;
      this.enumValues = builder.enumValues != null
            ? Collections.unmodifiableMap(new LinkedHashMap<>(builder.enumValues))
            : Collections.emptyMap();
      this.defaultValue = builder.defaultValue;
      this.products = Collections.unmodifiableSet(new LinkedHashSet<>(builder.products));
      this.driverOnly = builder.driverOnly;
      // a driver-only value never travels as device bytes, so there is nothing to decode
      this.signed = !driverOnly && lowestValue() < 0;
   }

   // Smallest and largest value the parameter can take: the range bounds, or the
   // numeric enum keys. Booleans and enums without numeric keys give 0.
   private long lowestValue() {
      if (type == ParamType.RANGE) {
         return min;
      }
      long lowest = 0;
      for (String key : enumValues.keySet()) {
         try {
            lowest = Math.min(lowest, Long.parseLong(key));
         } catch (NumberFormatException e) {
            // not a numeric key
         }
      }
      return lowest;
   }

   private long highestValue() {
      if (type == ParamType.RANGE) {
         return max;
      }
      long highest = 0;
      for (String key : enumValues.keySet()) {
         try {
            highest = Math.max(highest, Long.parseLong(key));
         } catch (NumberFormatException e) {
            // not a numeric key
         }
      }
      return highest;
   }

   public String getParamId() { return paramId; }
   /** The short name UIs show, or null if the driver gives none. */
   public String getLabel() { return label; }
   public String getDescription() { return description; }
   /** The unit of a range value, such as "s" or "%", or null. */
   public String getUnit() { return unit; }
   public ParamType getType() { return type; }
   public int getZwaveParamNo() { return zwaveParamNo; }
   public int getZwaveParamSize() { return zwaveParamSize; }
   public long getMin() { return min; }
   public long getMax() { return max; }
   public Map<String, String> getEnumValues() { return enumValues; }
   public long getDefaultValue() { return defaultValue; }

   /**
    * True for a setting the driver keeps itself, with no Z-Wave parameter behind
    * it, such as which of a keypad's sounds to play.
    */
   public boolean isDriverOnly() { return driverOnly; }

   /**
    * True when the parameter only exists on some of the products its driver matches.
    */
   public boolean hasProductFilter() {
      return !products.isEmpty();
   }

   /**
    * Whether this parameter exists on the given Z-Wave product. A parameter with no
    * product filter applies to every product; a filtered one never applies when the
    * product is unknown.
    */
   public boolean appliesTo(Integer productType, Integer productId) {
      if (products.isEmpty()) {
         return true;
      }
      if (productType == null || productId == null) {
         return false;
      }
      return products.contains(productKey(productType, productId));
   }

   /**
    * True when the value can be negative, so the bytes a device reports are a
    * two's-complement number. A parameter whose values are all zero or more
    * (e.g. 0..255 in one byte) is read as unsigned. {@link Builder#build()} rejects a
    * signed parameter whose values don't fit the signed range of its size.
    */
   public boolean isSigned() {
      return signed;
   }

   /**
    * Interprets an unsigned value read from {@code byteCount} bytes. For a signed
    * parameter, a value in the upper half of the unsigned range is the negative number
    * those bytes encode (0xFF in one byte is -1). Values that are already negative or
    * in the lower half come back unchanged, so this is safe to apply to a value that
    * has already been decoded.
    */
   public long decode(long raw, int byteCount) {
      if (!isSigned() || byteCount < 1 || byteCount > 4) {
         return raw;
      }
      int bits = byteCount * 8;
      long range = 1L << bits;
      if (raw >= (range >> 1) && raw < range) {
         return raw - range;
      }
      return raw;
   }

   /**
    * Applies {@link #decode} to a stored value, using the declared size. Values saved
    * before signed decoding existed (e.g. 255 for -1) read back correctly this way.
    */
   public Object decodeStored(Object stored) {
      if (stored instanceof Number && isSigned()) {
         return decode(((Number) stored).longValue(), zwaveParamSize);
      }
      return stored;
   }

   /**
    * Whether some product would get both this parameter and {@code other}.
    */
   public boolean sharesProductWith(DeviceSettingsParamDefinition other) {
      if (products.isEmpty() || other.products.isEmpty()) {
         return true;
      }
      return !Collections.disjoint(products, other.products);
   }

   static int productKey(int productType, int productId) {
      return ((productType & 0xFFFF) << 16) | (productId & 0xFFFF);
   }

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
    * {@code name} is the label, or the paramId for a parameter without one, as
    * drivers sent before labels existed.
    */
   public Map<String, Object> toMap() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("paramId", paramId);
      map.put("name", label != null ? label : paramId);
      map.put("description", description);
      if (unit != null) {
         map.put("unit", unit);
      }
      map.put("type", type.name().toLowerCase());
      if (type == ParamType.ENUM) {
         map.put("values", enumValues);
      }
      if (type == ParamType.RANGE) {
         map.put("min", min);
         map.put("max", max);
      }
      map.put("dflt", defaultValue);
      if (!driverOnly) {
         map.put("zwaveParamNo", zwaveParamNo);
         map.put("zwaveParamSize", zwaveParamSize);
      }
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
      private String label;
      private String description = "";
      private String unit;
      private ParamType type = ParamType.RANGE;
      private int zwaveParamNo;
      private int zwaveParamSize = 1;
      private long min = 0;
      private long max = 255;
      private Map<String, String> enumValues;
      private long defaultValue = 0;
      private final Set<Integer> products = new LinkedHashSet<>();
      private boolean driverOnly;

      public Builder withParamId(String paramId) { this.paramId = paramId; return this; }
      public Builder withLabel(String label) { this.label = label; return this; }
      public Builder withDescription(String description) { this.description = description; return this; }
      public Builder withUnit(String unit) { this.unit = unit; return this; }
      public Builder withType(ParamType type) { this.type = type; return this; }
      public Builder withZwaveParamNo(int no) { this.zwaveParamNo = no; return this; }
      public Builder withZwaveParamSize(int size) { this.zwaveParamSize = size; return this; }
      public Builder withMin(long min) { this.min = min; return this; }
      public Builder withMax(long max) { this.max = max; return this; }
      public Builder withEnumValues(Map<String, String> values) { this.enumValues = values; return this; }
      public Builder withDefaultValue(long dflt) { this.defaultValue = dflt; return this; }
      public Builder withDriverOnly(boolean driverOnly) { this.driverOnly = driverOnly; return this; }

      /** Limits the parameter to one Z-Wave product; call once per product. */
      public Builder withProduct(int productType, int productId) {
         if (productType < 0 || productType > 0xFFFF || productId < 0 || productId > 0xFFFF) {
            throw new IllegalArgumentException(String.format(
                  "product type and id must be 0x0000-0xFFFF, got 0x%X/0x%X", productType, productId));
         }
         this.products.add(productKey(productType, productId));
         return this;
      }

      public Builder withProducts(Collection<int[]> typeIdPairs) {
         for (int[] pair : typeIdPairs) {
            withProduct(pair[0], pair[1]);
         }
         return this;
      }

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
         if (label != null && (label.trim().isEmpty() || !label.equals(label.trim()))) {
            throw new IllegalArgumentException("param '" + paramId + "' label must not be blank or start or end with a space");
         }
         if (unit != null && (unit.trim().isEmpty() || !unit.equals(unit.trim()) || type != ParamType.RANGE)) {
            throw new IllegalArgumentException("param '" + paramId + "' unit must not be blank or start or end with a space, and only a range has one");
         }
         DeviceSettingsParamDefinition param = new DeviceSettingsParamDefinition(this);
         if (param.isSigned()) {
            // e.g. -1..255 in one byte: 200 and -56 are the same byte, so a value
            // couldn't be told apart from another one on the way back
            long half = 1L << (zwaveParamSize * 8 - 1);
            if (param.lowestValue() < -half || param.highestValue() > half - 1) {
               throw new IllegalArgumentException("param '" + paramId + "' goes below zero, so its values must fit "
                     + -half + ".." + (half - 1) + " for size " + zwaveParamSize);
            }
         }
         return param;
      }
   }
}

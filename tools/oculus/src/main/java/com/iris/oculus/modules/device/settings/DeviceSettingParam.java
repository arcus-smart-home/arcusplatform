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
package com.iris.oculus.modules.device.settings;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One entry of the devsettings:params attribute, as built by the driver's
 * DeviceSettingsContext. Values are held as the strings SetParam expects.
 */
public class DeviceSettingParam {
   private static final Logger logger = LoggerFactory.getLogger(DeviceSettingParam.class);

   public enum Type { ENUM, RANGE, BOOLEAN, UNSUPPORTED }

   private static final Pattern WORD_BREAK = Pattern.compile(
         "[_\\-\\s]+|(?<=[a-z])(?=[A-Z0-9])|(?<=[0-9])(?=[A-Za-z])|(?<=[A-Z])(?=[A-Z][a-z])"
   );

   private final String paramId;
   private final String name;
   private final String description;
   private final Type type;
   private final Map<String, String> values;
   private final Long min;
   private final Long max;
   private final String dflt;
   private final String value;
   private final String zwaveParamNo;
   private final String zwaveParamSize;

   /**
    * @param attribute the value of devsettings:params, which may be missing
    * @return the usable entries, empty if there are none
    */
   public static List<DeviceSettingParam> fromAttribute(Object attribute) {
      if(!(attribute instanceof Collection)) {
         return Collections.emptyList();
      }

      List<DeviceSettingParam> params = new ArrayList<>();
      for(Object entry: (Collection<?>) attribute) {
         Object paramId = entry instanceof Map ? ((Map<?, ?>) entry).get("paramId") : null;
         if(paramId == null) {
            logger.warn("Ignoring device setting without a paramId: {}", entry);
            continue;
         }
         params.add(new DeviceSettingParam(paramId.toString(), (Map<?, ?>) entry));
      }
      return params;
   }

   /**
    * Gson reads every number in devsettings:params as a Double, so 1 arrives as
    * 1.0 and a large value as 4.294967295E9. SetParam answers INVALID_VALUE to
    * those because it reads the value with Long.parseLong.
    */
   public static String toValue(Object raw) {
      if(raw == null) {
         return null;
      }
      if(raw instanceof Boolean) {
         return Boolean.TRUE.equals(raw) ? "1" : "0";
      }

      String text = raw.toString().trim();
      try {
         return new BigDecimal(text).stripTrailingZeros().toPlainString();
      }
      catch(NumberFormatException e) {
         return text;
      }
   }

   private DeviceSettingParam(String paramId, Map<?, ?> entry) {
      this.paramId = paramId;
      this.name = Objects.toString(entry.get("name"), null);
      this.description = Objects.toString(entry.get("description"), "");
      this.type = toType(entry.get("type"));
      this.values = toValues(entry.get("values"));
      this.min = toLong(entry.get("min"));
      this.max = toLong(entry.get("max"));
      this.dflt = toValue(entry.get("dflt"));
      this.value = toValue(entry.get("value"));
      this.zwaveParamNo = toValue(entry.get("zwaveParamNo"));
      this.zwaveParamSize = toValue(entry.get("zwaveParamSize"));
   }

   private static Type toType(Object raw) {
      try {
         return Type.valueOf(String.valueOf(raw).toUpperCase());
      }
      catch(IllegalArgumentException e) {
         return Type.UNSUPPORTED;
      }
   }

   private static Map<String, String> toValues(Object raw) {
      Map<String, String> values = new LinkedHashMap<>();
      if(raw instanceof Map) {
         ((Map<?, ?>) raw).forEach((key, label) -> values.put(String.valueOf(key), String.valueOf(label)));
      }
      return Collections.unmodifiableMap(values);
   }

   private static Long toLong(Object raw) {
      try {
         return raw == null ? null : Long.valueOf(toValue(raw));
      }
      catch(NumberFormatException e) {
         return null;
      }
   }

   public String getParamId() { return paramId; }
   public String getDescription() { return description; }

   /**
    * The description without the "Parameter 4: " drivers lead with, since the
    * detail line already gives the parameter number.
    */
   public String getDisplayDescription() {
      return description.replaceFirst("^Parameter \\d+:\\s*", "");
   }

   public Type getType() { return type; }
   /** Enum keys in driver order, mapped to their labels. */
   public Map<String, String> getValues() { return values; }
   public Long getMin() { return min; }
   public Long getMax() { return max; }
   public String getDefault() { return dflt; }
   public String getValue() { return value; }

   /**
    * Drivers currently send the id as the name, so turn it into words:
    * ledMode becomes Led Mode and btn1LedColor becomes Btn 1 Led Color.
    */
   public String getDisplayName() {
      if(StringUtils.isNotBlank(name) && !name.equals(paramId)) {
         return name;
      }

      List<String> words = new ArrayList<>();
      for(String word: WORD_BREAK.split(paramId)) {
         if(!word.isEmpty()) {
            words.add(StringUtils.capitalize(word));
         }
      }
      return String.join(" ", words);
   }

   /** The id and where the setting lives. Driver-only settings have no Z-Wave parameter. */
   public String getDetail() {
      StringBuilder detail = new StringBuilder(paramId);
      if(zwaveParamNo == null) {
         detail.append(", driver setting");
      }
      else {
         detail.append(", Z-Wave parameter ").append(zwaveParamNo);
      }
      if(zwaveParamSize != null) {
         detail.append(", size ").append(zwaveParamSize);
      }
      if(type == Type.RANGE && min != null && max != null) {
         detail.append(", ").append(min).append(" to ").append(max);
      }
      return detail.toString();
   }

   /**
    * The same rules the driver applies in DeviceSettingsParamDefinition.validateValue.
    * @return null if the driver should accept this value, otherwise the reason it won't
    */
   public String validate(String candidate) {
      if(candidate == null) {
         return "No value";
      }
      switch(type) {
      case ENUM:
         return values.containsKey(candidate) ? null : candidate + " is not a listed value";
      case BOOLEAN:
         return "0".equals(candidate) || "1".equals(candidate) ? null : "Must be 0 or 1";
      case RANGE:
         return validateRange(candidate);
      default:
         return "Unsupported setting type";
      }
   }

   private String validateRange(String candidate) {
      String required = "Must be a whole number";
      if(min != null && max != null) {
         required += " from " + min + " to " + max;
      }
      try {
         long number = Long.parseLong(candidate);
         boolean inRange = (min == null || number >= min) && (max == null || number <= max);
         return inRange ? null : required;
      }
      catch(NumberFormatException e) {
         return required;
      }
   }

   /** Text for a value of this param, keeping the raw value visible. */
   public String describe(String candidate) {
      if(candidate == null) {
         return "Not set";
      }
      switch(type) {
      case ENUM:
         return values.getOrDefault(candidate, "Unlisted") + " (" + candidate + ")";
      case BOOLEAN:
         return ("0".equals(candidate) ? "Off" : "On") + " (" + candidate + ")";
      default:
         return candidate;
      }
   }

   /** True if the two entries differ at most in their current value. */
   public boolean hasSameDefinition(DeviceSettingParam other) {
      return
            paramId.equals(other.paramId) &&
            Objects.equals(name, other.name) &&
            description.equals(other.description) &&
            type == other.type &&
            values.equals(other.values) &&
            Objects.equals(min, other.min) &&
            Objects.equals(max, other.max) &&
            Objects.equals(dflt, other.dflt) &&
            Objects.equals(zwaveParamNo, other.zwaveParamNo) &&
            Objects.equals(zwaveParamSize, other.zwaveParamSize);
   }
}

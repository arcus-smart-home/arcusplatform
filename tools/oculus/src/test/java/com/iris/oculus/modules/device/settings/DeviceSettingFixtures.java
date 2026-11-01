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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * devsettings:params entries in the form the client sees them, with every number a Double.
 */
final class DeviceSettingFixtures {

   static Map<String, Object> enumEntry(String paramId, double dflt, double value) {
      Map<String, Object> entry = entry(paramId, "enum", dflt, value);
      Map<String, String> values = new LinkedHashMap<>();
      values.put("0", "Always Off");
      values.put("1", "Always On");
      values.put("2", "Follows Switch");
      entry.put("values", values);
      return zwave(entry, 2, 1);
   }

   static Map<String, Object> rangeEntry(String paramId, double min, double max, double dflt, double value) {
      Map<String, Object> entry = entry(paramId, "range", dflt, value);
      entry.put("min", min);
      entry.put("max", max);
      return zwave(entry, 3, 1);
   }

   static Map<String, Object> booleanEntry(String paramId, double dflt, double value) {
      return zwave(entry(paramId, "boolean", dflt, value), 4, 1);
   }

   /** A setting the driver keeps itself, which has no Z-Wave parameter keys. */
   static Map<String, Object> driverOnlyEntry(String paramId, double min, double max, double dflt, double value) {
      Map<String, Object> entry = rangeEntry(paramId, min, max, dflt, value);
      entry.remove("zwaveParamNo");
      entry.remove("zwaveParamSize");
      return entry;
   }

   static DeviceSettingParam param(Map<String, Object> entry) {
      return DeviceSettingParam.fromAttribute(attribute(entry)).get(0);
   }

   /** The same definition with a different current value. */
   static DeviceSettingParam withValue(DeviceSettingParam param, Map<String, Object> entry, double value) {
      Map<String, Object> updated = new LinkedHashMap<>(entry);
      updated.put("value", value);
      return param(updated);
   }

   @SafeVarargs
   static List<Map<String, Object>> attribute(Map<String, Object>... entries) {
      return Arrays.asList(entries);
   }

   private static Map<String, Object> entry(String paramId, String type, double dflt, double value) {
      Map<String, Object> entry = new LinkedHashMap<>();
      entry.put("paramId", paramId);
      entry.put("name", paramId);
      entry.put("description", "Description of " + paramId);
      entry.put("type", type);
      entry.put("dflt", dflt);
      entry.put("value", value);
      return entry;
   }

   private static Map<String, Object> zwave(Map<String, Object> entry, double number, double size) {
      entry.put("zwaveParamNo", number);
      entry.put("zwaveParamSize", size);
      return entry;
   }

   private DeviceSettingFixtures() {
   }
}

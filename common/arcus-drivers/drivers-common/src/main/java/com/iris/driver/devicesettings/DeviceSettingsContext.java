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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Holds the parameter definitions for a driver's deviceSettings block.
 * Immutable once built.
 */
public class DeviceSettingsContext {

   private final List<DeviceSettingsParamDefinition> params;
   private final Map<String, DeviceSettingsParamDefinition> byParamId;
   private final Map<Integer, DeviceSettingsParamDefinition> byZwaveParamNo;

   public DeviceSettingsContext(List<DeviceSettingsParamDefinition> params) {
      this.params = Collections.unmodifiableList(new ArrayList<>(params));
      Map<String, DeviceSettingsParamDefinition> idMap = new LinkedHashMap<>();
      Map<Integer, DeviceSettingsParamDefinition> noMap = new LinkedHashMap<>();
      for (DeviceSettingsParamDefinition p : params) {
         idMap.put(p.getParamId(), p);
         noMap.put(p.getZwaveParamNo(), p);
      }
      this.byParamId = Collections.unmodifiableMap(idMap);
      this.byZwaveParamNo = Collections.unmodifiableMap(noMap);
   }

   public List<DeviceSettingsParamDefinition> getParams() {
      return params;
   }

   public DeviceSettingsParamDefinition getByParamId(String paramId) {
      return byParamId.get(paramId);
   }

   public DeviceSettingsParamDefinition getByZwaveParamNo(int paramNo) {
      return byZwaveParamNo.get(paramNo);
   }

   /**
    * Builds the client-facing list for the devsettings:params attribute.
    * Merges static metadata with current values from the variable lookup.
    *
    * @param varLookup function that takes a variable key (e.g. "devsettings:ledMode")
    *                  and returns the current value, or null if not set
    */
   public List<Map<String, Object>> buildParamsAttribute(Function<String, Object> varLookup) {
      List<Map<String, Object>> result = new ArrayList<>(params.size());
      for (DeviceSettingsParamDefinition param : params) {
         Map<String, Object> entry = param.toMap();
         Object currentValue = varLookup.apply(param.getVariableKey());
         if (currentValue != null) {
            entry.put("value", currentValue);
         } else {
            entry.put("value", param.getDefaultValue());
         }
         result.add(entry);
      }
      return result;
   }
}

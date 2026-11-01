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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Holds the parameter definitions for a driver's deviceSettings block.
 * Immutable once built.
 */
public class DeviceSettingsContext {

   private static final long UNKNOWN_PRODUCT = -1L;

   private final List<DeviceSettingsParamDefinition> params;
   private final Map<String, DeviceSettingsParamDefinition> byParamId;
   private final Map<Integer, DeviceSettingsParamDefinition> byZwaveParamNo;
   private final boolean productFilters;
   private final boolean readOnConnect;
   private final Map<Long, DeviceSettingsContext> byProduct = new ConcurrentHashMap<>();

   public DeviceSettingsContext(List<DeviceSettingsParamDefinition> params) {
      this(params, true);
   }

   /**
    * @param readOnConnect false when the driver reads the values itself, later than
    *                      connect (see {@link #isReadOnConnect()})
    * @throws IllegalArgumentException if two params share a name, or two params
    *         that can apply to one product share a parameter number
    */
   public DeviceSettingsContext(List<DeviceSettingsParamDefinition> params, boolean readOnConnect) {
      this.params = Collections.unmodifiableList(new ArrayList<>(params));
      this.readOnConnect = readOnConnect;
      Map<String, DeviceSettingsParamDefinition> idMap = new LinkedHashMap<>();
      Map<Integer, DeviceSettingsParamDefinition> noMap = new LinkedHashMap<>();
      Set<Integer> reused = new HashSet<>();
      boolean filters = false;
      for (int i = 0; i < this.params.size(); i++) {
         DeviceSettingsParamDefinition p = this.params.get(i);
         // names key the stored values
         if (idMap.put(p.getParamId(), p) != null) {
            throw new IllegalArgumentException("deviceSettings param name '" + p.getParamId() + "' is declared twice");
         }
         filters |= p.hasProductFilter();
         if (p.isDriverOnly()) {
            // kept by the driver, so it has no parameter number to clash with or be found by
            continue;
         }
         for (int j = 0; j < i; j++) {
            DeviceSettingsParamDefinition q = this.params.get(j);
            if (!q.isDriverOnly() && q.getZwaveParamNo() == p.getZwaveParamNo()) {
               if (p.sharesProductWith(q)) {
                  throw new IllegalArgumentException("deviceSettings params '" + q.getParamId() + "' and '"
                        + p.getParamId() + "' both use parameter " + p.getZwaveParamNo() + " on the same product");
               }
               reused.add(p.getZwaveParamNo());
            }
         }
         noMap.put(p.getZwaveParamNo(), p);
      }
      // A number used by different products means nothing until narrowed with forProduct
      noMap.keySet().removeAll(reused);
      this.byParamId = Collections.unmodifiableMap(idMap);
      this.byZwaveParamNo = Collections.unmodifiableMap(noMap);
      this.productFilters = filters;
   }

   public List<DeviceSettingsParamDefinition> getParams() {
      return params;
   }

   public DeviceSettingsParamDefinition getByParamId(String paramId) {
      return byParamId.get(paramId);
   }

   /**
    * Returns the param with this Z-Wave parameter number, or null. A number that
    * different products use for different params only resolves after
    * {@link #forProduct}.
    */
   public DeviceSettingsParamDefinition getByZwaveParamNo(int paramNo) {
      return byZwaveParamNo.get(paramNo);
   }

   /**
    * True when some parameter only exists on some of the driver's products.
    */
   public boolean hasProductFilters() {
      return productFilters;
   }

   /**
    * False when the driver asked to read the values itself instead of on connect,
    * e.g. a lock that drops commands sent while it is still being set up.
    */
   public boolean isReadOnConnect() {
      return readOnConnect;
   }

   /**
    * Returns the parameters that exist on one Z-Wave product. Lookups by parameter
    * number must go through this when two products reuse a number for different
    * settings. Returns this context when no parameter is product specific.
    */
   public DeviceSettingsContext forProduct(Integer productType, Integer productId) {
      if (!productFilters) {
         return this;
      }
      long key = productType == null || productId == null
            ? UNKNOWN_PRODUCT
            : DeviceSettingsParamDefinition.productKey(productType, productId) & 0xFFFFFFFFL;
      return byProduct.computeIfAbsent(key, k -> {
         List<DeviceSettingsParamDefinition> applicable = new ArrayList<>(params.size());
         for (DeviceSettingsParamDefinition p : params) {
            if (p.appliesTo(productType, productId)) {
               applicable.add(p);
            }
         }
         return new DeviceSettingsContext(applicable, readOnConnect);
      });
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
            entry.put("value", param.decodeStored(currentValue));
         } else {
            entry.put("value", param.getDefaultValue());
         }
         result.add(entry);
      }
      return result;
   }
}

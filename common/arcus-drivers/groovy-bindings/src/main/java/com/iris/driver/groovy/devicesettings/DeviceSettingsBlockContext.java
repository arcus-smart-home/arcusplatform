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
package com.iris.driver.groovy.devicesettings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import groovy.lang.GroovyObjectSupport;

import com.iris.driver.devicesettings.DeviceSettingsContext;
import com.iris.driver.devicesettings.DeviceSettingsParamDefinition;

/**
 * Closure delegate for the {@code deviceSettings { }} DSL block.
 * Collects param() calls and builds a DeviceSettingsContext.
 */
public class DeviceSettingsBlockContext extends GroovyObjectSupport {

   private final List<DeviceSettingsParamDefinition> params = new ArrayList<>();

   /**
    * Called from the driver DSL:
    * <pre>
    * param name: 'ledMode', label: 'LED indicator', id: 27, size: 1,
    *     type: 'enum', values: ['0': 'Always On', '1': 'On for 5 seconds'],
    *     description: 'Parameter 27: LED indicator behavior',
    *     dflt: 1
    *
    * param name: 'rampRate', label: 'Ramp rate', unit: 's', id: 9, size: 1,
    *     type: 'range', min: 0, max: 99,
    *     description: 'Parameter 9: Ramp rate (seconds, 0=instant)',
    *     dflt: 1
    * </pre>
    * {@code label} is the short name clients show, such as Home Assistant beside a
    * slider; {@code description} is the detail. {@code unit} is the unit of a range
    * value as clients display it, such as {@code 's'}, {@code 'min'} or {@code '%'};
    * leave it out when special values don't share it (e.g. 255 = "same as the
    * paddle").
    * <p>
    * Add {@code products: [[0x4F44, 0x3032], ...]} (Z-Wave product type, product id)
    * when the parameter only exists on some of the products the driver matches.
    * <p>
    * A setting that isn't a Z-Wave configuration parameter, such as a sound the
    * driver sends in another command, is declared {@code driverOnly: true} with
    * no {@code id} or {@code size}. The driver stores it and never asks the device.
    */
   @SuppressWarnings("unchecked")
   public void param(Map<String, Object> args) {
      String name = (String) args.get("name");
      if (name == null || name.isEmpty()) {
         throw new IllegalArgumentException("deviceSettings param requires a 'name'");
      }

      boolean driverOnly = Boolean.TRUE.equals(args.get("driverOnly"));
      Number id = (Number) args.get("id");
      Number size = (Number) args.get("size");
      if (driverOnly && (id != null || size != null)) {
         throw new IllegalArgumentException("deviceSettings param '" + name + "' is driverOnly, so it has no Z-Wave parameter 'id' or 'size'");
      }
      if (id == null && !driverOnly) {
         throw new IllegalArgumentException("deviceSettings param '" + name + "' requires an 'id' (Z-Wave parameter number)");
      }

      String typeStr = (String) args.get("type");
      String description = (String) args.get("description");
      String label = text(args, "label", name);
      String unit = text(args, "unit", name);
      Number dflt = (Number) args.get("dflt");

      DeviceSettingsParamDefinition.ParamType type;
      if ("enum".equalsIgnoreCase(typeStr)) {
         type = DeviceSettingsParamDefinition.ParamType.ENUM;
      } else if ("boolean".equalsIgnoreCase(typeStr)) {
         type = DeviceSettingsParamDefinition.ParamType.BOOLEAN;
      } else {
         type = DeviceSettingsParamDefinition.ParamType.RANGE;
      }

      DeviceSettingsParamDefinition.Builder builder = DeviceSettingsParamDefinition.builder()
            .withParamId(name)
            .withDriverOnly(driverOnly)
            .withType(type);

      if (id != null) {
         builder.withZwaveParamNo(id.intValue());
      }

      if (size != null) {
         builder.withZwaveParamSize(size.intValue());
      }
      if (description != null) {
         builder.withDescription(description);
      }
      builder.withLabel(label).withUnit(unit);
      if (dflt != null) {
         builder.withDefaultValue(dflt.longValue());
      }

      if (type == DeviceSettingsParamDefinition.ParamType.ENUM) {
         Object values = args.get("values");
         if (values instanceof Map) {
            Map<String, String> enumValues = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) values).entrySet()) {
               enumValues.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
            builder.withEnumValues(enumValues);
         }
      } else if (type == DeviceSettingsParamDefinition.ParamType.RANGE) {
         Number min = (Number) args.get("min");
         Number max = (Number) args.get("max");
         if (min != null) {
            builder.withMin(min.longValue());
         }
         if (max != null) {
            builder.withMax(max.longValue());
         }
      } else if (type == DeviceSettingsParamDefinition.ParamType.BOOLEAN) {
         builder.withMin(0).withMax(1);
      }

      Object products = args.get("products");
      if (products != null) {
         builder.withProducts(parseProducts(name, products));
      }

      params.add(builder.build());
   }

   private static String text(Map<String, Object> args, String key, String name) {
      Object value = args.get(key);
      if (value != null && !(value instanceof CharSequence)) {
         throw new IllegalArgumentException("deviceSettings param '" + name + "' " + key + " must be a string, got " + value);
      }
      return value == null ? null : value.toString();
   }

   private static List<int[]> parseProducts(String name, Object products) {
      if (!(products instanceof List) || ((List<?>) products).isEmpty()) {
         throw new IllegalArgumentException("deviceSettings param '" + name
               + "' products must be a non-empty list of [productType, productId] pairs");
      }
      List<int[]> pairs = new ArrayList<>();
      for (Object entry : (List<?>) products) {
         if (!(entry instanceof List) || ((List<?>) entry).size() != 2
               || !(((List<?>) entry).get(0) instanceof Number)
               || !(((List<?>) entry).get(1) instanceof Number)) {
            throw new IllegalArgumentException("deviceSettings param '" + name
                  + "' has a products entry that is not a [productType, productId] pair: " + entry);
         }
         List<?> pair = (List<?>) entry;
         pairs.add(new int[] { ((Number) pair.get(0)).intValue(), ((Number) pair.get(1)).intValue() });
      }
      return pairs;
   }

   public DeviceSettingsContext build() {
      return build(true);
   }

   public DeviceSettingsContext build(boolean readOnConnect) {
      return new DeviceSettingsContext(params, readOnConnect);
   }
}

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
    * param name: 'ledMode', id: 27, size: 1,
    *     type: 'enum', values: ['0': 'Always On', '1': 'On for 5 seconds'],
    *     description: 'LED indicator behavior',
    *     dflt: 1
    * </pre>
    */
   @SuppressWarnings("unchecked")
   public void param(Map<String, Object> args) {
      String name = (String) args.get("name");
      if (name == null || name.isEmpty()) {
         throw new IllegalArgumentException("deviceSettings param requires a 'name'");
      }

      Number id = (Number) args.get("id");
      if (id == null) {
         throw new IllegalArgumentException("deviceSettings param '" + name + "' requires an 'id' (Z-Wave parameter number)");
      }

      Number size = (Number) args.get("size");
      String typeStr = (String) args.get("type");
      String description = (String) args.get("description");
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
            .withZwaveParamNo(id.intValue())
            .withType(type);

      if (size != null) {
         builder.withZwaveParamSize(size.intValue());
      }
      if (description != null) {
         builder.withDescription(description);
      }
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

      params.add(builder.build());
   }

   public DeviceSettingsContext build() {
      return new DeviceSettingsContext(params);
   }
}

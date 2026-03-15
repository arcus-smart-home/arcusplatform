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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.devicesettings.DeviceSettingsContext;
import com.iris.driver.handler.GetAttributesProvider;
import com.iris.messages.capability.DeviceSettingsCapability;

/**
 * Provides the devsettings:params attribute by merging static parameter
 * metadata with current values from driver variables.
 */
public class DeviceSettingsGetAttributesProvider implements GetAttributesProvider {

   private final DeviceSettingsContext settingsContext;

   public DeviceSettingsGetAttributesProvider(DeviceSettingsContext settingsContext) {
      this.settingsContext = settingsContext;
   }

   @Override
   public String getNamespace() {
      return DeviceSettingsCapability.NAMESPACE;
   }

   @Override
   public Map<String, Object> getAttributes(DeviceDriverContext context, Set<String> names) {

      Map<String, Object> result = new HashMap<>(1);
      result.put(DeviceSettingsCapability.ATTR_PARAMS,
            settingsContext.buildParamsAttribute(key -> {
               try {
                  return context.getVariable(key);
               } catch (Exception e) {
                  return null;
               }
            }));
      return result;
   }
}

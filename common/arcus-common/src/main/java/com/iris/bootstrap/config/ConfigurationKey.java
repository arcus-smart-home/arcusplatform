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
package com.iris.bootstrap.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A configuration key that supports variable substitution.
 *
 * Drop-in replacement for com.netflix.governator.configuration.ConfigurationKey.
 */
public class ConfigurationKey {
   private final String rawKey;
   private final List<String> parts;

   public ConfigurationKey(String rawKey, List<String> parts) {
      this.rawKey = rawKey;
      this.parts = parts;
   }

   public String getRawKey() {
      return rawKey;
   }

   public String getKey(Map<String, String> variables) {
      if (parts.size() == 1) {
         return parts.get(0);
      }

      StringBuilder sb = new StringBuilder();
      for (String part : parts) {
         if (part.startsWith("${") && part.endsWith("}")) {
            String varName = part.substring(2, part.length() - 1);
            String value = variables.get(varName);
            sb.append(value != null ? value : part);
         } else {
            sb.append(part);
         }
      }
      return sb.toString();
   }

   @Override
   public String toString() {
      return rawKey;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ConfigurationKey that = (ConfigurationKey) o;
      return rawKey.equals(that.rawKey);
   }

   @Override
   public int hashCode() {
      return rawKey.hashCode();
   }
}

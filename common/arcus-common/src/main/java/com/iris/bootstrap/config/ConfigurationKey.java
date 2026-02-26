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

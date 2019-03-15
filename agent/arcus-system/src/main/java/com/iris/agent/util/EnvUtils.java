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
package com.iris.agent.util;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

public final class EnvUtils {
   private static final boolean isDevMode = getConfigAsBoolean("iris.agent.dev.mode", false);

   public static boolean isDevMode() {
      return isDevMode;
   }

   public static boolean isDevTraceEnabled(Logger log) {
      return isDevMode() && log.isTraceEnabled();
   }

   public static void devTrace(Logger log, String format) {
      if (isDevMode()) {
         log.trace(format);
      }
   }

   public static void devTrace(Logger log, String format, Object arg1) {
      if (isDevMode()) {
         log.trace(format, arg1);
      }
   }

   public static void devTrace(Logger log, String format, Object arg1, Object arg2) {
      if (isDevMode()) {
         log.trace(format, arg1, arg2);
      }
   }

   public static void devTrace(Logger log, String format, Object... args) {
      if (isDevMode()) {
         log.trace(format, args);
      }
   }

   private static boolean getConfigAsBoolean(String name, boolean def) {
      return getConfig(name, def ? "true" : null) != null;
   }

   private static String getConfig(String name, String def) {
      String envName = name.toUpperCase().replace('.', '_');
      
      String result = System.getenv(envName);
      if (!StringUtils.isBlank(result)) {
         return result;
      }

      result = System.getenv(name);
      if (!StringUtils.isBlank(result)) {
         return result;
      }

      result = System.getProperty(name);
      if (!StringUtils.isBlank(result)) {
         return result;
      }

      return def;
   }
}


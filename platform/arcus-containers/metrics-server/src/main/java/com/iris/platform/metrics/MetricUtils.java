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
package com.iris.platform.metrics;

import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class MetricUtils {
   public static final String TS = "ts";
   public static final String HOST = "hst";
   public static final String CONTAINER = "ctn";
   public static final String SERVICE = "svc";
   public static final String VERSION = "svr";

   private MetricUtils() {
   }

   @Nullable
   public static String getAsStringOrNull(JsonObject object, String name) {
      JsonElement elem = object.get(name);
      if (elem == null || elem.isJsonNull()) {
         return null;
      }

      return elem.getAsString();
   }

   @Nullable
   public static JsonArray getAsArrayOrNull(JsonObject object, String name) {
      JsonElement elem = object.get(name);
      if (elem == null || elem.isJsonNull()) {
         return null;
      }

      return elem.getAsJsonArray();
   }

   public static JsonObject getTags(@Nullable String host, @Nullable String container, @Nullable String service, @Nullable String version) {
      JsonObject obj = new JsonObject();
      if (host != null) {
         obj.addProperty("host", host);
      }

      if (container != null) {
         obj.addProperty("container", container);
      }

      if (service != null) {
         obj.addProperty("service", service);
      }

      if (version != null) {
         obj.addProperty("version", version);
      }

      return obj;
   }
}


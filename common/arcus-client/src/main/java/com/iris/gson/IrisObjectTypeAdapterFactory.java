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
/**
 *
 */
package com.iris.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

/**
 *
 */
public class IrisObjectTypeAdapterFactory implements TypeAdapterFactory {

   /**
    * No-op for backwards compatibility. The reflection hack is no longer needed
    * since Gson 2.8.9+ supports ToNumberPolicy.LONG_OR_DOUBLE which handles
    * Long deserialization without rounding errors.
    */
   public static void install() {
      // no longer needed
   }

   @SuppressWarnings("unchecked")
   @Override
   public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
      if (type.getRawType() == Object.class) {
         return (TypeAdapter<T>) new IrisObjectTypeAdapter(gson);
      } else {
         return null;
      }
   }
}

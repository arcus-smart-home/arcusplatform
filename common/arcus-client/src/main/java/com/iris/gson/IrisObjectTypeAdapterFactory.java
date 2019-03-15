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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.bind.ObjectTypeAdapter;
import com.google.gson.reflect.TypeToken;

/**
 *
 */
public class IrisObjectTypeAdapterFactory implements TypeAdapterFactory {

   /**
    * GSON will not allows the default Object type adapter to be replaced,
    * so this method reflectively replaces the reference to the global
    * Object type adapter.
    */
   public static void install() {
      // de-reference and throw if an error
      if(IrisObjectTypeAdapterInstaller.FAILURE != null) {
         throw new IllegalStateException("Unable to install type adapter factory", IrisObjectTypeAdapterInstaller.FAILURE);
      }
   }

   /* (non-Javadoc)
    * @see com.google.gson.TypeAdapterFactory#create(com.google.gson.Gson, com.google.gson.reflect.TypeToken)
    */
   @SuppressWarnings("unchecked")
   @Override
   public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
      if (type.getRawType() == Object.class) {
         return (TypeAdapter<T>) new IrisObjectTypeAdapter(gson);
      } else {
         return null;
      }
   }

   private static class IrisObjectTypeAdapterInstaller {
      private static final Throwable FAILURE;

      static {
         Throwable cause = null;
         try {
            Field field = ObjectTypeAdapter.class.getDeclaredField("FACTORY");
            field.setAccessible(true);

            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            field.set(null, new IrisObjectTypeAdapterFactory());
         }
         catch(Throwable e) {
            cause = e;
         }
         FAILURE = cause;
      }
   }
}


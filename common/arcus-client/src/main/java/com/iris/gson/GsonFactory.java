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
package com.iris.gson;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;

public class GsonFactory {

   private Gson gson;

   public GsonFactory(
         Set<TypeAdapterFactory> typeAdapterFactories,
         Set<TypeAdapter<?>> typeAdapters,
         Set<JsonSerializer<?>> serializers,
         Set<JsonDeserializer<?>> deserializers
   ) {
      this(typeAdapterFactories, typeAdapters, serializers, deserializers, true);
   }

   public GsonFactory(
         Set<TypeAdapterFactory> typeAdapterFactories,
         Set<TypeAdapter<?>> typeAdapters,
         Set<JsonSerializer<?>> serializers,
         Set<JsonDeserializer<?>> deserializers,
         boolean serializeNulls
   ) {
      this.gson = create(typeAdapterFactories, typeAdapters, serializers, deserializers, serializeNulls);
   }

   public Gson get() {
      return gson;
   }

   private static Gson create(
         Set<TypeAdapterFactory> typeAdapterFactories,
         Set<TypeAdapter<?>> typeAdapters,
         Set<JsonSerializer<?>> serializers,
         Set<JsonDeserializer<?>> deserializers,
         boolean serializeNulls
   ) {
      IrisObjectTypeAdapterFactory.install();

      GsonBuilder builder = new GsonBuilder();
      if (serializeNulls) {
         builder.serializeNulls();
      }

      builder.disableHtmlEscaping();
      builder.registerTypeAdapter(Date.class, new DateTypeAdapter());

      if(typeAdapterFactories != null) {
         for(TypeAdapterFactory factory : typeAdapterFactories) {
            builder.registerTypeAdapterFactory(factory);
         }
      }

      if(typeAdapters != null) {
         for(TypeAdapter<?> adapter : typeAdapters) {
            builder.registerTypeAdapter(extractType(adapter), adapter);
         }
      }

      if(serializers != null) {
         for(JsonSerializer<?> serializer : serializers) {
            builder.registerTypeAdapter(extractType(serializer, JsonSerializer.class), serializer);
         }
      }

      if(deserializers != null) {
         for(JsonDeserializer<?> deserializer : deserializers) {
            builder.registerTypeAdapter(extractType(deserializer, JsonDeserializer.class), deserializer);
         }
      }

      return builder.create();
   }

   private static Type extractType(TypeAdapter<?> adapter) {
      return ((ParameterizedType) adapter.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
   }

   private static <T> Type extractType(T object, Class<T> iface) {
      for(Type gIface: object.getClass().getGenericInterfaces()) {
         if(
               gIface instanceof ParameterizedType &&
               ((ParameterizedType) gIface).getRawType().equals(iface)
         ) {
            return ((ParameterizedType) gIface).getActualTypeArguments()[0];
         }
      }
      throw new IllegalArgumentException("Unable to determine generic type of [" + object + "].");
   }
}


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
package com.iris.io.json.gson;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * 
 */
public class PolymorphicTypeAdapterFactory<O> implements TypeAdapterFactory {
   private final Class<O> baseType;
   private final ConcurrentMap<String, Class<?>> cache;

   public PolymorphicTypeAdapterFactory(Class<O> baseType) {
      this.baseType = baseType;
      // we expect new entries in this map rarely, so keep concurrency at 1
      this.cache = new ConcurrentHashMap<String, Class<?>>(16, .75f, 1);
   }
   
   @SuppressWarnings({ "unchecked", "rawtypes" })
   @Override
   public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
      if(!baseType.isAssignableFrom(type.getRawType())) {
         return null;
      }
      
      // we just checked that T extends O, but no way to tell the compiler that
      return new ReflectiveTypeAdapter(gson, this, type);
   }

   protected String typeOf(O value) {
      return value.getClass().getName();
   }
   
   protected Class<?> classOf(String type) {
      try {
         Class<?> c = Class.forName(type);
         if(!baseType.isAssignableFrom(c)) {
            throw new IllegalArgumentException("Invalid type: " + type + " not an instance of " + baseType);
         }
         return c;
      }
      catch(ClassNotFoundException e) {
         throw new JsonParseException("Unable to determine class of " + type, e);
      }
   }
   
   private class ReflectiveTypeAdapter<T extends O> extends TypeAdapter<T> {
      private final Gson gson;
      private final TypeToken<T> token;
      
      public ReflectiveTypeAdapter(
            Gson gson, 
            TypeAdapterFactory skipPast, 
            TypeToken<T> token
      ) {
         this.gson = gson;
         this.token = token;
      }

      /* (non-Javadoc)
       * @see com.google.gson.TypeAdapter#write(com.google.gson.stream.JsonWriter, java.lang.Object)
       */
      @Override
      public void write(JsonWriter out, T value) throws IOException {
         if(value == null) {
            out.nullValue();
            return;
         }
         
         out.beginArray();
         out.value(typeOf(value));
         gson.getDelegateAdapter(PolymorphicTypeAdapterFactory.this, token).write(out, value);
         out.endArray();
      }

      /* (non-Javadoc)
       * @see com.google.gson.TypeAdapter#read(com.google.gson.stream.JsonReader)
       */
      @Override
      public T read(JsonReader in) throws IOException {
         if(in.peek().equals(JsonToken.NULL)) {
            return null;
         }
         
         in.beginArray();
         String type = in.nextString();
         Class<?> c = cache.get(type);
         c = classOf(type);
         cache.put(type, c);
         T value = (T) gson.getDelegateAdapter(PolymorphicTypeAdapterFactory.this, TypeToken.get(c)).read(in);
         in.endArray();
         return value;
      }

   }
}


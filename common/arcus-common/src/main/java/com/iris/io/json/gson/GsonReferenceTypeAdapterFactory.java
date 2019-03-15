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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.inject.Singleton;
import com.iris.type.LooselyTypedReference;

/**
 * 
 */
@Singleton
public class GsonReferenceTypeAdapterFactory implements TypeAdapterFactory {
   
   /* (non-Javadoc)
    * @see com.google.gson.TypeAdapterFactory#create(com.google.gson.Gson, com.google.gson.reflect.TypeToken)
    */
   @Override
   public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
      if(type.getType().equals(LooselyTypedReference.class)) {
         return (TypeAdapter<T>) new GsonReferenceTypeAdapter(gson);
      }
      return null;
   }

   private class GsonReferenceTypeAdapter extends TypeAdapter<LooselyTypedReference> {
      private final Gson gson;

      GsonReferenceTypeAdapter(Gson gson) {
         this.gson = gson;
      }
      
      @Override
      public void write(JsonWriter out, LooselyTypedReference value)
            throws IOException {
         gson.getAdapter(JsonElement.class).write(out, value.as(JsonElement.class));
      }

      @Override
      public LooselyTypedReference read(JsonReader in) throws IOException {
         JsonElement element = gson.getAdapter(JsonElement.class).read(in);
         return GsonReference.wrap(gson, element);
      }
   }
}


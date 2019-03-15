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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.iris.io.Deserializer;
import com.iris.util.TypeMarker;

/**
 * 
 */
public class GsonDeserializer<T> implements Deserializer<T> {
   private final Gson gson;
   private final Type type;
   
   public GsonDeserializer(Gson gson, Class<T> type) {
      this.gson = gson;
      this.type = type;
   }

   public GsonDeserializer(Gson gson, TypeMarker<T> marker) {
      this.gson = gson;
      this.type = marker.getType();
   }

   /* (non-Javadoc)
    * @see com.iris.io.Deserializer#deserialize(byte[])
    */
   @Override
   public T deserialize(byte[] input) throws IllegalArgumentException {
      return gson.fromJson(new String(input, Charsets.UTF_8), type);
   }

   /* (non-Javadoc)
    * @see com.iris.io.Deserializer#deserialize(java.io.InputStream)
    */
   @Override
   public T deserialize(InputStream input) throws IOException, IllegalArgumentException {
      return gson.fromJson(new InputStreamReader(input, Charsets.UTF_8), type);
   }

}


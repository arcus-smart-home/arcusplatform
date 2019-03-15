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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.iris.io.Serializer;
import com.iris.util.TypeMarker;

/**
 * 
 */
public class GsonSerializer<T> implements Serializer<T> {
   private final Gson gson;
   private final Type type;
   
   public GsonSerializer(Gson gson) {
      this.gson = gson;
      this.type = null;
   }
   
   public GsonSerializer(Gson gson, Class<T> type) {
      this.gson = gson;
      this.type = type;
   }

   public GsonSerializer(Gson gson, TypeMarker<T> marker) {
      this.gson = gson;
      this.type = marker.getType();
   }

   @Override
   public byte[] serialize(T value) throws IllegalArgumentException {
      String json;
      if(type == null) {
         json = gson.toJson(value);
      }
      else {
         json = gson.toJson(value, type);
      }
      return json.getBytes(Charsets.UTF_8);
   }

   @Override
   public void serialize(T value, OutputStream out) throws IOException, IllegalArgumentException {
      OutputStreamWriter osw = new OutputStreamWriter(out, Charsets.UTF_8);
      if(type == null) {
         gson.toJson(value, osw);
      }
      else {
         gson.toJson(value, type, osw);
      }
      // we didn't open it, don't close it, but make sure if this was a successful write all bytes are flushed 
      osw.flush();
   }
   
}


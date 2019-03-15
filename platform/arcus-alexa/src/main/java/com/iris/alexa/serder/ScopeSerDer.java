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
package com.iris.alexa.serder;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.iris.alexa.message.Scope;

class ScopeSerDer implements JsonSerializer<Scope>, JsonDeserializer<Scope> {

   private static final String ATTR_TYPE = "type";
   private static final String ATTR_TOKEN = "token";

   @Override
   public Scope deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      JsonObject obj = json.getAsJsonObject();
      return new Scope(
         obj.get(ATTR_TYPE).getAsString(),
         obj.get(ATTR_TOKEN).getAsString()
      );
   }

   @Override
   public JsonElement serialize(Scope src, Type typeOfSrc, JsonSerializationContext context) {
      JsonObject obj = new JsonObject();
      obj.addProperty(ATTR_TYPE, src.getType());
      obj.addProperty(ATTR_TOKEN, src.getToken());
      return obj;
   }
}


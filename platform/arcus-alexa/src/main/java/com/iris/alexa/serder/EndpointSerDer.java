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
import java.util.Map;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.iris.alexa.message.Endpoint;
import com.iris.alexa.message.Scope;

class EndpointSerDer implements JsonSerializer<Endpoint>, JsonDeserializer<Endpoint> {

   private static final TypeToken<Map<String, String>> COOKIE_TYPE = new TypeToken<Map<String, String>>() {};

   private static final String ATTR_SCOPE = "scope";
   private static final String ATTR_ENDPOINTID = "endpointId";
   private static final String ATTR_COOKIE = "cookie";

   @Override
   public Endpoint deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      JsonObject obj = json.getAsJsonObject();
      return new Endpoint(
         context.deserialize(obj.get(ATTR_SCOPE), Scope.class),
         obj.get(ATTR_ENDPOINTID).getAsString(),
         context.deserialize(obj.get(ATTR_COOKIE), COOKIE_TYPE.getType())
      );
   }

   @Override
   public JsonElement serialize(Endpoint src, Type typeOfSrc, JsonSerializationContext context) {
      JsonObject obj = new JsonObject();
      if(src.getScope() != null) {
         obj.add(ATTR_SCOPE, context.serialize(src.getScope(), Scope.class));
      }
      obj.addProperty(ATTR_ENDPOINTID, src.getEndpointId());
      if(src.getCookie() != null) {
         obj.add(ATTR_COOKIE, context.serialize(src.getCookie(), COOKIE_TYPE.getType()));
      }
      return obj;
   }
}


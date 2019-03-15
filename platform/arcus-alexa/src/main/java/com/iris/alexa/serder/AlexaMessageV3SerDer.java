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
import com.iris.alexa.message.AlexaMessage;
import com.iris.alexa.message.Endpoint;
import com.iris.alexa.message.Header;

class AlexaMessageV3SerDer implements JsonDeserializer<AlexaMessage>, JsonSerializer<AlexaMessage> {

   private static final TypeToken<Map<String, Object>> PAYLOAD_TYPE = new TypeToken<Map<String, Object>>() {};
   private static final String ATTR_ENDPOINT = "endpoint";

   @Override
   public AlexaMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      try {
         JsonObject obj = json.getAsJsonObject().getAsJsonObject(SerDer.ATTR_DIRECTIVE);
         Header h = context.deserialize(obj.get(SerDer.ATTR_HEADER), Header.class);
         Map<String, Object> p = context.deserialize(obj.get(SerDer.ATTR_PAYLOAD), PAYLOAD_TYPE.getType());
         Endpoint e = context.deserialize(obj.get(ATTR_ENDPOINT), Endpoint.class);
         return new AlexaMessage(h, p, e, null);
      } catch(Exception e) {
         throw new JsonParseException(e);
      }
   }

   @Override
   public JsonElement serialize(AlexaMessage src, Type typeOfSrc, JsonSerializationContext context) {
      JsonObject event = new JsonObject();
      event.add(SerDer.ATTR_HEADER, context.serialize(src.getHeader(), Header.class));
      event.add(SerDer.ATTR_PAYLOAD, context.serialize(src.getPayload(), PAYLOAD_TYPE.getType()));
      if(src.getEndpoint() != null) {
         event.add(ATTR_ENDPOINT, context.serialize(src.getEndpoint(), Endpoint.class));
      }

      JsonObject response = new JsonObject();
      response.add("event", event);

      if(src.getContext() != null) {
         response.add("context", context.serialize(src.getContext()));
      }

      return response;
   }
}


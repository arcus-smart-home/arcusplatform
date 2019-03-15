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
import com.iris.alexa.message.AlexaMessage;

class AlexaMessageFacadeSerDer implements JsonDeserializer<AlexaMessage>, JsonSerializer<AlexaMessage> {

   private static final AlexaMessageV2SerDer v2 = new AlexaMessageV2SerDer();
   private static final AlexaMessageV3SerDer v3 = new AlexaMessageV3SerDer();

   @Override
   public AlexaMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      JsonObject obj = json.getAsJsonObject();
      if(obj.has(SerDer.ATTR_DIRECTIVE)) {
         return v3.deserialize(json, typeOfT, context);
      }
      return v2.deserialize(json, typeOfT, context);
   }

   @Override
   public JsonElement serialize(AlexaMessage src, Type typeOfSrc, JsonSerializationContext context) {
      if(src.getHeader().isV2()) {
         return v2.serialize(src, typeOfSrc, context);
      }
      if(src.getHeader().isV3()) {
         return v3.serialize(src, typeOfSrc, context);
      }
      throw new IllegalArgumentException("Invalid payload version " + src.getHeader().getPayloadVersion());
   }
}


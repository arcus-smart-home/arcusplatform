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
import com.iris.alexa.message.Header;

class HeaderSerDer implements JsonSerializer<Header>, JsonDeserializer<Header> {

   private static final String ATTR_CORRELATION_TOKEN = "correlationToken";
   private static final String ATTR_MESSAGE_ID = "messageId";
   private static final String ATTR_NAME = "name";
   private static final String ATTR_NAMESPACE = "namespace";
   private static final String ATTR_PAYLOAD_VERSION = "payloadVersion";

   @Override
   public Header deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      JsonObject obj = json.getAsJsonObject();
      JsonElement token = obj.get(ATTR_CORRELATION_TOKEN);
      return new Header(
         obj.get(ATTR_MESSAGE_ID).getAsString(),
         obj.get(ATTR_NAME).getAsString(),
         obj.get(ATTR_NAMESPACE).getAsString(),
         obj.get(ATTR_PAYLOAD_VERSION).getAsString(),
         token == null || token.isJsonNull() ? null : token.getAsString()
      );
   }

   @Override
   public JsonElement serialize(Header src, Type typeOfSrc, JsonSerializationContext context) {
      JsonObject obj = new JsonObject();
      obj.addProperty(ATTR_MESSAGE_ID, src.getMessageId());
      obj.addProperty(ATTR_NAME, src.getName());
      obj.addProperty(ATTR_NAMESPACE, src.getNamespace());
      obj.addProperty(ATTR_PAYLOAD_VERSION, src.getPayloadVersion());
      if(src.getCorrelationToken() != null) {
         obj.addProperty(ATTR_CORRELATION_TOKEN, src.getCorrelationToken());
      }
      return obj;
   }
}


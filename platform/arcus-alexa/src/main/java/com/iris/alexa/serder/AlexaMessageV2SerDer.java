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
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.iris.alexa.message.AlexaMessage;
import com.iris.alexa.message.Header;
import com.iris.alexa.message.v2.Payload;

class AlexaMessageV2SerDer implements JsonDeserializer<AlexaMessage>, JsonSerializer<AlexaMessage> {

   private static final Logger logger = LoggerFactory.getLogger(AlexaMessageV2SerDer.class);

   @SuppressWarnings("rawtypes")
   private final ConcurrentHashMap<String,Class> classCache = new ConcurrentHashMap<>();

   @SuppressWarnings({"rawtypes", "unchecked"})
   @Override
   public AlexaMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      try {
         JsonObject obj = json.getAsJsonObject();
         Header h = context.deserialize(obj.get(SerDer.ATTR_HEADER), Header.class);
         Class clazz = getPayloadClass(h.getName());
         logger.debug("deserializing payload type {}", clazz);

         Payload p = context.deserialize(obj.get(SerDer.ATTR_PAYLOAD), clazz);
         if(p == null) {
            logger.debug("got back null, returning new instance of {}", clazz);
            p = (Payload) clazz.getConstructor().newInstance();
         }
         return new AlexaMessage(h, p);
      } catch(Exception e) {
         throw new JsonParseException(e);
      }
   }

   @Override
   public JsonElement serialize(AlexaMessage src, Type typeOfSrc, JsonSerializationContext context) {
      JsonObject obj = new JsonObject();
      obj.add(SerDer.ATTR_HEADER, context.serialize(src.getHeader(), Header.class));
      obj.add(SerDer.ATTR_PAYLOAD, context.serialize(src.getPayload()));
      return obj;
   }

   @SuppressWarnings("rawtypes")
   private Class getPayloadClass(String type) {
      if(type.endsWith("Request")) {
         return getPayloadClass(type, "request");
      }
      if(type.endsWith("Error")) {
         return getPayloadClass(type, "error");
      }
      return getPayloadClass(type, "response");
   }

   @SuppressWarnings("rawtypes")
   private Class getPayloadClass(String type, String pkg) {
      String name = "com.iris.alexa.message.v2." + pkg + '.' + type;
      return classCache.computeIfAbsent(name, (f) -> {
         try {
            return Class.forName(name);
         } catch(Exception e) {
            throw new RuntimeException(e);
         }
      });
   }
}


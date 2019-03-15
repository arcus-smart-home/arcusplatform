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
package com.iris.tools.kat.message;

import java.io.StringReader;
import java.time.Instant;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

/**
 * 
 */
public class Entry {
   private final Instant timestamp;
   private final JsonObject payload;
   
   /**
    * 
    */
   public Entry(Instant timestamp, JsonObject payload) {
      this.timestamp = timestamp;
      this.payload = payload;
   }

   /**
    * @return the timestamp
    */
   public Instant getTimestamp() {
      return timestamp;
   }

   /**
    * @return the payload
    */
   public JsonObject getPayload() {
      return payload;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "Message [timestamp=" + timestamp + ", payload=" + payload + "]";
   }
   
   private static final Gson GSON =
         new GsonBuilder()
            .create();
            

   protected static Instant instant(JsonElement value) {
      return Instant.ofEpochMilli(value.getAsLong());
   }
   
   protected static JsonObject deserialize(String buffer) {
      JsonReader reader = new JsonReader(new StringReader(buffer));
      reader.setLenient(true);
      return GSON.fromJson(reader, JsonObject.class);
   }
   
   public static Function<String, ? extends Entry> getDeserializerForTopic(String topic) {
      if("platform".equals(topic) || "analytics".equals(topic)) {
         return PlatformEntry.deserializer();
      }
      else if(topic.startsWith("prot")) {
         return ProtocolEntry.deserializer();
      }
      else if("irisLog".equals(topic)) {
         return LogEntry.deserializer();
      }
      else {
         throw new IllegalArgumentException("Unrecognized topic: " + topic);
      }
   }
}


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
package com.iris.protocol.ipcd.message;

import java.io.Reader;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iris.protocol.ipcd.message.model.MessageType;

public class IpcdMessageParser {
   private final List<ParserAdapter> adapters;
   private final Gson gson;

   public IpcdMessageParser(Gson gson, ParserAdapter... messageTypeAdapters) {
      adapters = Arrays.asList(messageTypeAdapters);
      this.gson = gson;
   }
   
   public IpcdMessage parseIpcdMessage(Reader json) {
      JsonParser p = new JsonParser();
      JsonObject envelope  = p.parse(json).getAsJsonObject();
      
      for (ParserAdapter adapter : adapters) {
         if (hasRequiredElements(envelope, adapter)) {
            return adapter.parse(gson, envelope);
         }
      }
      return null;
   }
   
   public MessageType discoverMessageType(Reader json) {
      JsonParser p = new JsonParser();
      JsonObject envelope  = p.parse(json).getAsJsonObject();
      
      for (ParserAdapter adapter : adapters) {
         if (hasRequiredElements(envelope, adapter)) {
            return adapter.getMessageType();
         }
      }
      return null;
   }
   
   private boolean hasRequiredElements(JsonObject envelope, ParserAdapter adapter) {      
      for (String name : adapter.requiredElements()) {
         JsonElement element = envelope.get(name); 
         if (element == null) {
            return false;
         }
      }
      return true;
   }
}


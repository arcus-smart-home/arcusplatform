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
package com.iris.tools.kat.output;

import java.io.IOException;
import java.io.Writer;
import java.util.Base64;
import java.util.Date;

import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.iris.tools.kat.message.Entry;

/**
 * 
 */
public class FormattedMessageWriter implements MessageWriter {
   public static final String TIMESTAMP = "time";
   public static final String DECODED = "decoded";
   public static final String DFLT_PLATFORM_FORMAT = "${" + TIMESTAMP + "}\tfrom:${source} to:${destination} type:${type} attributes:${payload.attributes}";
   public static final String DFLT_PROTOCOL_FORMAT = "${" + TIMESTAMP + "}\tfrom:${source} to:${destination} protocol:${typeName} buffer:${buffer} decoded:${" + DECODED + "}";
   
   private String platformFormat;
   private String protocolFormat;
   private Writer writer;

   public FormattedMessageWriter(Writer writer) {
      this(writer, DFLT_PLATFORM_FORMAT, DFLT_PROTOCOL_FORMAT);
   }

   public FormattedMessageWriter(Writer writer, String platformFormat, String protocolFormat) {
      this.writer = writer;
      this.platformFormat = platformFormat;
      this.protocolFormat = protocolFormat;
   }

   @Override
   public void write(Entry entry) throws IOException {
      StrSubstitutor substitutor = new StrSubstitutor(new EntryLookup(entry), "${", "}", '\\');
      boolean isProtocol = isProtocolMessage(entry);
      String message = substitutor.replace(isProtocol ? protocolFormat : platformFormat) + "\n";
      writer.write(message);
      writer.flush();
   }

   @Override
   public void close() {
      try {
         writer.close();
      }
      catch(IOException e) {
         // ignore
      }
   }
   
   private boolean isProtocolMessage(Entry entry) {
      if(entry.getPayload() == null) {
         return false;
      }
      
      JsonElement to = entry.getPayload().get("source");
      if(to != null && to.isJsonPrimitive() && to.getAsString().startsWith("PROT:")) {
         return true;
      }
      JsonElement from = entry.getPayload().get("destination");
      if(from != null && from.isJsonPrimitive() && from.getAsString().startsWith("PROT:")) {
         return true;
      }
      return false;
   }

   private class EntryLookup extends StrLookup<Object> {
      private Entry entry;
      
      EntryLookup(Entry entry) {
         this.entry = entry;
      }
      
      @Override
      public String lookup(String key) {
         if(TIMESTAMP.equals(key)) {
            return new Date(entry.getTimestamp().toEpochMilli()).toString();
         }
         if(DECODED.equals(key)) {
            String type = format(getValue(entry.getPayload(), new String[] { "typeName" }, 0));
            if("ZIGB".equals(type) || "ZWAV".equals(type)) {
               return "<binary>";
            }
            
            byte [] buffer = Base64.getDecoder().decode(entry.getPayload().get("buffer").getAsString());
            return new String(buffer);
         }
         String [] parts = key.split("\\.");
         JsonElement value = getValue(entry.getPayload(), parts, 0);
         return format(value);
      }

      private String format(JsonElement value) {
         // TODO better formatters
         return value.toString();
      }

      private JsonElement getValue(JsonObject payload, String[] parts, int index) {
         String name = parts[index];
         JsonElement e = payload.get(name);
         if(e == null) {
            e = JsonNull.INSTANCE;
         }
         index++;
         if(index == parts.length) {
            return e;
         }
         
         if(e.isJsonObject()) {
            return getValue(e.getAsJsonObject(), parts, index);
         }
         else if(e.isJsonArray()) {
            return getValue(e.getAsJsonArray(), parts, index);
         }
         else {
            return e;
         }
      }

      private JsonElement getValue(JsonArray payload, String[] parts, int index) {
         Integer offset = Integer.parseInt(parts[index]);
         JsonElement e = payload.get(offset);
         if(e == null) {
            e = JsonNull.INSTANCE;
         }
         index++;
         if(index == parts.length) {
            return e;
         }
         
         if(e.isJsonObject()) {
            return getValue(e.getAsJsonObject(), parts, index);
         }
         else if(e.isJsonArray()) {
            return getValue(e.getAsJsonArray(), parts, index);
         }
         else {
            return e;
         }
      }
      
   }

}


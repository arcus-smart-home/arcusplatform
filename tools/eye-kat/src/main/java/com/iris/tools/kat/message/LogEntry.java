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
package com.iris.tools.kat.message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class LogEntry extends Entry {
   private static Logger logger = LoggerFactory.getLogger(LogEntry.class);
   static final Pattern pattern = Pattern.compile(".*(\\d{4}-\\d{2}-\\d{2}T\\d{2}\\:\\d{2}\\:\\d{2}\\.\\d{6}[+-]\\d{2}\\:\\d{2}).*");

   public static Function<String, LogEntry> deserializer() {
      return (json) -> {
         try {
            JsonObject wholeThing = deserialize(json);
            String message = null;
            try {
               if(wholeThing.has("jsonmsg")) {
                  message = wholeThing.get("jsonmsg").getAsString();
                  JsonObject log = deserialize(message);
                  return new LogEntry(log);
               }
               else {
                  message = wholeThing.get("msg").getAsString();
               }
            }
            catch(Exception e) {
               // oops, try again
            }
            if(message == null) {
               message = json;
            }

            // else or exception
            SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-DD'T'HH:mm:ss.SSSSSSX");
            Date date = sdf.parse(wholeThing.get("ts").getAsString());
            JsonObject jo = new JsonObject();
            jo.addProperty("ts", date.getTime());
            jo.addProperty("msg", message);
            return new LogEntry(jo);
         }
         catch(JsonParseException e) {
            logger.warn("Invalid json: [{}]\n\t{}", json, e.getMessage());
         }
         catch(Exception e) {
            logger.warn("Unable to parse message: [{}]", json, e);
         }
         try {
            Matcher m = pattern.matcher(json);
            if(m.matches()) {
               SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-DD'T'HH:mm:ss.SSSSSSX");
               Date date = sdf.parse(m.group(1));
               JsonObject jo = new JsonObject();
               jo.addProperty("ts", date.getTime());
               jo.addProperty("msg", json);
               return new LogEntry(jo);
            }
         }
         catch(Exception e) {
            logger.warn("Unable to extract date from message: [{}]", json, e);
         }
         return null;
      };
   }

   public LogEntry(JsonObject jo) {
      super(instant(jo.get("ts")), jo);
   }

}


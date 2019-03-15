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

import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iris.protocol.ipcd.message.model.MessageType;

public abstract class ParserAdapter {
   public abstract MessageType getMessageType();
   public abstract Set<String> requiredElements();
   public abstract IpcdMessage parse(Gson gson, JsonObject jsonObject);
   
   protected String getCommand(String commandElement, JsonObject jsonObject) {
      if (StringUtils.isEmpty(commandElement)) {
         return null;
      }
      if (".".equals(commandElement)) {
         // Get it from the current element.
         return jsonObject.get("command").getAsString();
      }
      // Need to get the element and then extract the command.
      JsonElement element = jsonObject.get(commandElement);
      if (element != null) {
         return element.getAsJsonObject().get("command").getAsString();
      }
      return null;
   }
}


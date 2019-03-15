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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.iris.alexa.message.AlexaMessage;
import com.iris.alexa.message.Endpoint;
import com.iris.alexa.message.Header;
import com.iris.alexa.message.Scope;

public enum SerDer {
   ;

   public static final String ATTR_HEADER = "header";
   public static final String ATTR_PAYLOAD = "payload";
   public static final String ATTR_DIRECTIVE = "directive";

   private static final Gson gson = new GsonBuilder()
      .registerTypeAdapter(Scope.class, new ScopeSerDer())
      .registerTypeAdapter(Endpoint.class, new EndpointSerDer())
      .registerTypeAdapter(Header.class, new HeaderSerDer())
      .registerTypeAdapter(AlexaMessage.class, new AlexaMessageFacadeSerDer())
      .create();

   public static String serialize(AlexaMessage m) {
      return gson.toJson(m, AlexaMessage.class);
   }

   @SuppressWarnings("unchecked")
   public static AlexaMessage deserialize(String json) {
      return gson.fromJson(json, AlexaMessage.class);
   }

}


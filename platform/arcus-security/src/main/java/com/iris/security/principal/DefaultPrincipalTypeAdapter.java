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
package com.iris.security.principal;

import java.lang.reflect.Type;
import java.util.UUID;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class DefaultPrincipalTypeAdapter implements JsonSerializer<DefaultPrincipal>, JsonDeserializer<DefaultPrincipal> {
	
	private static final String ATTR_USER_NAME = "user";
	private static final String ATTR_USER_ID   = "id";

	@Override
   public JsonElement serialize(DefaultPrincipal src, Type typeOfSrc, JsonSerializationContext context) {
	   JsonObject response = new JsonObject();
	   response.add(ATTR_USER_NAME, context.serialize(src.getUsername()));
	   response.add(ATTR_USER_ID, context.serialize(src.getUserId()));
	   return response;
   }
	
	@Override
   public DefaultPrincipal deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		DefaultPrincipal principal = null;
	   if (json.isJsonObject()) {
	   	JsonObject obj = json.getAsJsonObject();
	   	String userName = context.deserialize(obj.get(ATTR_USER_NAME), String.class);
	   	UUID userId = context.deserialize(obj.get(ATTR_USER_ID), UUID.class);
	   	principal = new DefaultPrincipal(userName, userId);
	   }
	   return principal;
   }
}


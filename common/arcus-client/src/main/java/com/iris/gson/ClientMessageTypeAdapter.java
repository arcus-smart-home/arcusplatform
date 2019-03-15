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
package com.iris.gson;

import java.lang.reflect.Type;
import java.util.Map;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;

/**
 *
 */
public class ClientMessageTypeAdapter implements JsonSerializer<ClientMessage>, JsonDeserializer<ClientMessage> {
	private static final String ATTR_TYPE = "type";
	private static final String ATTR_HEADERS = "headers";
	private static final String ATTR_PAYLOAD = "payload";

	@Override
   public JsonElement serialize(ClientMessage src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject response = new JsonObject();
		response.add(ATTR_TYPE, context.serialize(src.getType()));
		response.add(ATTR_HEADERS, context.serialize(src.getHeaders()));
		response.add(ATTR_PAYLOAD, context.serialize(src.getPayload()));
		return response;
   }

   @Override
   public ClientMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		JsonObject src = json.getAsJsonObject();
		if(src.isJsonNull()) {
			return null;
		}
		try {
		   Map<String,Object> headers = context.deserialize(src.get(ATTR_HEADERS), new TypeToken<Map<String, Object>>(){}.getType());
   		MessageBody payload = context.deserialize(src.get(ATTR_PAYLOAD), MessageBody.class);

   		return ClientMessage.builder()
   		      .withHeaders(headers)
   		      .withPayload(payload)
   		      .create();
		}
		catch(Exception e) {
			throw new JsonParseException("Unable to create ClientMessage", e);
		}
   }

}


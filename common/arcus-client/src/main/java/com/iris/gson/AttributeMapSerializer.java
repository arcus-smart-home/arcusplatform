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

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.device.attributes.AttributeValue;

/**
 * 
 */
public class AttributeMapSerializer implements JsonSerializer<AttributeMap>, JsonDeserializer<AttributeMap> {

	@Override
   public AttributeMap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		AttributeMap map = AttributeMap.newMap(); 
	   JsonArray ja = json.getAsJsonArray();
	   for(JsonElement e: ja) {
	   	deserializeEntry(e, context, map);
	   }
	   return map;
   }

	@SuppressWarnings("unchecked")
   private void deserializeEntry(JsonElement e, JsonDeserializationContext context, AttributeMap map) {
	   JsonArray value = e.getAsJsonArray();
	   String key = context.deserialize(value.get(0), String.class);
	   Type type = context.deserialize(value.get(1), Type.class);
	   Object v = context.deserialize(value.get(2), type);
	   
	   map.set((AttributeKey) AttributeKey.createType(key, type), v);
   }

	@Override
   public JsonElement serialize(AttributeMap src, Type typeOfSrc, JsonSerializationContext context) {
	   JsonArray values = new JsonArray();
	   for(AttributeValue<?> value: src.entries()) {
	      values.add(serializeValue(value, context));
	   }
	   return values;
   }

	// TODO could reduce this to an array...
	private JsonElement serializeValue(AttributeValue<?> value, JsonSerializationContext context) {
		String name = value.getKey().getName();
		Type type = value.getKey().getType();
		Object v = value.getValue();
		JsonArray ja = new JsonArray();
		ja.add(context.serialize(name));
		ja.add(context.serialize(type, Type.class));
		ja.add(context.serialize(v, type));
	   return ja;
   }

}


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
package com.iris.io.json.gson;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.iris.util.Result;
import com.iris.util.Results;

public class ResultTypeAdapter implements JsonSerializer<Result>, JsonDeserializer<Result> {
   private static final String ATTR_VALUE_TYPE = "valueType";
	private static final String ATTR_VALUE = "value";
	private static final String ATTR_ERRSMG = "error";

	@Override
   public JsonElement serialize(Result src, Type typeOfSrc, JsonSerializationContext context) {
	   return src.isError() ? serializeError(context, src.getError()) : serializeValue(context, src.getValue());
   }

	private JsonElement serializeError(JsonSerializationContext context, Throwable error) {
	   JsonObject object = new JsonObject();
	   object.add(ATTR_ERRSMG, context.serialize(error.getMessage()));
	   return object;
	}

	private JsonElement serializeValue(JsonSerializationContext context, Object value) {
	   if(value == null) {
	      return null;
	   }
	   JsonObject object = new JsonObject();
	   object.add(ATTR_VALUE_TYPE, context.serialize(value.getClass().getName()));
      object.add(ATTR_VALUE, context.serialize(value, value.getClass()));
      return object;
	}

	@Override
   public Result deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		JsonObject src = json.getAsJsonObject();
		if(src.isJsonNull()) {
			return null;
		}

		try {
   		if(src.get(ATTR_ERRSMG) != null) {
   		   return Results.fromError(new Exception(src.get(ATTR_ERRSMG).getAsString()));
   		}
   		Class valueType = Class.forName(src.get(ATTR_VALUE_TYPE).getAsString());
   		Object value = context.deserialize(src.get(ATTR_VALUE), valueType);
   		return Results.fromValue(value);
		}
		catch(Exception e) {
			throw new JsonParseException("Unable to create PlatformMessage", e);
		}
   }

}


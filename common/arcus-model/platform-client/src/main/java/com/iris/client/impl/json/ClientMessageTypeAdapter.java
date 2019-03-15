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
package com.iris.client.impl.json;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.iris.capability.definition.AttributeType;
import com.iris.capability.definition.Definition;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.capability.definition.EventDefinition;
import com.iris.capability.definition.ParameterDefinition;
import com.iris.client.ClientMessage;

/**
 *
 */
public class ClientMessageTypeAdapter implements JsonSerializer<ClientMessage>, JsonDeserializer<ClientMessage> {
	private static final String ATTR_TYPE = "type";
	private static final String ATTR_HEADERS = "headers";
	private static final String ATTR_PAYLOAD = "payload";
   private static final String ATTR_ATTRIBUTES = "attributes";

	private static final TypeToken<Map<String, Object>> TYPE_MAP = new TypeToken<Map<String, Object>>() {};
	
	private final DefinitionRegistry definitions;
	
	public ClientMessageTypeAdapter(DefinitionRegistry definitions) {
	   this.definitions = definitions;
   }
	
	@Override
   public JsonElement serialize(ClientMessage src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject response = new JsonObject();
		JsonElement type = context.serialize(src.getEvent().getType());
		response.add(ATTR_TYPE, type);
		response.add(ATTR_HEADERS, context.serialize(src.getHeaders()));
		
		JsonObject payload = new JsonObject();
		payload.add("messageType", type);
		payload.add(ATTR_ATTRIBUTES, context.serialize(src.getEvent().getAttributes()));
		response.add(ATTR_PAYLOAD, payload);
		
		return response;
   }

   @Override
   public ClientMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		JsonObject src = json.getAsJsonObject();
		if(src.isJsonNull()) {
			return null;
		}
		try {
		   String type = context.deserialize(src.get(ATTR_TYPE), String.class);
		   Map<String,Object> headers = context.deserialize(src.get(ATTR_HEADERS), TYPE_MAP.getType());
		   JsonObject payload = src.getAsJsonObject(ATTR_PAYLOAD);
		   
		   EventDefinition event = definitions.getEvent(type);
		   Map<String, Object> attributes = deserializeEvent(payload.get(ATTR_ATTRIBUTES).getAsJsonObject(), event, context);

   		return 
   		      ClientMessage
      		      .builder()
      		      .withHeaders(headers)
                  .withType(type)
      		      .withAttributes(attributes)
      		      .create();
		}
		catch(Exception e) {
			throw new JsonParseException("Unable to create ClientMessage", e);
		}
   }

   private Map<String, Object> deserializeEvent(JsonObject object, EventDefinition event, JsonDeserializationContext ctx) {
      if(event == null) {
         return (Map<String, Object>)ctx.deserialize(object, TYPE_MAP.getType());
      }
      
      // TODO cache this
      Map<String, ParameterDefinition> parameters = toMap(event.getParameters());
      
      Map<String, Object> attributes = new HashMap<String, Object>();
      for(Map.Entry<String, JsonElement> entry: object.entrySet()) {
         String name = entry.getKey();
         ParameterDefinition parameter = parameters.get(name);
         Object value = deserializeAttribute(entry.getValue(), parameter != null ? parameter.getType() : null, ctx);
         if(value != null) {
            attributes.put(name, value);
         }
      }
      return attributes;
   }

   private Object deserializeAttribute(JsonElement value, AttributeType attributeType, JsonDeserializationContext ctx) {
      if(attributeType == null) {
         return ctx.deserialize(value, Object.class);
      }
      return ctx.deserialize(value, attributeType.getJavaType());
   }

   private <D extends Definition> Map<String, D> toMap(Collection<D> definitions) {
      if(definitions == null || definitions.isEmpty()) {
         return Collections.emptyMap();
      }
      Map<String, D> map = new HashMap<String, D>(definitions.size());
      for(D definition: definitions) {
         map.put(definition.getName(), definition);
      }
      return map;
   }

}


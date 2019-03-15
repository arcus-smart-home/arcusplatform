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

import java.io.IOException;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.iris.messages.MessageBody;

// TODO we could generate this serializer to be capability aware and optimize the 
//      deserialization to return constants for enum values and names
public class MessageBodyTypeAdapter extends TypeAdapter<MessageBody> {
   private static final String ATTR_MESSAGE_TYPE = "messageType";
   private static final String ATTR_ATTRIBUTES = "attributes";

   private final TypeAdapter<Object> valueAdapter;

   public MessageBodyTypeAdapter(Gson gson) {
      this.valueAdapter = gson.getAdapter(Object.class);
   }

   @Override
   public void write(JsonWriter out, MessageBody value) throws IOException {
   	out.beginObject();
   	out.name(ATTR_MESSAGE_TYPE);
   	out.value(value.getMessageType());
   	Map<String, Object> attributes = value.getAttributes();
   	if(attributes == null || attributes.isEmpty()) {
   		if(out.getSerializeNulls()) {
   			out.name(ATTR_ATTRIBUTES);
   			out.beginObject();
   			out.endObject();
   		}
   	}
   	else {
   		out.name(ATTR_ATTRIBUTES);
   		out.beginObject();
   		for(Map.Entry<String, Object> e: attributes.entrySet()) {
   			out.name(e.getKey());
   			valueAdapter.write(out, e.getValue());
   		}
   		out.endObject();
   	}
   	out.endObject();
   }

	@Override
   public MessageBody read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
         in.nextNull();
         return null;
      }
      
      String messageType = null;
      ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();

      in.beginObject();
      while (in.hasNext()) {
         switch (in.nextName()) {
         case ATTR_MESSAGE_TYPE:
            if (in.peek() != JsonToken.NULL) {
               messageType = in.nextString();
            } else {
               in.nextNull();
            }
            break;
            
         case ATTR_ATTRIBUTES:
            if (in.peek() != JsonToken.NULL) {
               in.beginObject();
               while(in.peek() == JsonToken.NAME) {
               	String name = in.nextName();
               	if(in.peek() != JsonToken.NULL) {
               		builder.put(name, valueAdapter.read(in));
               	}
               	else {
               		in.nextNull();
               	}
               }
               in.endObject();
            } else {
               in.nextNull();
            }
            break;
            
         default:
            // ignore extra fields
        	   in.skipValue();
            break;
         }
      }
      in.endObject();

      return MessageBody.buildMessage(messageType, builder.build());
   }

}


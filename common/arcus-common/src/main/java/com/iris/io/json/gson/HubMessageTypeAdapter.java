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

import org.apache.commons.codec.binary.Base64;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.iris.messages.HubMessage;

public class HubMessageTypeAdapter extends TypeAdapter<HubMessage> {
	private static final String ATTR_PAYLOAD_TYPE = "type";
	private static final String ATTR_PAYLOAD = "buffer";
   private static final byte[] EMPTY_BUFFER = new byte[0];

   @Override
   public void write(JsonWriter out, HubMessage value) throws IOException {
      HubMessage.Type type = value.getType();
      byte[] bf = value.getPayload();

      out.beginObject();
      if (type != null) {
         out.name(ATTR_PAYLOAD_TYPE).value(type.name());
      } else {
         out.name(ATTR_PAYLOAD_TYPE).nullValue();
      }

      if (bf != null && bf.length > 0) {
         out.name(ATTR_PAYLOAD).value(Base64.encodeBase64String(bf));
      } else {
         out.name(ATTR_PAYLOAD).nullValue();
      }

      out.endObject();
   }

   @Override
   public HubMessage read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
         in.nextNull();
         return null;
      }

		HubMessage.Type type = null;
		byte[] buffer = EMPTY_BUFFER;

    	in.beginObject();
    	while (in.hasNext()) {
      	switch (in.nextName()) {
      	case ATTR_PAYLOAD_TYPE:
      	   type = HubMessage.Type.valueOf(in.nextString());
        		break;
      	case ATTR_PAYLOAD:
      	   buffer = Base64.decodeBase64(in.nextString());
        		break;
        	default:
        	   // ignore extra fields
        	   break;
      	}
    	}
    	in.endObject();

      return HubMessage.create(type, buffer);
   }
}


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
package com.iris.platform.rule.catalog.serializer.json;

import java.io.IOException;
import java.util.Arrays;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.iris.capability.definition.AttributeType;
import com.iris.capability.definition.AttributeTypes;

public class AttributeTypeAdapter extends TypeAdapter<AttributeType> {

   @Override
   public void write(JsonWriter out, AttributeType value) throws IOException {
      if(value == null) {
         out.nullValue();
      }
      else {
         out.value(value.getRepresentation());
      }
   }

   @Override
   public AttributeType read(JsonReader in) throws IOException {
      if(in.peek() == JsonToken.NULL) {
         in.nextNull();
         return null;
      }
      else {
         String type = in.nextString();
         if(type.startsWith("enum<")) {
            String [] parts = type.substring(5, type.length() - 1).split(",");
            return AttributeTypes.enumOf(Arrays.asList(parts));
         }
         else {
            return AttributeTypes.parse(type);
         }
      }
   }

}


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
package com.iris.gson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Base64;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class ByteArrayToBase64TypeAdapter extends TypeAdapter<byte[]> {

   @Override
   public void write(JsonWriter out, byte[] value) throws IOException {
      byte[] data = Base64.encodeBase64(value);
      if(data == null) {
      	out.value("");
      }
      else {
	      String str = new String(data, StandardCharsets.UTF_8);
	      out.value(str);
      }
   }

   @Override
   public byte[] read(JsonReader in) throws IOException {
      String str = in.nextString();
      if (str == null) {
         return null;
      }

      byte[] data = str.getBytes(StandardCharsets.UTF_8);
      return Base64.decodeBase64(data);
   }

}


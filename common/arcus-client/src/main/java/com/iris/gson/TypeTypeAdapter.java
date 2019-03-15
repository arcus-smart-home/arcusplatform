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

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.iris.Utils;

/**
 * 
 */
public class TypeTypeAdapter extends TypeAdapter<Type> {

	// TODO handle deep types
	/* (non-Javadoc)
	 * @see com.google.gson.TypeAdapter#write(com.google.gson.stream.JsonWriter, java.lang.Object)
	 */
	@Override
	public void write(JsonWriter out, Type value) throws IOException {
		if(value == null) {
			out.nullValue();
		}
		else {
		   String type = Utils.serializeType(value);
		   out.value(type);
		}
	}

	/* (non-Javadoc)
	 * @see com.google.gson.TypeAdapter#read(com.google.gson.stream.JsonReader)
	 */
	@Override
	public Type read(JsonReader in) throws IOException {
		// TODO handle complex types
		String name = in.nextString();
		if(name == null) {
			return null;
		}
		try {
	      return Utils.deserializeType(name);
      } 
		catch (Exception e) {
	      throw new JsonParseException("Unable to create Class " + name, e);
      }
	}
	
}


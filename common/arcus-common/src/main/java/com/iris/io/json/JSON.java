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
package com.iris.io.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.iris.io.Deserializer;
import com.iris.io.Serializer;
import com.iris.util.TypeMarker;

/**
 * Depends on service locator being initialized.
 */
public class JSON {
   private static final Logger logger = LoggerFactory.getLogger(JSON.class);
   
   private JSON() {
   }

   public static String toJson(Object obj) {
      return getSerializer().toJson(obj);
   }

   public static void toJson(Object obj, Writer output) throws IOException {
      getSerializer().toJson(obj, output);
   }

   public static <T> T fromJson(String json, TypeMarker<T> type) {
      return getDeserializer().fromJson(json, type);
   }

   public static <T> T fromJson(String json, Class<T> clazz) {
      return getDeserializer().fromJson(json, clazz);
   }

   public static <T> T fromJson(Reader input, TypeMarker<T> type) throws IOException {
      return getDeserializer().fromJson(input, type);
   }

   public static <T> T fromJson(Reader input, Class<T> clazz) throws IOException {
      return getDeserializer().fromJson(input, clazz);
   }

   public static <T> Serializer<T> createSerializer(Class<T> type) {
   	return new Serializer<T>() {
			@Override
         public void serialize(Object value, OutputStream out) throws IOException, IllegalArgumentException {
				OutputStreamWriter osw = new OutputStreamWriter(out, Charsets.UTF_8);
		      toJson(value, osw);
		      osw.flush();
         }

			@Override
         public byte[] serialize(T value) throws IllegalArgumentException {
	         return toJson(value).getBytes(Charsets.UTF_8);
         }
   	};
   }

   public static <T> Deserializer<T> createDeserializer(final Class<T> type) {
   	return new Deserializer<T>() {
			@Override
         public T deserialize(InputStream input) throws IOException, IllegalArgumentException {
	         return fromJson(new InputStreamReader(input, Charsets.UTF_8), type);
         }

			@Override
         public T deserialize(byte[] input) throws IllegalArgumentException {
	         return fromJson(new String(input, Charsets.UTF_8), type);
         }
   	};
   }
   
   private static JsonSerializer getSerializer() {
      JsonSerializer s = SerializerRef.get();
      while(s == null) {
         s = JsonFactory.createDefaultSerializer();
         if(SerializerRef.compareAndSet(null, s)) {
            logger.warn("No JsonSerializer registered, falling back to default serializer");
         }
         else {
            s = SerializerRef.get(); 
         }
      }
      return s;
   }
   
   private static JsonDeserializer getDeserializer() {
      JsonDeserializer d = DeserializerRef.get();
      while(d == null) {
         d = JsonFactory.createDefaultDeserializer();
         if(DeserializerRef.compareAndSet(null, d)) {
            logger.warn("No JsonDeserializer registered, falling back to default deserializer");
         }
         else {
            d = DeserializerRef.get(); 
         }
      }
      return d;
   }
   
   static void setSerializer(JsonSerializer serializer) {
      if(SerializerRef.getAndSet(serializer) != null) {
         logger.warn("Reconfiguring JSON serializer at run time -- this should not happen in a live system");
      }
   }
   
   static void setDeserializer(JsonDeserializer deserializer) {
      if(DeserializerRef.getAndSet(deserializer) != null) {
         logger.warn("Reconfiguring JSON serializer at run time -- this should not happen in a live system");
      }
   }
   
   private static final AtomicReference<JsonSerializer> SerializerRef = new AtomicReference<>();
   private static final AtomicReference<JsonDeserializer> DeserializerRef = new AtomicReference<>();
   
}


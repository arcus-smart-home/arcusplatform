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
package com.iris.io.json;

import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.iris.gson.AddressTypeAdapterFactory;
import com.iris.gson.AttributeMapSerializer;
import com.iris.gson.ByteArrayToBase64TypeAdapter;
import com.iris.gson.ClientMessageTypeAdapter;
import com.iris.gson.GsonFactory;
import com.iris.gson.ProtocolDeviceIdTypeAdapter;
import com.iris.gson.TypeTypeAdapterFactory;
import com.iris.io.json.gson.GsonDeserializerImpl;
import com.iris.io.json.gson.GsonReferenceTypeAdapterFactory;
import com.iris.io.json.gson.GsonSerializerImpl;
import com.iris.io.json.gson.MessageBodyTypeAdapterFactory;
import com.iris.io.json.gson.MessageTypeAdapterFactory;
import com.iris.io.json.gson.ResultTypeAdapter;

/**
 * 
 */
// TODO find a better way to keep this in sync with GsonModule / MessagesModule
public class JsonFactory {
   static final GsonFactory factory;
   
   static {
      Set<TypeAdapterFactory> adapterFactories = new HashSet<TypeAdapterFactory>();
      adapterFactories.add(new TypeTypeAdapterFactory());
      adapterFactories.add(new AddressTypeAdapterFactory());
      adapterFactories.add(new GsonReferenceTypeAdapterFactory());
      adapterFactories.add(new MessageTypeAdapterFactory());
      adapterFactories.add(new MessageBodyTypeAdapterFactory());
      
      Set<TypeAdapter<?>> adapters = new HashSet<TypeAdapter<?>>();
      adapters.add(new ByteArrayToBase64TypeAdapter());
      adapters.add(new ProtocolDeviceIdTypeAdapter());
      
      Set<JsonDeserializer<?>> deserializers = new HashSet<JsonDeserializer<?>>();
      Set<JsonSerializer<?>> serializers = new HashSet<JsonSerializer<?>>();
      {
         AttributeMapSerializer adapter = new AttributeMapSerializer();
         deserializers.add(adapter);
         serializers.add(adapter);
      }
      {
         ClientMessageTypeAdapter adapter = new ClientMessageTypeAdapter();
         deserializers.add(adapter);
         serializers.add(adapter);
      }
      {
         ResultTypeAdapter adapter = new ResultTypeAdapter();
         deserializers.add(adapter);
         serializers.add(adapter);
      }
   
      factory = new GsonFactory(
            adapterFactories,
            adapters,
            serializers,
            deserializers
      );
   }
   
   public static com.iris.io.json.JsonSerializer createDefaultSerializer() {
      return new GsonSerializerImpl(factory);
   }
   
   public static com.iris.io.json.JsonDeserializer createDefaultDeserializer() {
      return new GsonDeserializerImpl(factory);
   }
}


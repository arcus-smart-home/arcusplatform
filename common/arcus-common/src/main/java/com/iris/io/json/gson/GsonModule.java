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

import java.util.Set;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.iris.gson.AttributeMapSerializer;
import com.iris.gson.ByteArrayToBase64TypeAdapter;
import com.iris.gson.GsonFactory;
import com.iris.gson.TypeTypeAdapterFactory;
import com.iris.io.json.JsonModule;
import com.netflix.governator.annotations.Modules;

@Modules(include=JsonModule.class)
public class GsonModule extends AbstractModule {
   @Inject(optional=true) @Named("gson.serialize.nulls")
   private boolean serializeNulls = true;

   @Override
   protected void configure() {
      bind(com.iris.io.json.JsonSerializer.class).to(GsonSerializerImpl.class);
      bind(com.iris.io.json.JsonDeserializer.class).to(GsonDeserializerImpl.class);

      Multibinder
      	.newSetBinder(binder(), new TypeLiteral<com.google.gson.JsonSerializer<?>>() {})
      	.addBinding()
      	.to(AttributeMapSerializer.class);
      Multibinder
      	.newSetBinder(binder(), new TypeLiteral<com.google.gson.JsonDeserializer<?>>() {})
      	.addBinding()
      	.to(AttributeMapSerializer.class);
      Multibinder
         .newSetBinder(binder(), new TypeLiteral<TypeAdapter<?>>() {})
         .addBinding()
         .to(ByteArrayToBase64TypeAdapter.class);
      Multibinder
         .newSetBinder(binder(), new TypeLiteral<TypeAdapterFactory>() {})
         .addBinding()
         .to(TypeTypeAdapterFactory.class);
   }

   @Provides
   public GsonFactory gson(
      Set<TypeAdapterFactory> typeAdapterFactories,
      Set<TypeAdapter<?>> typeAdapters,
      Set<JsonSerializer<?>> serializers,
      Set<JsonDeserializer<?>> deserializers
   ) {
      return new GsonFactory(typeAdapterFactories, typeAdapters, serializers, deserializers, serializeNulls);
   }
}


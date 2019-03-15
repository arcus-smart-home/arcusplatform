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
package com.iris.core.messaging.kafka;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.kafka.common.serialization.Serializer;

import com.iris.io.json.JSON;

public class KafkaSerializers {

   public static Serializer<Void> voidSerializer() {
      return VoidSerializer.INSTANCE;
   }
   
   @SuppressWarnings("unchecked")
   public static <T> Serializer<T> jsonSerializer() {
      return NonNullJsonSerializer.INSTANCE;
   }
   
   @SuppressWarnings("unchecked")
   public static <T> Serializer<T> nullableJsonSerializer() {
      return NullableJsonSerializer.INSTANCE;
   }
   
   private enum VoidSerializer implements Serializer<Void> {
      INSTANCE;
      private final byte[] empty = new byte[] {};
      
      @Override
      public void configure(Map<String, ?> configs, boolean isKey) {
         //no-op
      }

      @Override
      public byte[] serialize(String topic, Void value) {
         return empty;
      }

      @Override
      public void close() {
         // no-op
      }
   }

   @SuppressWarnings("rawtypes")
   private enum NonNullJsonSerializer implements Serializer {
      INSTANCE;

      @Override
      public void configure(Map configs, boolean isKey) {
         //no-op
      }

      @Override
      public byte[] serialize(String topic, Object value) {
         if (value == null) throw new IllegalArgumentException("null");
         return JSON.toJson(value).getBytes(StandardCharsets.UTF_8);
      }

      @Override
      public void close() {
         // no-op
      }
   }

   @SuppressWarnings("rawtypes")
   private enum NullableJsonSerializer implements Serializer {
      INSTANCE;

      @Override
      public void configure(Map configs, boolean isKey) {
         //no-op
      }

      @Override
      public byte[] serialize(String topic, Object value) {
         if (value == null) {
            return new byte [] { 'n', 'u', 'l', 'l' };
         }
         else {
            return JSON.toJson(value).getBytes(StandardCharsets.UTF_8);
         }
      }

      @Override
      public void close() {
         // no-op
      }

   }
}


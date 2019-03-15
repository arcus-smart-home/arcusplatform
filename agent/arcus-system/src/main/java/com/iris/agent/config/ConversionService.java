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
package com.iris.agent.config;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.iris.util.Hex;

public final class ConversionService {
   private static final ConcurrentMap<Class<?>,Converter<?>> converters;
   private static final Gson GSON = new GsonBuilder()
      .disableHtmlEscaping()
      .registerTypeAdapter(Date.class, new DateTypeAdapter())
      .create();

   static {
      ConcurrentMap<Class<?>,Converter<?>> conv = new ConcurrentHashMap<>();
      register(conv, String.class, StringConverter.INSTANCE);
      register(conv, Boolean.class, BooleanConverter.INSTANCE);
      register(conv, Byte.class, ByteConverter.INSTANCE);
      register(conv, Short.class, ShortConverter.INSTANCE);
      register(conv, Integer.class, IntConverter.INSTANCE);
      register(conv, Long.class, LongConverter.INSTANCE);
      register(conv, Double.class, DoubleConverter.INSTANCE);
      register(conv, UUID.class, UuidConverter.INSTANCE);

      register(conv, boolean.class, BooleanConverter.INSTANCE);
      register(conv, byte.class, ByteConverter.INSTANCE);
      register(conv, byte[].class, ByteArrayConverter.INSTANCE);
      register(conv, short.class, ShortConverter.INSTANCE);
      register(conv, int.class, IntConverter.INSTANCE);
      register(conv, long.class, LongConverter.INSTANCE);
      register(conv, double.class, DoubleConverter.INSTANCE);

      register(conv, Set.class, SetConverter.INSTANCE);
      register(conv, LinkedHashSet.class, SetConverter.INSTANCE);
      register(conv, HashSet.class, SetConverter.INSTANCE);

      register(conv, List.class, ListConverter.INSTANCE);
      register(conv, ArrayList.class, ListConverter.INSTANCE);

      converters = conv;
   }

   private ConversionService() {
   }

   /////////////////////////////////////////////////////////////////////////////
   // Conversion extension registering
   /////////////////////////////////////////////////////////////////////////////

   public static <T> void register(Class<T> clazz, Converter<T> converter) {
      register(converters, clazz, converter);
   }

   private static <T> void register(ConcurrentMap<Class<?>,Converter<?>> convs, Class<T> clazz, Converter<? extends T> converter) {
      Converter<?> old = convs.putIfAbsent(clazz, converter);
      if (old != null) {
         throw new ConversionException("converter already exists for: " + clazz);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Type conversion to/from strings
   /////////////////////////////////////////////////////////////////////////////

   @Nullable
   public static <T> T to(Class<T> clazz, @Nullable String value) {
      if (value == null) {
         return null;
      }

      return lookup(clazz).to(value);
   }

   @Nullable
   @SuppressWarnings("unchecked")
   public static <T> String from(@Nullable T value) {
      if (value == null) {
         return null;
      }

      Class<T> clazz = (Class<T>)value.getClass();
      return lookup(clazz).from(value);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Implementation details
   /////////////////////////////////////////////////////////////////////////////

   @SuppressWarnings("unchecked")
   private static <T> Converter<T> lookup(Class<T> clazz) {
      Converter<T> result = (Converter<T>)converters.get(clazz);
      if (result == null) {
         throw new ConversionException("no converter registered for: " + clazz);
      }

      return result;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Standard converter implementations
   /////////////////////////////////////////////////////////////////////////////

   public static enum StringConverter implements Converter<String> {
      INSTANCE;

      @Override
      public String to(String value) {
         return value;
      }

      @Override
      public String from(String value) {
         return value;
      }
   }

   public static enum BooleanConverter implements Converter<Boolean> {
      INSTANCE;

      @Override
      public Boolean to(String value) {
         switch (value.toLowerCase()) {
         case "true":
         case "yes":
         case "on":
         case "y":
            return Boolean.TRUE;

         default:
            return Boolean.FALSE;
         }
      }

      @Override
      public String from(Boolean value) {
         return String.valueOf(value.booleanValue());
      }
   }

   public static enum ByteConverter implements Converter<Byte> {
      INSTANCE;

      @Override
      public Byte to(String value) {
         if (value.startsWith("0x") || value.startsWith("0X")) {
            return Byte.parseByte(value.substring(2), 16);
         } else if (value.startsWith("0b") || value.startsWith("0B")) {
            return Byte.parseByte(value.substring(2), 2);
         } else {
            return Byte.parseByte(value);
         }
      }

      @Override
      public String from(Byte value) {
         return String.valueOf(value.byteValue());
      }
   }

   public static enum ShortConverter implements Converter<Short> {
      INSTANCE;

      @Override
      public Short to(String value) {
         if (value.startsWith("0x") || value.startsWith("0X")) {
            return Short.parseShort(value.substring(2), 16);
         } else if (value.startsWith("0b") || value.startsWith("0B")) {
            return Short.parseShort(value.substring(2), 2);
         } else {
            return Short.parseShort(value);
         }
      }

      @Override
      public String from(Short value) {
         return String.valueOf(value.shortValue());
      }
   }

   public static enum IntConverter implements Converter<Integer> {
      INSTANCE;

      @Override
      public Integer to(String value) {
         if (value.startsWith("0x") || value.startsWith("0X")) {
            return Integer.parseInt(value.substring(2), 16);
         } else if (value.startsWith("0b") || value.startsWith("0B")) {
            return Integer.parseInt(value.substring(2), 2);
         } else {
            return Integer.parseInt(value);
         }
      }

      @Override
      public String from(Integer value) {
         return String.valueOf(value.intValue());
      }
   }

   public static enum LongConverter implements Converter<Long> {
      INSTANCE;

      @Override
      public Long to(String value) {
         if (value.startsWith("0x") || value.startsWith("0X")) {
            return Long.parseLong(value.substring(2), 16);
         } else if (value.startsWith("0b") || value.startsWith("0B")) {
            return Long.parseLong(value.substring(2), 2);
         } else {
            return Long.parseLong(value);
         }
      }

      @Override
      public String from(Long value) {
         return String.valueOf(value.longValue());
      }
   }

   public static enum DoubleConverter implements Converter<Double> {
      INSTANCE;

      @Override
      public Double to(String value) {
         return Double.parseDouble(value);
      }

      @Override
      public String from(Double value) {
         return String.valueOf(value.doubleValue());
      }
   }

   public static enum ByteArrayConverter implements Converter<byte[]> {
      INSTANCE;

      @Override
      public byte[] to(String value) {
         return Hex.fromHexString(value);
      }

      @Override
      public String from(byte[] value) {
         return Hex.toString(value);
      }
   }

   public static enum UuidConverter implements Converter<UUID> {
      INSTANCE;

      @Override
      public UUID to(String value) {
         return UUID.fromString(value);
      }

      @Override
      public String from(UUID value) {
         return value.toString();
      }
   }

   @SuppressWarnings("rawtypes")
   public static enum SetConverter implements Converter<LinkedHashSet> {
      INSTANCE;

      @Override
      public LinkedHashSet to(String value) {
         return GSON.fromJson(value, LinkedHashSet.class);
      }

      @Override
      public String from(LinkedHashSet value) {
         return GSON.toJson(value);
      }
   }

   @SuppressWarnings("rawtypes")
   public static enum ListConverter implements Converter<ArrayList> {
      INSTANCE;

      @Override
      public ArrayList to(String value) {
         return GSON.fromJson(value, ArrayList.class);
      }

      @Override
      public String from(ArrayList value) {
         return GSON.toJson(value);
      }
   }

   @SuppressWarnings({"null","unused"})
   private static final class DateTypeAdapter extends TypeAdapter<Date> {
      @Override
      public Date read(JsonReader reader) throws IOException {
         if (reader.peek() == JsonToken.NULL) {
            return null;
         }

         return new Date(reader.nextLong());
      }

      @Override
      public void write(JsonWriter writer, Date value) throws IOException {
         if (value == null) {
            writer.nullValue();
         } else {
            writer.value(value.getTime());
         }
      }
   }
}


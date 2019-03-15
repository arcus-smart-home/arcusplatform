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
package com.iris.driver.groovy.reflex;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.iris.driver.groovy.GroovyValidator;
import com.iris.protoc.runtime.ProtocUtil;

public final class ReflexUtil {
   public static final Object WILDCARD = new Object();

   private ReflexUtil() {
   }

   public static byte[] extractAsByteArray(Map<String,Object> map, String key) {
      Object value = map.get(key);
      if (value == null) {
         GroovyValidator.error("zigbee message must define 'payload'");
         return new byte[0];
      }

      return convertToByteArray(value);
   }

   public static List<String> extractAsMatchList(Map<String,Object> map, String key) {
      Object value = map.get(key);
      if (value == null) {
         return ImmutableList.of();
      }

      return convertToMatchList(value);
   }

   public static byte[] convertToByteArray(Object value) {
      byte[] result;
      if (value instanceof int[]) {
         int[] arr = (int[])value;
         result = new byte[arr.length];
         for (int i = 0; i < arr.length; ++i) {
            result[i] = (byte)(arr[i] & 0xFF);
         }
      } else if (value instanceof Integer[]) {
         Integer[] arr = (Integer[])value;
         result = new byte[arr.length];
         for (int i = 0; i < arr.length; ++i) {
            result[i] = (byte)(arr[i] & 0xFF);
         }
      } else if (value instanceof List) {
         List<?> arr = (List<?>)value;
         result = new byte[arr.size()];
         
         int i = 0;
         for (Object obj : arr) {
            result[i++] = ((Number)obj).byteValue();
         }
      } else {
         result = (byte[])value;
      }

      return result;
   }

   public static List<String> convertToMatchList(Object value) {
      ImmutableList.Builder<String> result = ImmutableList.builder();

      if (value instanceof int[]) {
         int[] arr = (int[])value;
         for (int i = 0; i < arr.length; ++i) {
            result.add(ProtocUtil.toHexString((byte)(arr[i] & 0xFF)));
         }
      } else if (value instanceof Integer[]) {
         Integer[] arr = (Integer[])value;
         for (int i = 0; i < arr.length; ++i) {
            result.add(ProtocUtil.toHexString((byte)(arr[i] & 0xFF)));
         }
      } else if (value instanceof byte[]) {
         byte[] arr = (byte[])value;
         for (int i = 0; i < arr.length; ++i) {
            result.add(ProtocUtil.toHexString(arr[i]));
         }
      } else if (value instanceof Byte[]) {
         Byte[] arr = (Byte[])value;
         for (int i = 0; i < arr.length; ++i) {
            result.add(ProtocUtil.toHexString(arr[i]));
         }
      } else if (value instanceof List) {
         List<?> arr = (List<?>)value;
         
         for (Object obj : arr) {
            if (obj == null || obj == WILDCARD) {
               result.add(".");
            } else if (obj instanceof Number) {
               result.add(ProtocUtil.toHexString(((Number)obj).byteValue()));
            } else if (obj instanceof CharSequence) {
               if ("".equals(obj) || ".".equals(obj) || "*".equals(obj)) {
                  result.add(".");
               } else {
                  throw new RuntimeException("value " + obj + " cannot be used in regex match");
               }
            } else {
               throw new RuntimeException("value " + obj + " cannot be used in regex match");
            }
         }
      } else {
         throw new RuntimeException("value " + value + " cannot be used in regex match");
      }

      return result.build();
   }
}


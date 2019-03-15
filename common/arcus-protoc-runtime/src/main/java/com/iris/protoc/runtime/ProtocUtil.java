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
package com.iris.protoc.runtime;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.util.Collection;

public final class ProtocUtil {
   private static final String START = "[";
   private static final String SEP= ",";
   private static final String END = "]";

   public static String toHexString(byte value) {
      return toHexString(value, 1);
   }

   // NOTE: This prevents the byte value from being delivered
   //       to one of the other methods due to overloading.
   public static String toHexString(byte value, String sep) {
      return toHexString(value, 1, sep);
   }

   public static String toHexString(short value) {
      return toHexString(value, "");
   }

   public static String toHexString(short value, String sep) {
      return toHexString(value, 2, sep);
   }

   public static String toHexString(int value) {
      return toHexString(value, "");
   }

   public static String toHexString(int value, String sep) {
      return toHexString(value, 4, sep);
   }

   public static String toHexString(long value) {
      return toHexString(value, "");
   }

   public static String toHexString(long value, String sep) {
      return toHexString(value, 8, sep);
   }

   public static String toHexString(byte[] value) {
      return toHexString(value, START, SEP, END);
   }

   public static String toHexString(byte[] value, String start, String sep, String end) {
      if (value == null) return "null";

      StringBuilder builder = new StringBuilder(3*value.length + 1);
      builder.append(start);
      for (int i = 0; i < value.length; ++i) {
         if (i != 0) builder.append(sep);
         toHexString(builder, value[i], 1);
      }
      builder.append(end);
      return builder.toString();
   }

   public static String toHexString(short[] value) {
      return toHexString(value, START, SEP, END);
   }

   public static String toHexString(short[] value, String start, String sep, String end) {
      if (value == null) return "null";


      StringBuilder builder = new StringBuilder(5*value.length + 1);
      builder.append(start);
      for (int i = 0; i < value.length; ++i) {
         if (i != 0) builder.append(sep);
         toHexString(builder, value[i], 2);
      }
      builder.append(end);
      return builder.toString();
   }

   public static String toHexString(int[] value) {
      return toHexString(value, START, SEP, END);
   }

   public static String toHexString(int[] value, String start, String sep, String end) {
      if (value == null) return "null";

      StringBuilder builder = new StringBuilder(9*value.length + 1);
      builder.append(start);
      for (int i = 0; i < value.length; ++i) {
         if (i != 0) builder.append(sep);
         toHexString(builder, value[i], 4);
      }
      builder.append(end);
      return builder.toString();
   }

   public static String toHexString(long[] value) {
      return toHexString(value, START, SEP, END);
   }

   public static String toHexString(long[] value, String start, String sep, String end) {
      if (value == null) return "null";


      StringBuilder builder = new StringBuilder(17*value.length + 1);
      builder.append(start);
      for (int i = 0; i < value.length; ++i) {
         if (i != 0) builder.append(sep);
         toHexString(builder, value[i], 8);
      }
      builder.append(end);
      return builder.toString();
   }

   public static String toHexString(ByteBuf value) {
      return toHexString(value, START, SEP, END);
   }

   public static String toHexString(ByteBuf value, String start, String sep, String end) {
      if (value == null) return "null";

      StringBuilder builder = new StringBuilder(3*value.readableBytes() + 1);
      builder.append(start);
      for (int i = value.readerIndex(), e = i + value.readableBytes(); i < e; ++i) {
         if (builder.length() > 1) builder.append(sep);
         toHexString(builder, value.getByte(i), 1);
      }
      builder.append(end);
      return builder.toString();
   }

   public static String toHexString(ByteBuffer value) {
      return toHexString(value, START, SEP, END);
   }

   public static String toHexString(ByteBuffer value, String start, String sep, String end) {
      if (value == null) return "null";

      StringBuilder builder = new StringBuilder(3*value.remaining() + 1);
      builder.append(start);
      for (int i = value.position(), e = value.limit(); i < e; ++i) {
         if (i != 0) builder.append(sep);
         toHexString(builder, value.get(i), 1);
      }
      builder.append(end);
      return builder.toString();
   }

   private static char[] HEXTABLE = new char[] {
      '0', '1', '2', '3', '4', '5', '6', '7',
      '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
   };

   public static String toHexString(long value, int bytes) {
      return toHexString(value, bytes, "");
   }

   public static String toHexString(long value, int bytes, String sep) {
      StringBuilder builder = new StringBuilder(2*bytes);
      toHexString(builder, value, bytes, sep);
      return builder.toString();
   }

   public static void toHexString(StringBuilder builder, long value, int bytes) {
      toHexString(builder, value, bytes, "");
   }

   public static void toHexString(StringBuilder builder, long value, int bytes, String sep) {
      boolean isEmpty = sep == null || sep.isEmpty();

      int shift = 4 * (2*bytes - 1);
      for (int i = 0; i < 2*bytes; ++i) {
         if (!isEmpty && i != 0 && (i % 2) == 0) builder.append(sep);

         int next = (int)((value >> shift) & 0xF);
         builder.append(HEXTABLE[next]);
         shift -= 4;
      }
   }

   public static byte[] toByteArray(Collection<Byte> list) {
      if (list == null || list.isEmpty()) {
         return new byte[0];
      }

      int i = 0;
      byte[] result = new byte[list.size()];
      for (Byte next : list) {
         result[i++] = next.byteValue();
      }

      return result;
   }

   public static short[] toShortArray(Collection<Short> list) {
      if (list == null || list.isEmpty()) {
         return new short[0];
      }

      int i = 0;
      short[] result = new short[list.size()];
      for (Short next : list) {
         result[i++] = next.shortValue();
      }

      return result;
   }

   public static int[] toIntArray(Collection<Integer> list) {
      if (list == null || list.isEmpty()) {
         return new int[0];
      }

      int i = 0;
      int[] result = new int[list.size()];
      for (Integer next : list) {
         result[i++] = next.intValue();
      }

      return result;
   }

   public static long[] toLongArray(Collection<Long> list) {
      if (list == null || list.isEmpty()) {
         return new long[0];
      }

      int i = 0;
      long[] result = new long[list.size()];
      for (Long next : list) {
         result[i++] = next.longValue();
      }

      return result;
   }

   public static float[] toFloatArray(Collection<Float> list) {
      if (list == null || list.isEmpty()) {
         return new float[0];
      }

      int i = 0;
      float[] result = new float[list.size()];
      for (Float next : list) {
         result[i++] = next.floatValue();
      }

      return result;
   }

   public static double[] toDoubleArray(Collection<Double> list) {
      if (list == null || list.isEmpty()) {
         return new double[0];
      }

      int i = 0;
      double[] result = new double[list.size()];
      for (Double next : list) {
         result[i++] = next.doubleValue();
      }

      return result;
   }
}


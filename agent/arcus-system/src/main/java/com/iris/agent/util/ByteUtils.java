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
package com.iris.agent.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.io.BaseEncoding;

public class ByteUtils {
   public static final byte[] EMPTY_ARRAY = new byte[0];

   public static byte[] fill(byte[] target, int value) {
      return fill(target, (byte) value);
   }

   public static byte[] fill(byte[] target, byte value) {
      if (target == null) {
         return null;
      }
      for (int i = 0; i < target.length; i++) {
         target[i] = value;
      }
      return target;
   }

   public static byte[] clone(byte[] src) {
      if (src == null) {
         return null;
      }
      return Arrays.copyOf(src, src.length);
   }
   
   public static byte[] toByteArray(byte b) {
      byte[] bytes = new byte[1];
      bytes[0] = b;
      return bytes;
   }
   
   public static byte[] concat(byte[]...arrays) {
      if (arrays == null || arrays.length == 0) {
         return new byte[0];
      }
      
      int totalLength = 0;
      for (byte[] array : arrays) {
         if (array != null) {
            totalLength += array.length;
         }
      }
      
      final byte[] returnBytes = new byte[totalLength];
      int pos = 0;
      for (byte[] array : arrays) {
         if (array != null) {
            System.arraycopy(array, 0, returnBytes, pos, array.length);
            pos += array.length;
         }
      }
      return returnBytes;
   }

   public static byte[] prepend(byte[] a, byte b) {
      if (a == null) {
         return new byte[] { b };
      }

      int length = a.length;
      byte[] result = new byte[length + 1];
      System.arraycopy(a, 0, result, 1, length);
      result[0] = b;
      return result;
   }
   
   public static byte[] ints2Bytes(Collection<Integer> vals) {
      if (vals != null) {
         final byte[] buffer = new byte[vals.size()];
         int i = 0;
         for(int x : vals) {
            buffer[i] = (byte)x;
            i++;
         }
         return buffer;
      }
      else {
         return new byte[0];
      }
   }

   public static byte[] ints2Bytes(int... vals) {
      if (vals != null && vals.length > 0) {
         byte[] bytes = new byte[vals.length];
         for (int i = 0; i < vals.length; i++) {
            bytes[i] = (byte) vals[i];
         }
         return bytes;
      } else {
         return new byte[0];
      }
   }
   
   /**
    * Convert 2 bytes to 16 bit integer using big-endian byte ordering.
    * 
    * @param bytes byte array
    * @return 16 bit integer
    */
   public static int from16BitToInt(byte[] bytes) {
      return from16BitToInt(bytes, 0);
   }
   
   /**
    * Convert 2 bytes to 16 bit integer using big-endian byte ordering.
    * 
    * @param bytes byte array
    * @param offset in byte array
    * @return 16 bit integer
    */
   public static int from16BitToInt(byte[] bytes, int off) {
      return (bytes[0 + off] << 8) + bytes[1 + off];
   }
   
   /**
    * Convert 3 bytes to 24 bit integer using big-endian byte ordering.
    * 
    * @param bytes byte array
    * @return 24 bit integer
    */
   public static int from24BitToInt(byte[] bytes) {
      return from24BitToInt(bytes, 0);
   }
   
   /**
    * Convert 3 bytes to 24 bit integer using big-endian byte ordering.
    * 
    * @param bytes byte array
    * @param offset in byte array
    * @return 24 bit integer
    */
   public static int from24BitToInt(byte[] bytes, int off) {
      return (bytes[0 + off] << 16) + (bytes[1 + off] << 8) + bytes[2 + off];
   }
   
   /**
    * Convert 4 bytes to 32 bit integer using big-endian byte ordering.
    * 
    * @param bytes byte array
    * @return 32 bit integer
    */
   public static int from32BitToInt(byte[] bytes) {
      return from32BitToInt(bytes, 0);
   }
   
   /**
    * Convert 4 bytes to 32 bit integer using big-endian byte ordering.
    * 
    * @param bytes byte array
    * @param offset in byte array
    * @return 32 bit integer
    */
   public static int from32BitToInt(byte[] bytes, int off) {
      return (bytes[0 + off] << 24) + (bytes[1 + off] << 16) + (bytes[2 + off] << 8) + bytes[3 + off];
   }
   
   public static byte[] to8Bits(int i) {
      return toByteArray((byte)i);
   }
   
   public static byte[] to16Bits(int i) {
      byte[] bytes = new byte[2];
      bytes[0] = (byte)((0x0000FF00 & i) >> 8);
      bytes[1] = (byte)((0x000000FF & i));
      return bytes;
   }
   
   public static byte[] to24Bits(int i) {
      byte[] bytes = new byte[3];
      bytes[0] = (byte)((0x00FF0000 & i) >> 16);
      bytes[1] = (byte)((0x0000FF00 & i) >> 8);
      bytes[2] = (byte)((0x000000FF & i));
      return bytes;
   }
   
   public static byte[] to32Bits(int i) {
      byte[] bytes = new byte[4];
      bytes[0] = (byte)((0xFF000000 & i) >> 24);
      bytes[1] = (byte)((0x00FF0000 & i) >> 16);
      bytes[2] = (byte)((0x0000FF00 & i) >> 8);
      bytes[3] = (byte)((0x000000FF & i));
      return bytes;
   }
   
   public static byte[] to32Bits(long l) {
      byte[] bytes = new byte[4];
      bytes[0] = (byte)((0xFF000000 & l) >> 24);
      bytes[1] = (byte)((0x00FF0000 & l) >> 16);
      bytes[2] = (byte)((0x0000FF00 & l) >> 8);
      bytes[3] = (byte)((0x000000FF & l));
      return bytes;
   }
   
   public static int setFlags(boolean bit7, boolean bit6, boolean bit5, boolean bit4, boolean bit3, boolean bit2, boolean bit1, boolean bit0) {
      return 
              (bit7 ? BitMasks.BIT_7 : 0)
            | (bit6 ? BitMasks.BIT_6 : 0)
            | (bit5 ? BitMasks.BIT_5 : 0)
            | (bit4 ? BitMasks.BIT_4 : 0)
            | (bit3 ? BitMasks.BIT_3 : 0)
            | (bit2 ? BitMasks.BIT_2 : 0)
            | (bit1 ? BitMasks.BIT_1 : 0)
            | (bit0 ? BitMasks.BIT_0 : 0);
   }
   
   /**
    * Sets a specific bit in a byte to 1. Any bits already
    * set to 1 will remain set to 1.
    * 
    * The bit index is a value from 0 to 7 where
    * 7 is MSB and 0 is LSB.
    * 
    * Bits
    * 7  6  5  4  3  2  1  0
    * 
    * @param b the original byte to set a bit in
    * @param bitIndex the index of the bit to set
    * @return the new value
    */
   public static byte setBit(byte b, int bitIndex) {
      return (byte)((1 << bitIndex) | (0x00FF & b));
   }
   
   public static boolean isSet(int mask, int b) {
      return (mask & b) > 0;
   }
   
   public static int[] byteArray2Ints(byte[] bytes, int offset, int length) {
      int[] vals = new int[length];
      for (int i = offset; ((i - offset) < length && i < bytes.length); i++) {
         vals[i - offset] = 0x00FF & bytes[i];
      }
      return vals;
   }
   
   public static List<String> byteArray2StringList(byte[] bytes) {
      List<String> toBytes = new ArrayList<>();
      if (bytes != null && bytes.length > 0) {
         for (int i = 0; i < bytes.length; i++) {
            toBytes.add(String.format("%02x", 0x00FF & bytes[i]));
         }
      }
      return toBytes;
   }

   public static String byteArray2SpacedString(byte[] bytes) {
      return Joiner.on(' ').join(byteArray2StringList(bytes));
   }
   
   public static String byteArray2StringBlock(byte[] bytes, int cols) {
      return byteArray2StringBlock(Integer.MAX_VALUE, bytes, cols, 0, 0);
   }
   
   public static String byteArray2StringBlock(int len, byte[] bytes, int cols) {
      return byteArray2StringBlock(len, bytes, cols, 0, 0);
   }
   
   public static String byteArray2StringBlock(byte[] bytes, int cols, int indent) {
      return byteArray2StringBlock(Integer.MAX_VALUE, bytes, cols, indent, indent);
   }
   
   public static String byteArray2StringBlock(int len, byte[] bytes, int cols, int indent) {
      return byteArray2StringBlock(len, bytes, cols, indent, indent);
   }
   
   public static String byteArray2StringBlock(byte[] bytes, int cols, int indent, int firstIndent) {
      return byteArray2StringBlock(Integer.MAX_VALUE, bytes, cols, indent, firstIndent);
   }
   
   public static String byteArray2StringBlock(int len, byte[] bytes, int cols, int indent, int firstIndent) {
      List<String> toBytes = byteArray2StringList(bytes);
      StringBuffer sb = new StringBuffer();
      String indentStr = indent(firstIndent);
      String indentOngoing = indent(indent);
      int offset = 0;
      int length = Math.min(len, toBytes.size());
      while (offset < length) {
         sb.append(indentStr);
         for(int i = offset; i < Math.min(offset + cols, length); i++) {
            if (i > offset) {
               sb.append(' ');
            }
            sb.append(toBytes.get(i));
         }
         sb.append('\n');
         indentStr = indentOngoing;
         offset += cols;
      }
      return sb.toString();
   }

   public static byte[] string2bytes(String byteStr) {
      if (byteStr != null) {
         String processed = byteStr.trim().toUpperCase().replaceAll("\\s", "");
         return BaseEncoding.base16().decode(processed);
      } else {
         return new byte[0];
      }
   }
   
   public static String print(byte b) {
      return print(0x00FF & b);
   }
   
   public static String print(int i) {
      return String.format("%02x", i);
   }
   
   private static String indent(int indent) {
      StringBuffer sb = new StringBuffer(indent);
      for (int i = 0; i < indent; i++) {
         sb.append(' ');
      }
      return sb.toString();
   }
}


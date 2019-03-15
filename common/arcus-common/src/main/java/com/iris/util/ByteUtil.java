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
package com.iris.util;


public class ByteUtil {
   
   public static byte [] intToBytes(int value) {
      byte [] buffer = new byte[4];
      intToBytes(value, buffer, 0);
      return buffer;
   }
   
   public static void intToBytes(int value, byte[] buffer, int offset) {
      // write the last byte first to cause an ArrayIndexOutOfBounds early (before the buffer has been modified)
      buffer[offset + 3] = (byte) (value & 0xff);
      buffer[offset + 2] = (byte) ((value >>  8) & 0xff);
      buffer[offset + 1] = (byte) ((value >> 16) & 0xff);
      buffer[offset + 0] = (byte) ((value >> 24) & 0xff);
   }
   
   public static int bytesToInt(byte[] buffer) {
      return bytesToInt(buffer, 0);
   }
   
   public static int bytesToInt(byte[] buffer, int offset) {
      int value = 0;
      value |=  ((int)buffer[offset + 3]) & 0xff;
      value |= (((int)buffer[offset + 2]) & 0xff) <<  8;
      value |= (((int)buffer[offset + 1]) & 0xff) << 16;
      value |= (((int)buffer[offset + 0]) & 0xff) << 24;
      return value;
   }

   public static String toHex(byte[] bytes) {
      return Hex.toPrint(bytes);
   }

   public static byte[] xor(byte[] a, byte[] b) {
      int n = a.length;
      if (b.length < n) {
         n = b.length;
      }
      byte[] c = new byte[n];

      return xor(a, b, c);
   }

   public static byte[] xor(byte[] a, byte[] b, byte[] out) {
      int n = a.length;
      if (b.length < n) {
         n = b.length;
      }
      if ( out.length < n ) {
         n = out.length;
      }
      for (int i = 0; i < n; i++) {
         out[i] = (byte) (a[i] ^ b[i]);
      }
      return out;
   }

   public static byte[] pad16(byte[] bytes) {
      return pad(bytes, 16);
   }

   public static byte[] pad(byte[] bytes, int size) {
      return pad(bytes, size, (byte) 0);
   }

   public static byte[] pad(byte[] bytes, int size, byte value) {

      int toAdd = size - (bytes.length % size);
      if (toAdd == size) {
         toAdd = 0;
      }
      byte[] padded = new byte[bytes.length + toAdd];

      return pad(bytes, padded, size, value);
   }

   public static byte[] pad(byte[] in, byte[] out, int size, byte value) {
      for (int i = 0; i < out.length; i++) {
         out[i] = value;
      }
      System.arraycopy(in, 0, out, 0, in.length);
      return out;
   }

   public static boolean checkMask(byte value, byte mask) {
      return ((value & mask) == mask);
   }
   
}


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
package com.iris.protocol.zigbee;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.iris.protocol.zigbee.zcl.Constants.*;

public final class ZclDataUtil {
   private static final Logger log = LoggerFactory.getLogger(ZclDataUtil.class);

   public static final int LENGTH_MIN = 1;
   public static final int LENGTH_MAX = -1;

   public static final Calendar ZIGBEE_EPOCH;
   public static final Object NONE = new Object() {
      @Override
      public String toString() {
         return "INVALID";
      }
   };

   static {
      Calendar zec = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
      zec.set(2000, 0, 1, 0, 0, 0);

      ZIGBEE_EPOCH = zec;
   }

   public static boolean isAnalog(byte dataType) {
      switch (dataType) {
      case ZB_TYPE_UNSIGNED_8BIT:
      case ZB_TYPE_UNSIGNED_16BIT:
      case ZB_TYPE_UNSIGNED_24BIT:
      case ZB_TYPE_UNSIGNED_32BIT:
      case ZB_TYPE_UNSIGNED_40BIT:
      case ZB_TYPE_UNSIGNED_48BIT:
      case ZB_TYPE_UNSIGNED_56BIT:
      case ZB_TYPE_UNSIGNED_64BIT:
      case ZB_TYPE_SIGNED_8BIT:
      case ZB_TYPE_SIGNED_16BIT:
      case ZB_TYPE_SIGNED_24BIT:
      case ZB_TYPE_SIGNED_32BIT:
      case ZB_TYPE_SIGNED_40BIT:
      case ZB_TYPE_SIGNED_48BIT:
      case ZB_TYPE_SIGNED_56BIT:
      case ZB_TYPE_SIGNED_64BIT:
      case ZB_TYPE_SEMIFLOAT:
      case ZB_TYPE_FLOAT:
      case ZB_TYPE_DOUBLE:
      case ZB_TYPE_TIME_OF_DAY:
      case ZB_TYPE_DATE:
      case ZB_TYPE_UTCTIME:
         return true;
      default:
         return false;
      }
   }

   public static int extraSize(byte dataType) {
      switch (dataType) {
      case ZB_TYPE_STRING_OCTET:
      case ZB_TYPE_STRING_CHAR:
         return 1;

      case ZB_TYPE_LONG_STRING_OCTET:
      case ZB_TYPE_LONG_STRING_CHAR:
      case ZB_TYPE_ARRAY:
      case ZB_TYPE_STRUCT:
      case ZB_TYPE_SET:
      case ZB_TYPE_BAG:
         return 2;
      }

      return 0;
   }

   public static void writeValue(DataOutput output, byte dataType, byte[] dataValue) throws IOException {
      switch (dataType) {
      case ZB_TYPE_STRING_OCTET:
      case ZB_TYPE_STRING_CHAR:
         output.writeByte(dataValue.length);
         break;

      case ZB_TYPE_LONG_STRING_OCTET:
      case ZB_TYPE_LONG_STRING_CHAR:
      case ZB_TYPE_ARRAY:
      case ZB_TYPE_STRUCT:
      case ZB_TYPE_SET:
      case ZB_TYPE_BAG:
         output.writeShort(dataValue.length);
         break;
      }

      output.write(dataValue);
   }

   public static void writeValue(ByteBuffer output, byte dataType, byte[] dataValue) throws IOException {
      switch (dataType) {
      case ZB_TYPE_STRING_OCTET:
      case ZB_TYPE_STRING_CHAR:
         output.put((byte)dataValue.length);
         break;

      case ZB_TYPE_LONG_STRING_OCTET:
      case ZB_TYPE_LONG_STRING_CHAR:
      case ZB_TYPE_ARRAY:
      case ZB_TYPE_STRUCT:
      case ZB_TYPE_SET:
      case ZB_TYPE_BAG:
         output.putShort((short)dataValue.length);
         break;
      }

      output.put(dataValue);
   }

   public static void writeValue(ByteBuf output, byte dataType, byte[] dataValue) throws IOException {
      switch (dataType) {
      case ZB_TYPE_STRING_OCTET:
      case ZB_TYPE_STRING_CHAR:
         output.writeByte(dataValue.length);
         break;

      case ZB_TYPE_LONG_STRING_OCTET:
      case ZB_TYPE_LONG_STRING_CHAR:
      case ZB_TYPE_ARRAY:
      case ZB_TYPE_STRUCT:
      case ZB_TYPE_SET:
      case ZB_TYPE_BAG:
         output.writeShort(dataValue.length);
         break;
      }

      output.writeBytes(dataValue);
   }

   public static byte[] readValue(DataInput input, byte dataType) throws IOException {
      byte[] results = new byte[valueSize(dataType,input)];
      input.readFully(results);
      return results;
   }

   public static byte[] readValue(ByteBuffer input, byte dataType) throws IOException {
      byte[] results = new byte[valueSize(dataType,input)];
      input.get(results);
      return results;
   }

   public static byte[] readValue(ByteBuf input, byte dataType) throws IOException {
      byte[] results = new byte[valueSize(dataType,input)];
      input.readBytes(results);
      return results;
   }

   private static int valueSize(byte dataType, DataInput input) throws IOException {
      switch (dataType) {
      case ZB_TYPE_STRING_OCTET: return stringLength(1, input);
      case ZB_TYPE_STRING_CHAR: return stringLength(1, input);
      case ZB_TYPE_LONG_STRING_OCTET: return stringLength(2, input);
      case ZB_TYPE_LONG_STRING_CHAR: return stringLength(2, input);
      case ZB_TYPE_ARRAY: return stringLength(2, input);
      case ZB_TYPE_STRUCT: return stringLength(2, input);
      case ZB_TYPE_SET: return stringLength(2, input);
      case ZB_TYPE_BAG: return stringLength(2, input);
      default: return dataValueSize(dataType, null);
      }
   }

   private static int valueSize(byte dataType, ByteBuffer input) throws IOException {
      switch (dataType) {
      case ZB_TYPE_STRING_OCTET: return stringLength(1, input);
      case ZB_TYPE_STRING_CHAR: return stringLength(1, input);
      case ZB_TYPE_LONG_STRING_OCTET: return stringLength(2, input);
      case ZB_TYPE_LONG_STRING_CHAR: return stringLength(2, input);
      case ZB_TYPE_ARRAY: return stringLength(2, input);
      case ZB_TYPE_STRUCT: return stringLength(2, input);
      case ZB_TYPE_SET: return stringLength(2, input);
      case ZB_TYPE_BAG: return stringLength(2, input);
      default: return dataValueSize(dataType, null);
      }
   }

   private static int valueSize(byte dataType, ByteBuf input) throws IOException {
      switch (dataType) {
      case ZB_TYPE_STRING_OCTET: return stringLength(1, input);
      case ZB_TYPE_STRING_CHAR: return stringLength(1, input);
      case ZB_TYPE_LONG_STRING_OCTET: return stringLength(2, input);
      case ZB_TYPE_LONG_STRING_CHAR: return stringLength(2, input);
      case ZB_TYPE_ARRAY: return stringLength(2, input);
      case ZB_TYPE_STRUCT: return stringLength(2, input);
      case ZB_TYPE_SET: return stringLength(2, input);
      case ZB_TYPE_BAG: return stringLength(2, input);
      default: return dataValueSize(dataType, null);
      }
   }

   private static int stringLength(int byteLength, DataInput input) throws IOException {
      if (byteLength == 1) {
         byte length = input.readByte();
         return ((length & 0xFF) == 0xFF) ? 0 : length;
      }

      short length = input.readShort();
      return ((length & 0xFFFF) == 0xFFFF) ? 0 : length;
   }

   private static int stringLength(int byteLength, ByteBuffer input) throws IOException {
      if (byteLength == 1) {
         byte length = input.get();
         return ((length & 0xFF) == 0xFF) ? 0 : length;
      }

      short length = input.getShort();
      return ((length & 0xFFFF) == 0xFFFF) ? 0 : length;
   }

   private static int stringLength(int byteLength, ByteBuf input) throws IOException {
      if (byteLength == 1) {
         byte length = input.readByte();
         return ((length & 0xFF) == 0xFF) ? 0 : length;
      }

      short length = input.readShort();
      return ((length & 0xFFFF) == 0xFFFF) ? 0 : length;
   }

   public static int dataValueSize(byte dataType, Object dataValue) {
      switch (dataType) {
      case ZB_TYPE_NO_DATA: return 0;
      case ZB_TYPE_8BIT: return 1;
      case ZB_TYPE_16BIT: return 2;
      case ZB_TYPE_24BIT: return 3;
      case ZB_TYPE_32BIT: return 4;
      case ZB_TYPE_40BIT: return 5;
      case ZB_TYPE_48BIT: return 6;
      case ZB_TYPE_56BIT: return 7;
      case ZB_TYPE_64BIT: return 8;
      case ZB_TYPE_BOOLEAN: return 1;
      case ZB_TYPE_BITMAP_8BIT: return 1;
      case ZB_TYPE_BITMAP_16BIT: return 2;
      case ZB_TYPE_BITMAP_24BIT: return 3;
      case ZB_TYPE_BITMAP_32BIT: return 4;
      case ZB_TYPE_BITMAP_40BIT: return 5;
      case ZB_TYPE_BITMAP_48BIT: return 6;
      case ZB_TYPE_BITMAP_56BIT: return 7;
      case ZB_TYPE_BITMAP_64BIT: return 8;
      case ZB_TYPE_UNSIGNED_8BIT: return 1;
      case ZB_TYPE_UNSIGNED_16BIT: return 2;
      case ZB_TYPE_UNSIGNED_24BIT: return 3;
      case ZB_TYPE_UNSIGNED_32BIT: return 4;
      case ZB_TYPE_UNSIGNED_40BIT: return 5;
      case ZB_TYPE_UNSIGNED_48BIT: return 6;
      case ZB_TYPE_UNSIGNED_56BIT: return 7;
      case ZB_TYPE_UNSIGNED_64BIT: return 8;
      case ZB_TYPE_SIGNED_8BIT: return 1;
      case ZB_TYPE_SIGNED_16BIT: return 2;
      case ZB_TYPE_SIGNED_24BIT: return 3;
      case ZB_TYPE_SIGNED_32BIT: return 4;
      case ZB_TYPE_SIGNED_40BIT: return 5;
      case ZB_TYPE_SIGNED_48BIT: return 6;
      case ZB_TYPE_SIGNED_56BIT: return 7;
      case ZB_TYPE_SIGNED_64BIT: return 8;
      case ZB_TYPE_ENUM_8BIT: return 1;
      case ZB_TYPE_ENUM_16BIT: return 2;
      case ZB_TYPE_SEMIFLOAT: return 2;
      case ZB_TYPE_FLOAT: return 4;
      case ZB_TYPE_DOUBLE: return 8;
      case ZB_TYPE_STRING_OCTET: return 1 + ((byte[])dataValue).length;
      case ZB_TYPE_STRING_CHAR: return 1 + stringValueSize((String)dataValue);
      case ZB_TYPE_LONG_STRING_OCTET: return 2 + ((byte[])dataValue).length;
      case ZB_TYPE_LONG_STRING_CHAR: return 2 + stringValueSize((String)dataValue);
      case ZB_TYPE_ARRAY: throw new UnsupportedOperationException();
      case ZB_TYPE_STRUCT: throw new UnsupportedOperationException();
      case ZB_TYPE_SET: throw new UnsupportedOperationException();
      case ZB_TYPE_BAG: throw new UnsupportedOperationException();
      case ZB_TYPE_TIME_OF_DAY: return 4;
      case ZB_TYPE_DATE: return 4;
      case ZB_TYPE_UTCTIME: return 4;
      case ZB_TYPE_CLUSTER_ID: return 2;
      case ZB_TYPE_ATTR_ID: return 2;
      case ZB_TYPE_BACNET_OID: return 4;
      case ZB_TYPE_IEEE: return 8;
      case ZB_TYPE_KEY_128BIT: return 16;
      default: throw new RuntimeException("unknown zigbee data type: " + (dataType & 0xFF));
      }
   }

   public static int stringValueSize(String value) {
      // TODO: charset encoding?
      byte[] backing = value.getBytes(StandardCharsets.UTF_8);
      return backing.length;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Marshalling to/from Java IO streams
   /////////////////////////////////////////////////////////////////////////////

   public static Object decode(byte dataType, DataInput input) throws IOException {
      Object dataValue;
      switch (dataType) {
      case ZB_TYPE_NO_DATA: { dataValue = NONE; } break;
      case ZB_TYPE_8BIT: { dataValue = readBit8(input,false,false); } break;
      case ZB_TYPE_16BIT: { dataValue = readBit16(input,false,false); } break;
      case ZB_TYPE_24BIT: { dataValue = readBit24(input,false,false); } break;
      case ZB_TYPE_32BIT: { dataValue = readBit32(input,false,false); } break;
      case ZB_TYPE_40BIT: { dataValue = readBit40(input,false,false); } break;
      case ZB_TYPE_48BIT: { dataValue = readBit48(input,false,false); } break;
      case ZB_TYPE_56BIT: { dataValue = readBit56(input,false,false); } break;
      case ZB_TYPE_64BIT: { dataValue = readBit64(input,false,false); } break;
      case ZB_TYPE_BOOLEAN: { dataValue = readBoolean(input); } break;
      case ZB_TYPE_BITMAP_8BIT: { dataValue = readBit8(input,false,false); } break;
      case ZB_TYPE_BITMAP_16BIT: { dataValue = readBit16(input,false,false); } break;
      case ZB_TYPE_BITMAP_24BIT: { dataValue = readBit24(input,false,false); } break;
      case ZB_TYPE_BITMAP_32BIT: { dataValue = readBit32(input,false,false); } break;
      case ZB_TYPE_BITMAP_40BIT: { dataValue = readBit40(input,false,false); } break;
      case ZB_TYPE_BITMAP_48BIT: { dataValue = readBit48(input,false,false); } break;
      case ZB_TYPE_BITMAP_56BIT: { dataValue = readBit56(input,false,false); } break;
      case ZB_TYPE_BITMAP_64BIT: { dataValue = readBit64(input,false,false); } break;
      case ZB_TYPE_UNSIGNED_8BIT: { dataValue = readBit8(input,true,false); } break;
      case ZB_TYPE_UNSIGNED_16BIT: { dataValue = readBit16(input,true,false); } break;
      case ZB_TYPE_UNSIGNED_24BIT: { dataValue = readBit24(input,true,false); } break;
      case ZB_TYPE_UNSIGNED_32BIT: { dataValue = readBit32(input,true,false); } break;
      case ZB_TYPE_UNSIGNED_40BIT: { dataValue = readBit40(input,true,false); } break;
      case ZB_TYPE_UNSIGNED_48BIT: { dataValue = readBit48(input,true,false); } break;
      case ZB_TYPE_UNSIGNED_56BIT: { dataValue = readBit56(input,true,false); } break;
      case ZB_TYPE_UNSIGNED_64BIT: { dataValue = readBit64(input,true,false); } break;
      case ZB_TYPE_SIGNED_8BIT: { dataValue = readBit8(input,true,true); } break;
      case ZB_TYPE_SIGNED_16BIT: { dataValue = readBit16(input,true,true); } break;
      case ZB_TYPE_SIGNED_24BIT: { dataValue = readBit24(input,true,true); } break;
      case ZB_TYPE_SIGNED_32BIT: { dataValue = readBit32(input,true,true); } break;
      case ZB_TYPE_SIGNED_40BIT: { dataValue = readBit40(input,true,true); } break;
      case ZB_TYPE_SIGNED_48BIT: { dataValue = readBit48(input,true,true); } break;
      case ZB_TYPE_SIGNED_56BIT: { dataValue = readBit56(input,true,true); } break;
      case ZB_TYPE_SIGNED_64BIT: { dataValue = readBit64(input,true,true); } break;
      case ZB_TYPE_ENUM_8BIT: { dataValue = readBit8(input,true,false); } break;
      case ZB_TYPE_ENUM_16BIT: { dataValue = readBit16(input,true,false); } break;
      case ZB_TYPE_SEMIFLOAT: { dataValue = readSemiFloat(input); } break;
      case ZB_TYPE_FLOAT: { dataValue = readFloat(input); } break;
      case ZB_TYPE_DOUBLE: { dataValue = readDouble(input); } break;
      case ZB_TYPE_STRING_OCTET: { dataValue = readByteString(input,1); } break;
      case ZB_TYPE_STRING_CHAR: { dataValue = readString(input,1); } break;
      case ZB_TYPE_LONG_STRING_OCTET: { dataValue = readByteString(input,2); } break;
      case ZB_TYPE_LONG_STRING_CHAR: { dataValue = readString(input,2); } break;
      case ZB_TYPE_ARRAY: { dataValue = readArray(input); } break;
      case ZB_TYPE_STRUCT: { dataValue = readStruct(input); } break;
      case ZB_TYPE_SET: { dataValue = readSet(input); } break;
      case ZB_TYPE_BAG: { dataValue = readBag(input); } break;
      case ZB_TYPE_TIME_OF_DAY: { dataValue = readTimeOfDay(input); } break;
      case ZB_TYPE_DATE: { dataValue = readDate(input); } break;
      case ZB_TYPE_UTCTIME: { dataValue = readUtcTime(input); } break;
      case ZB_TYPE_CLUSTER_ID: { dataValue = readBit16(input,true,false); } break;
      case ZB_TYPE_ATTR_ID: { dataValue = readBit16(input,true,false); } break;
      case ZB_TYPE_BACNET_OID: { dataValue = readBit32(input,true,false); } break;
      case ZB_TYPE_IEEE: { dataValue = readFixedByteString(input, 8, true); } break;
      case ZB_TYPE_KEY_128BIT: { dataValue = readFixedByteString(input, 16, false); } break;
      default: throw new IOException("unknown zigbee data type: " + dataType);
      }

      return dataValue;
   }

   public static void encode(DataOutput output, byte dataType, Object dataValue) throws IOException {
      switch (dataType) {
      case ZB_TYPE_NO_DATA: { } break;
      case ZB_TYPE_8BIT: { writeBit8(output, dataValue, false); } break;
      case ZB_TYPE_16BIT: { writeBit16(output, dataValue, false); } break;
      case ZB_TYPE_24BIT: { writeBit24(output, dataValue, false); } break;
      case ZB_TYPE_32BIT: { writeBit32(output, dataValue, false); } break;
      case ZB_TYPE_40BIT: { writeBit40(output, dataValue, false); } break;
      case ZB_TYPE_48BIT: { writeBit48(output, dataValue, false); } break;
      case ZB_TYPE_56BIT: { writeBit56(output, dataValue, false); } break;
      case ZB_TYPE_64BIT: { writeBit64(output, dataValue, false); } break;
      case ZB_TYPE_BOOLEAN: { writeBoolean(output, dataValue); } break;
      case ZB_TYPE_BITMAP_8BIT: { writeBit8(output, dataValue, false); } break;
      case ZB_TYPE_BITMAP_16BIT: { writeBit16(output, dataValue, false); } break;
      case ZB_TYPE_BITMAP_24BIT: { writeBit24(output, dataValue, false); } break;
      case ZB_TYPE_BITMAP_32BIT: { writeBit32(output, dataValue, false); } break;
      case ZB_TYPE_BITMAP_40BIT: { writeBit40(output, dataValue, false); } break;
      case ZB_TYPE_BITMAP_48BIT: { writeBit48(output, dataValue, false); } break;
      case ZB_TYPE_BITMAP_56BIT: { writeBit56(output, dataValue, false); } break;
      case ZB_TYPE_BITMAP_64BIT: { writeBit64(output, dataValue, false); } break;
      case ZB_TYPE_UNSIGNED_8BIT: { writeBit8(output, dataValue, false); } break;
      case ZB_TYPE_UNSIGNED_16BIT: { writeBit16(output, dataValue, false); } break;
      case ZB_TYPE_UNSIGNED_24BIT: { writeBit24(output, dataValue, false); } break;
      case ZB_TYPE_UNSIGNED_32BIT: { writeBit32(output, dataValue, false); } break;
      case ZB_TYPE_UNSIGNED_40BIT: { writeBit40(output, dataValue, false); } break;
      case ZB_TYPE_UNSIGNED_48BIT: { writeBit48(output, dataValue, false); } break;
      case ZB_TYPE_UNSIGNED_56BIT: { writeBit56(output, dataValue, false); } break;
      case ZB_TYPE_UNSIGNED_64BIT: { writeBit64(output, dataValue, false); } break;
      case ZB_TYPE_SIGNED_8BIT: { writeBit8(output, dataValue, true); } break;
      case ZB_TYPE_SIGNED_16BIT: { writeBit16(output, dataValue, true); } break;
      case ZB_TYPE_SIGNED_24BIT: { writeBit24(output, dataValue, true); } break;
      case ZB_TYPE_SIGNED_32BIT: { writeBit32(output, dataValue, true); } break;
      case ZB_TYPE_SIGNED_40BIT: { writeBit40(output, dataValue, true); } break;
      case ZB_TYPE_SIGNED_48BIT: { writeBit48(output, dataValue, true); } break;
      case ZB_TYPE_SIGNED_56BIT: { writeBit56(output, dataValue, true); } break;
      case ZB_TYPE_SIGNED_64BIT: { writeBit64(output, dataValue, true); } break;
      case ZB_TYPE_ENUM_8BIT: { writeBit8(output, dataValue, false); } break;
      case ZB_TYPE_ENUM_16BIT: { writeBit16(output, dataValue, false); } break;
      case ZB_TYPE_SEMIFLOAT: { writeSemiFloat(output, dataValue); } break;
      case ZB_TYPE_FLOAT: { writeFloat(output, dataValue); } break;
      case ZB_TYPE_DOUBLE: { writeDouble(output, dataValue); } break;
      case ZB_TYPE_STRING_OCTET: { writeByteString(output, dataValue, 1); } break;
      case ZB_TYPE_STRING_CHAR: { writeString(output, dataValue, 1); } break;
      case ZB_TYPE_LONG_STRING_OCTET: { writeByteString(output, dataValue, 2); } break;
      case ZB_TYPE_LONG_STRING_CHAR: { writeString(output, dataValue, 2); } break;
      case ZB_TYPE_ARRAY: { writeArray(output, dataValue); } break;
      case ZB_TYPE_STRUCT: { writeStruct(output, dataValue); } break;
      case ZB_TYPE_SET: { writeSet(output, dataValue); } break;
      case ZB_TYPE_BAG: { writeBag(output, dataValue); } break;
      case ZB_TYPE_TIME_OF_DAY: { writeTimeOfDay(output, dataValue); } break;
      case ZB_TYPE_DATE: { writeDate(output, dataValue); } break;
      case ZB_TYPE_UTCTIME: { writeUtcTime(output, dataValue); } break;
      case ZB_TYPE_CLUSTER_ID: { writeBit16(output, dataValue, false); } break;
      case ZB_TYPE_ATTR_ID: { writeBit16(output, dataValue, false); } break;
      case ZB_TYPE_BACNET_OID: { writeBit32(output, dataValue, false); } break;
      case ZB_TYPE_IEEE: { writeFixedByteString(output, dataValue, 8); } break;
      case ZB_TYPE_KEY_128BIT: { writeFixedByteString(output, dataValue, 16); } break;
      default: throw new IOException("unknown zigbee data type: " + dataType);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Marshalling to/from Java NIO streams
   /////////////////////////////////////////////////////////////////////////////

   public static Object decode(byte dataType, ByteBuffer input) throws IOException {
      Object dataValue;
      switch (dataType) {
      case ZB_TYPE_NO_DATA: { dataValue = NONE; } break;
      case ZB_TYPE_8BIT: { dataValue = readBit8(input,false,false); } break;
      case ZB_TYPE_16BIT: { dataValue = readBit16(input,false,false); } break;
      case ZB_TYPE_24BIT: { dataValue = readBit24(input,false,false); } break;
      case ZB_TYPE_32BIT: { dataValue = readBit32(input,false,false); } break;
      case ZB_TYPE_40BIT: { dataValue = readBit40(input,false,false); } break;
      case ZB_TYPE_48BIT: { dataValue = readBit48(input,false,false); } break;
      case ZB_TYPE_56BIT: { dataValue = readBit56(input,false,false); } break;
      case ZB_TYPE_64BIT: { dataValue = readBit64(input,false,false); } break;
      case ZB_TYPE_BOOLEAN: { dataValue = readBoolean(input); } break;
      case ZB_TYPE_BITMAP_8BIT: { dataValue = readBit8(input,false,false); } break;
      case ZB_TYPE_BITMAP_16BIT: { dataValue = readBit16(input,false,false); } break;
      case ZB_TYPE_BITMAP_24BIT: { dataValue = readBit24(input,false,false); } break;
      case ZB_TYPE_BITMAP_32BIT: { dataValue = readBit32(input,false,false); } break;
      case ZB_TYPE_BITMAP_40BIT: { dataValue = readBit40(input,false,false); } break;
      case ZB_TYPE_BITMAP_48BIT: { dataValue = readBit48(input,false,false); } break;
      case ZB_TYPE_BITMAP_56BIT: { dataValue = readBit56(input,false,false); } break;
      case ZB_TYPE_BITMAP_64BIT: { dataValue = readBit64(input,false,false); } break;
      case ZB_TYPE_UNSIGNED_8BIT: { dataValue = readBit8(input,true,false); } break;
      case ZB_TYPE_UNSIGNED_16BIT: { dataValue = readBit16(input,true,false); } break;
      case ZB_TYPE_UNSIGNED_24BIT: { dataValue = readBit24(input,true,false); } break;
      case ZB_TYPE_UNSIGNED_32BIT: { dataValue = readBit32(input,true,false); } break;
      case ZB_TYPE_UNSIGNED_40BIT: { dataValue = readBit40(input,true,false); } break;
      case ZB_TYPE_UNSIGNED_48BIT: { dataValue = readBit48(input,true,false); } break;
      case ZB_TYPE_UNSIGNED_56BIT: { dataValue = readBit56(input,true,false); } break;
      case ZB_TYPE_UNSIGNED_64BIT: { dataValue = readBit64(input,true,false); } break;
      case ZB_TYPE_SIGNED_8BIT: { dataValue = readBit8(input,true,true); } break;
      case ZB_TYPE_SIGNED_16BIT: { dataValue = readBit16(input,true,true); } break;
      case ZB_TYPE_SIGNED_24BIT: { dataValue = readBit24(input,true,true); } break;
      case ZB_TYPE_SIGNED_32BIT: { dataValue = readBit32(input,true,true); } break;
      case ZB_TYPE_SIGNED_40BIT: { dataValue = readBit40(input,true,true); } break;
      case ZB_TYPE_SIGNED_48BIT: { dataValue = readBit48(input,true,true); } break;
      case ZB_TYPE_SIGNED_56BIT: { dataValue = readBit56(input,true,true); } break;
      case ZB_TYPE_SIGNED_64BIT: { dataValue = readBit64(input,true,true); } break;
      case ZB_TYPE_ENUM_8BIT: { dataValue = readBit8(input,true,false); } break;
      case ZB_TYPE_ENUM_16BIT: { dataValue = readBit16(input,true,false); } break;
      case ZB_TYPE_SEMIFLOAT: { dataValue = readSemiFloat(input); } break;
      case ZB_TYPE_FLOAT: { dataValue = readFloat(input); } break;
      case ZB_TYPE_DOUBLE: { dataValue = readDouble(input); } break;
      case ZB_TYPE_STRING_OCTET: { dataValue = readByteString(input,1); } break;
      case ZB_TYPE_STRING_CHAR: { dataValue = readString(input,1); } break;
      case ZB_TYPE_LONG_STRING_OCTET: { dataValue = readByteString(input,2); } break;
      case ZB_TYPE_LONG_STRING_CHAR: { dataValue = readString(input,2); } break;
      case ZB_TYPE_ARRAY: { dataValue = readArray(input); } break;
      case ZB_TYPE_STRUCT: { dataValue = readStruct(input); } break;
      case ZB_TYPE_SET: { dataValue = readSet(input); } break;
      case ZB_TYPE_BAG: { dataValue = readBag(input); } break;
      case ZB_TYPE_TIME_OF_DAY: { dataValue = readTimeOfDay(input); } break;
      case ZB_TYPE_DATE: { dataValue = readDate(input); } break;
      case ZB_TYPE_UTCTIME: { dataValue = readUtcTime(input); } break;
      case ZB_TYPE_CLUSTER_ID: { dataValue = readBit16(input,true,false); } break;
      case ZB_TYPE_ATTR_ID: { dataValue = readBit16(input,true,false); } break;
      case ZB_TYPE_BACNET_OID: { dataValue = readBit32(input,true,false); } break;
      case ZB_TYPE_IEEE: { dataValue = readFixedByteString(input, 8, true); } break;
      case ZB_TYPE_KEY_128BIT: { dataValue = readFixedByteString(input, 16, false); } break;
      default: throw new IOException("unknown zigbee data type: " + dataType);
      }

      return dataValue;
   }

   public static void encode(ByteBuffer output, byte dataType, Object dataValue) throws IOException {
      switch (dataType) {
      case ZB_TYPE_NO_DATA: { } break;
      case ZB_TYPE_8BIT: { writeBit8(output, dataValue, false); } break;
      case ZB_TYPE_16BIT: { writeBit16(output, dataValue, false); } break;
      case ZB_TYPE_24BIT: { writeBit24(output, dataValue, false); } break;
      case ZB_TYPE_32BIT: { writeBit32(output, dataValue, false); } break;
      case ZB_TYPE_40BIT: { writeBit40(output, dataValue, false); } break;
      case ZB_TYPE_48BIT: { writeBit48(output, dataValue, false); } break;
      case ZB_TYPE_56BIT: { writeBit56(output, dataValue, false); } break;
      case ZB_TYPE_64BIT: { writeBit64(output, dataValue, false); } break;
      case ZB_TYPE_BOOLEAN: { writeBoolean(output, dataValue); } break;
      case ZB_TYPE_BITMAP_8BIT: { writeBit8(output, dataValue, false); } break;
      case ZB_TYPE_BITMAP_16BIT: { writeBit16(output, dataValue, false); } break;
      case ZB_TYPE_BITMAP_24BIT: { writeBit24(output, dataValue, false); } break;
      case ZB_TYPE_BITMAP_32BIT: { writeBit32(output, dataValue, false); } break;
      case ZB_TYPE_BITMAP_40BIT: { writeBit40(output, dataValue, false); } break;
      case ZB_TYPE_BITMAP_48BIT: { writeBit48(output, dataValue, false); } break;
      case ZB_TYPE_BITMAP_56BIT: { writeBit56(output, dataValue, false); } break;
      case ZB_TYPE_BITMAP_64BIT: { writeBit64(output, dataValue, false); } break;
      case ZB_TYPE_UNSIGNED_8BIT: { writeBit8(output, dataValue, false); } break;
      case ZB_TYPE_UNSIGNED_16BIT: { writeBit16(output, dataValue, false); } break;
      case ZB_TYPE_UNSIGNED_24BIT: { writeBit24(output, dataValue, false); } break;
      case ZB_TYPE_UNSIGNED_32BIT: { writeBit32(output, dataValue, false); } break;
      case ZB_TYPE_UNSIGNED_40BIT: { writeBit40(output, dataValue, false); } break;
      case ZB_TYPE_UNSIGNED_48BIT: { writeBit48(output, dataValue, false); } break;
      case ZB_TYPE_UNSIGNED_56BIT: { writeBit56(output, dataValue, false); } break;
      case ZB_TYPE_UNSIGNED_64BIT: { writeBit64(output, dataValue, false); } break;
      case ZB_TYPE_SIGNED_8BIT: { writeBit8(output, dataValue, true); } break;
      case ZB_TYPE_SIGNED_16BIT: { writeBit16(output, dataValue, true); } break;
      case ZB_TYPE_SIGNED_24BIT: { writeBit24(output, dataValue, true); } break;
      case ZB_TYPE_SIGNED_32BIT: { writeBit32(output, dataValue, true); } break;
      case ZB_TYPE_SIGNED_40BIT: { writeBit40(output, dataValue, true); } break;
      case ZB_TYPE_SIGNED_48BIT: { writeBit48(output, dataValue, true); } break;
      case ZB_TYPE_SIGNED_56BIT: { writeBit56(output, dataValue, true); } break;
      case ZB_TYPE_SIGNED_64BIT: { writeBit64(output, dataValue, true); } break;
      case ZB_TYPE_ENUM_8BIT: { writeBit8(output, dataValue, false); } break;
      case ZB_TYPE_ENUM_16BIT: { writeBit16(output, dataValue, false); } break;
      case ZB_TYPE_SEMIFLOAT: { writeSemiFloat(output, dataValue); } break;
      case ZB_TYPE_FLOAT: { writeFloat(output, dataValue); } break;
      case ZB_TYPE_DOUBLE: { writeDouble(output, dataValue); } break;
      case ZB_TYPE_STRING_OCTET: { writeByteString(output, dataValue, 1); } break;
      case ZB_TYPE_STRING_CHAR: { writeString(output, dataValue, 1); } break;
      case ZB_TYPE_LONG_STRING_OCTET: { writeByteString(output, dataValue, 2); } break;
      case ZB_TYPE_LONG_STRING_CHAR: { writeString(output, dataValue, 2); } break;
      case ZB_TYPE_ARRAY: { writeArray(output, dataValue); } break;
      case ZB_TYPE_STRUCT: { writeStruct(output, dataValue); } break;
      case ZB_TYPE_SET: { writeSet(output, dataValue); } break;
      case ZB_TYPE_BAG: { writeBag(output, dataValue); } break;
      case ZB_TYPE_TIME_OF_DAY: { writeTimeOfDay(output, dataValue); } break;
      case ZB_TYPE_DATE: { writeDate(output, dataValue); } break;
      case ZB_TYPE_UTCTIME: { writeUtcTime(output, dataValue); } break;
      case ZB_TYPE_CLUSTER_ID: { writeBit16(output, dataValue, false); } break;
      case ZB_TYPE_ATTR_ID: { writeBit16(output, dataValue, false); } break;
      case ZB_TYPE_BACNET_OID: { writeBit32(output, dataValue, false); } break;
      case ZB_TYPE_IEEE: { writeFixedByteString(output, dataValue, 8); } break;
      case ZB_TYPE_KEY_128BIT: { writeFixedByteString(output, dataValue, 16); } break;
      default: throw new IOException("unknown zigbee data type: " + dataType);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Marshalling to/from Netty streams
   /////////////////////////////////////////////////////////////////////////////

   public static Object decode(byte dataType, ByteBuf input) throws IOException {
      Object dataValue;
      switch (dataType) {
      case ZB_TYPE_NO_DATA: { dataValue = NONE; } break;
      case ZB_TYPE_8BIT: { dataValue = readBit8(input,false,false); } break;
      case ZB_TYPE_16BIT: { dataValue = readBit16(input,false,false); } break;
      case ZB_TYPE_24BIT: { dataValue = readBit24(input,false,false); } break;
      case ZB_TYPE_32BIT: { dataValue = readBit32(input,false,false); } break;
      case ZB_TYPE_40BIT: { dataValue = readBit40(input,false,false); } break;
      case ZB_TYPE_48BIT: { dataValue = readBit48(input,false,false); } break;
      case ZB_TYPE_56BIT: { dataValue = readBit56(input,false,false); } break;
      case ZB_TYPE_64BIT: { dataValue = readBit64(input,false,false); } break;
      case ZB_TYPE_BOOLEAN: { dataValue = readBoolean(input); } break;
      case ZB_TYPE_BITMAP_8BIT: { dataValue = readBit8(input,false,false); } break;
      case ZB_TYPE_BITMAP_16BIT: { dataValue = readBit16(input,false,false); } break;
      case ZB_TYPE_BITMAP_24BIT: { dataValue = readBit24(input,false,false); } break;
      case ZB_TYPE_BITMAP_32BIT: { dataValue = readBit32(input,false,false); } break;
      case ZB_TYPE_BITMAP_40BIT: { dataValue = readBit40(input,false,false); } break;
      case ZB_TYPE_BITMAP_48BIT: { dataValue = readBit48(input,false,false); } break;
      case ZB_TYPE_BITMAP_56BIT: { dataValue = readBit56(input,false,false); } break;
      case ZB_TYPE_BITMAP_64BIT: { dataValue = readBit64(input,false,false); } break;
      case ZB_TYPE_UNSIGNED_8BIT: { dataValue = readBit8(input,true,false); } break;
      case ZB_TYPE_UNSIGNED_16BIT: { dataValue = readBit16(input,true,false); } break;
      case ZB_TYPE_UNSIGNED_24BIT: { dataValue = readBit24(input,true,false); } break;
      case ZB_TYPE_UNSIGNED_32BIT: { dataValue = readBit32(input,true,false); } break;
      case ZB_TYPE_UNSIGNED_40BIT: { dataValue = readBit40(input,true,false); } break;
      case ZB_TYPE_UNSIGNED_48BIT: { dataValue = readBit48(input,true,false); } break;
      case ZB_TYPE_UNSIGNED_56BIT: { dataValue = readBit56(input,true,false); } break;
      case ZB_TYPE_UNSIGNED_64BIT: { dataValue = readBit64(input,true,false); } break;
      case ZB_TYPE_SIGNED_8BIT: { dataValue = readBit8(input,true,true); } break;
      case ZB_TYPE_SIGNED_16BIT: { dataValue = readBit16(input,true,true); } break;
      case ZB_TYPE_SIGNED_24BIT: { dataValue = readBit24(input,true,true); } break;
      case ZB_TYPE_SIGNED_32BIT: { dataValue = readBit32(input,true,true); } break;
      case ZB_TYPE_SIGNED_40BIT: { dataValue = readBit40(input,true,true); } break;
      case ZB_TYPE_SIGNED_48BIT: { dataValue = readBit48(input,true,true); } break;
      case ZB_TYPE_SIGNED_56BIT: { dataValue = readBit56(input,true,true); } break;
      case ZB_TYPE_SIGNED_64BIT: { dataValue = readBit64(input,true,true); } break;
      case ZB_TYPE_ENUM_8BIT: { dataValue = readBit8(input,true,false); } break;
      case ZB_TYPE_ENUM_16BIT: { dataValue = readBit16(input,true,false); } break;
      case ZB_TYPE_SEMIFLOAT: { dataValue = readSemiFloat(input); } break;
      case ZB_TYPE_FLOAT: { dataValue = readFloat(input); } break;
      case ZB_TYPE_DOUBLE: { dataValue = readDouble(input); } break;
      case ZB_TYPE_STRING_OCTET: { dataValue = readByteString(input,1); } break;
      case ZB_TYPE_STRING_CHAR: { dataValue = readString(input,1); } break;
      case ZB_TYPE_LONG_STRING_OCTET: { dataValue = readByteString(input,2); } break;
      case ZB_TYPE_LONG_STRING_CHAR: { dataValue = readString(input,2); } break;
      case ZB_TYPE_ARRAY: { dataValue = readArray(input); } break;
      case ZB_TYPE_STRUCT: { dataValue = readStruct(input); } break;
      case ZB_TYPE_SET: { dataValue = readSet(input); } break;
      case ZB_TYPE_BAG: { dataValue = readBag(input); } break;
      case ZB_TYPE_TIME_OF_DAY: { dataValue = readTimeOfDay(input); } break;
      case ZB_TYPE_DATE: { dataValue = readDate(input); } break;
      case ZB_TYPE_UTCTIME: { dataValue = readUtcTime(input); } break;
      case ZB_TYPE_CLUSTER_ID: { dataValue = readBit16(input,true,false); } break;
      case ZB_TYPE_ATTR_ID: { dataValue = readBit16(input,true,false); } break;
      case ZB_TYPE_BACNET_OID: { dataValue = readBit32(input,true,false); } break;
      case ZB_TYPE_IEEE: { dataValue = readFixedByteString(input, 8, true); } break;
      case ZB_TYPE_KEY_128BIT: { dataValue = readFixedByteString(input, 16, false); } break;
      default: throw new IOException("unknown zigbee data type: " + dataType);
      }

      return dataValue;
   }

   public static void encode(ByteBuf output, byte dataType, Object dataValue) throws IOException {
      switch (dataType) {
      case ZB_TYPE_NO_DATA: { } break;
      case ZB_TYPE_8BIT: { writeBit8(output, dataValue, false); } break;
      case ZB_TYPE_16BIT: { writeBit16(output, dataValue, false); } break;
      case ZB_TYPE_24BIT: { writeBit24(output, dataValue, false); } break;
      case ZB_TYPE_32BIT: { writeBit32(output, dataValue, false); } break;
      case ZB_TYPE_40BIT: { writeBit40(output, dataValue, false); } break;
      case ZB_TYPE_48BIT: { writeBit48(output, dataValue, false); } break;
      case ZB_TYPE_56BIT: { writeBit56(output, dataValue, false); } break;
      case ZB_TYPE_64BIT: { writeBit64(output, dataValue, false); } break;
      case ZB_TYPE_BOOLEAN: { writeBoolean(output, dataValue); } break;
      case ZB_TYPE_BITMAP_8BIT: { writeBit8(output, dataValue, false); } break;
      case ZB_TYPE_BITMAP_16BIT: { writeBit16(output, dataValue, false); } break;
      case ZB_TYPE_BITMAP_24BIT: { writeBit24(output, dataValue, false); } break;
      case ZB_TYPE_BITMAP_32BIT: { writeBit32(output, dataValue, false); } break;
      case ZB_TYPE_BITMAP_40BIT: { writeBit40(output, dataValue, false); } break;
      case ZB_TYPE_BITMAP_48BIT: { writeBit48(output, dataValue, false); } break;
      case ZB_TYPE_BITMAP_56BIT: { writeBit56(output, dataValue, false); } break;
      case ZB_TYPE_BITMAP_64BIT: { writeBit64(output, dataValue, false); } break;
      case ZB_TYPE_UNSIGNED_8BIT: { writeBit8(output, dataValue, false); } break;
      case ZB_TYPE_UNSIGNED_16BIT: { writeBit16(output, dataValue, false); } break;
      case ZB_TYPE_UNSIGNED_24BIT: { writeBit24(output, dataValue, false); } break;
      case ZB_TYPE_UNSIGNED_32BIT: { writeBit32(output, dataValue, false); } break;
      case ZB_TYPE_UNSIGNED_40BIT: { writeBit40(output, dataValue, false); } break;
      case ZB_TYPE_UNSIGNED_48BIT: { writeBit48(output, dataValue, false); } break;
      case ZB_TYPE_UNSIGNED_56BIT: { writeBit56(output, dataValue, false); } break;
      case ZB_TYPE_UNSIGNED_64BIT: { writeBit64(output, dataValue, false); } break;
      case ZB_TYPE_SIGNED_8BIT: { writeBit8(output, dataValue, true); } break;
      case ZB_TYPE_SIGNED_16BIT: { writeBit16(output, dataValue, true); } break;
      case ZB_TYPE_SIGNED_24BIT: { writeBit24(output, dataValue, true); } break;
      case ZB_TYPE_SIGNED_32BIT: { writeBit32(output, dataValue, true); } break;
      case ZB_TYPE_SIGNED_40BIT: { writeBit40(output, dataValue, true); } break;
      case ZB_TYPE_SIGNED_48BIT: { writeBit48(output, dataValue, true); } break;
      case ZB_TYPE_SIGNED_56BIT: { writeBit56(output, dataValue, true); } break;
      case ZB_TYPE_SIGNED_64BIT: { writeBit64(output, dataValue, true); } break;
      case ZB_TYPE_ENUM_8BIT: { writeBit8(output, dataValue, false); } break;
      case ZB_TYPE_ENUM_16BIT: { writeBit16(output, dataValue, false); } break;
      case ZB_TYPE_SEMIFLOAT: { writeSemiFloat(output, dataValue); } break;
      case ZB_TYPE_FLOAT: { writeFloat(output, dataValue); } break;
      case ZB_TYPE_DOUBLE: { writeDouble(output, dataValue); } break;
      case ZB_TYPE_STRING_OCTET: { writeByteString(output, dataValue, 1); } break;
      case ZB_TYPE_STRING_CHAR: { writeString(output, dataValue, 1); } break;
      case ZB_TYPE_LONG_STRING_OCTET: { writeByteString(output, dataValue, 2); } break;
      case ZB_TYPE_LONG_STRING_CHAR: { writeString(output, dataValue, 2); } break;
      case ZB_TYPE_ARRAY: { writeArray(output, dataValue); } break;
      case ZB_TYPE_STRUCT: { writeStruct(output, dataValue); } break;
      case ZB_TYPE_SET: { writeSet(output, dataValue); } break;
      case ZB_TYPE_BAG: { writeBag(output, dataValue); } break;
      case ZB_TYPE_TIME_OF_DAY: { writeTimeOfDay(output, dataValue); } break;
      case ZB_TYPE_DATE: { writeDate(output, dataValue); } break;
      case ZB_TYPE_UTCTIME: { writeUtcTime(output, dataValue); } break;
      case ZB_TYPE_CLUSTER_ID: { writeBit16(output, dataValue, false); } break;
      case ZB_TYPE_ATTR_ID: { writeBit16(output, dataValue, false); } break;
      case ZB_TYPE_BACNET_OID: { writeBit32(output, dataValue, false); } break;
      case ZB_TYPE_IEEE: { writeFixedByteString(output, dataValue, 8); } break;
      case ZB_TYPE_KEY_128BIT: { writeFixedByteString(output, dataValue, 16); } break;
      default: throw new IOException("unknown zigbee data type: " + dataType);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Encoding and decoding helpers
   /////////////////////////////////////////////////////////////////////////////

   private static Object readBoolean(DataInput input) throws IOException {
      return decodeBoolean(input.readByte());
   }

   private static Object readBoolean(ByteBuffer input) throws IOException {
      return decodeBoolean(input.get());
   }

   private static Object readBoolean(ByteBuf input) throws IOException {
      return decodeBoolean(input.readByte());
   }

   private static void writeBoolean(DataOutput output, Object value) throws IOException {
      if (value == NONE) { output.writeByte(0xFF); return; }
      output.writeByte(((Boolean)value).booleanValue() ? 0x01 : 0x00);
   }

   private static void writeBoolean(ByteBuffer output, Object value) throws IOException {
      if (value == NONE) { output.put((byte)0xFF); return; }
      output.put(((Boolean)value).booleanValue() ? (byte)0x01 : (byte)0x00);
   }

   private static void writeBoolean(ByteBuf output, Object value) throws IOException {
      if (value == NONE) { output.writeByte(0xFF); return; }
      output.writeByte(((Boolean)value).booleanValue() ? 0x01 : 0x00);
   }

   private static Object readBit8(DataInput input, boolean decode, boolean signed) throws IOException {
      if (decode) return decode(input.readByte(), signed);
      return input.readByte();
   }

   private static Object readBit8(ByteBuffer input, boolean decode, boolean signed) throws IOException {
      if (decode) return decode(input.get(), signed);
      return input.get();
   }

   private static Object readBit8(ByteBuf input, boolean decode, boolean signed) throws IOException {
      if (decode) return decode(input.readByte(), signed);
      return input.readByte();
   }

   private static void writeBit8(DataOutput output, Object value, boolean signed) throws IOException {
      if (value == NONE && !signed) { output.writeByte(0xFF); return; }
      if (value == NONE && signed) { output.writeByte(0x80); return; }
      output.writeByte(((Byte)value).byteValue());
   }

   private static void writeBit8(ByteBuffer output, Object value, boolean signed) throws IOException {
      if (value == NONE && !signed) { output.put((byte)0xFF); return; }
      if (value == NONE && signed) { output.put((byte)0x80); return; }
      output.put(((Byte)value).byteValue());
   }

   private static void writeBit8(ByteBuf output, Object value, boolean signed) throws IOException {
      if (value == NONE && !signed) { output.writeByte(0xFF); return; }
      if (value == NONE && signed) { output.writeByte(0x80); return; }
      output.writeByte(((Byte)value).byteValue());
   }

   private static Object readBit16(DataInput input, boolean decode, boolean signed) throws IOException {
      if (decode) return decode(input.readShort(), signed);
      return input.readShort();
   }

   private static Object readBit16(ByteBuffer input, boolean decode, boolean signed) throws IOException {
      if (decode) return decode(input.getShort(), signed);
      return input.getShort();
   }

   private static Object readBit16(ByteBuf input, boolean decode, boolean signed) throws IOException {
      if (decode) return decode(input.readShort(), signed);
      return input.readShort();
   }

   private static void writeBit16(DataOutput output, Object value, boolean signed) throws IOException {
      if (value == NONE && !signed) { output.writeShort(0xFFFF); return; }
      if (value == NONE && signed) { output.writeShort(0x8000); return; }
      output.writeShort(((Short)value).shortValue());
   }

   private static void writeBit16(ByteBuffer output, Object value, boolean signed) throws IOException {
      if (value == NONE && !signed) { output.putShort((short)0xFFFF); return; }
      if (value == NONE && signed) { output.putShort((short)0x8000); return; }
      output.putShort(((Short)value).shortValue());
   }

   private static void writeBit16(ByteBuf output, Object value, boolean signed) throws IOException {
      if (value == NONE && !signed) { output.writeShort(0xFFFF); return; }
      if (value == NONE && signed) { output.writeShort(0x8000); return; }
      output.writeShort(((Short)value).shortValue());
   }

   private static Object readBit24(DataInput input, boolean decode, boolean signed) throws IOException {
      long value = readBits(input, 3);
      if (decode) return decode24((int)value, signed);
      return (int)value;
   }

   private static Object readBit24(ByteBuffer input, boolean decode, boolean signed) throws IOException {
      long value = readBits(input, 3);
      if (decode) return decode24((int)value, signed);
      return (int)value;
   }

   private static Object readBit24(ByteBuf input, boolean decode, boolean signed) throws IOException {
      long value = readBits(input, 3);
      if (decode) return decode24((int)value, signed);
      return (int)value;
   }

   private static void writeBit24(DataOutput output, Object value, boolean signed) throws IOException {
      if (value == NONE && !signed) { writeBits(output, 0xFFFFFFL, 3); return; }
      if (value == NONE && signed) { writeBits(output, 0x800000L, 3); return; }
      writeBits(output, ((Integer)value).longValue(), 3);
   }

   private static void writeBit24(ByteBuffer output, Object value, boolean signed) throws IOException {
      if (value == NONE && !signed) { writeBits(output, 0xFFFFFFL, 3); return; }
      if (value == NONE && signed) { writeBits(output, 0x800000L, 3); return; }
      writeBits(output, ((Integer)value).longValue(), 3);
   }

   private static void writeBit24(ByteBuf output, Object value, boolean signed) throws IOException {
      if (value == NONE && !signed) { writeBits(output, 0xFFFFFFL, 3); return; }
      if (value == NONE && signed) { writeBits(output, 0x800000L, 3); return; }
      writeBits(output, ((Integer)value).longValue(), 3);
   }

   private static Object readBit32(DataInput input, boolean decode, boolean signed) throws IOException {
      if (decode) return decode(input.readInt(), signed);
      return input.readInt();
   }

   private static Object readBit32(ByteBuffer input, boolean decode, boolean signed) throws IOException {
      if (decode) return decode(input.getInt(), signed);
      return input.getInt();
   }

   private static Object readBit32(ByteBuf input, boolean decode, boolean signed) throws IOException {
      if (decode) return decode(input.readInt(), signed);
      return input.readInt();
   }

   private static void writeBit32(DataOutput output, Object value, boolean signed) throws IOException {
      if (value == NONE && !signed) { output.writeInt(0xFFFFFFFF); return; }
      if (value == NONE && signed) { output.writeInt(0x80000000); return; }
      output.writeInt(((Integer)value).intValue());
   }

   private static void writeBit32(ByteBuffer output, Object value, boolean signed) throws IOException {
      if (value == NONE && !signed) { output.putInt(0xFFFFFFFF); return; }
      if (value == NONE && signed) { output.putInt(0x80000000); return; }
      output.putInt(((Integer)value).intValue());
   }

   private static void writeBit32(ByteBuf output, Object value, boolean signed) throws IOException {
      if (value == NONE && !signed) { output.writeInt(0xFFFFFFFF); return; }
      if (value == NONE && signed) { output.writeInt(0x80000000); return; }
      output.writeInt(((Integer)value).intValue());
   }

   private static Object readBit40(DataInput input, boolean decode, boolean signed) throws IOException {
      long value = readBits(input, 5);
      if (decode) return decode40(value, signed);
      return value;
   }

   private static Object readBit40(ByteBuffer input, boolean decode, boolean signed) throws IOException {
      long value = readBits(input, 5);
      if (decode) return decode40(value, signed);
      return value;
   }

   private static Object readBit40(ByteBuf input, boolean decode, boolean signed) throws IOException {
      long value = readBits(input, 5);
      if (decode) return decode40(value, signed);
      return value;
   }

   private static void writeBit40(DataOutput output, Object value, boolean signed) throws IOException {
      if (value == NONE && !signed) { writeBits(output, 0xFFFFFFFFFFL, 5); return; }
      if (value == NONE && signed) { writeBits(output, 0x8000000000L, 5); return; }
      writeBits(output, ((Long)value).longValue(), 5);
   }

   private static void writeBit40(ByteBuffer output, Object value, boolean signed) throws IOException {
      if (value == NONE && !signed) { writeBits(output, 0xFFFFFFFFFFL, 5); return; }
      if (value == NONE && signed) { writeBits(output, 0x8000000000L, 5); return; }
      writeBits(output, ((Long)value).longValue(), 5);
   }

   private static void writeBit40(ByteBuf output, Object value, boolean signed) throws IOException {
      if (value == NONE && !signed) { writeBits(output, 0xFFFFFFFFFFL, 5); return; }
      if (value == NONE && signed) { writeBits(output, 0x8000000000L, 5); return; }
      writeBits(output, ((Long)value).longValue(), 5);
   }

   private static Object readBit48(DataInput input, boolean decode, boolean signed) throws IOException {
      long value = readBits(input, 6);
      if (decode) return decode48(value, signed);
      return value;
   }

   private static Object readBit48(ByteBuffer input, boolean decode, boolean signed) throws IOException {
      long value = readBits(input, 6);
      if (decode) return decode48(value, signed);
      return value;
   }

   private static Object readBit48(ByteBuf input, boolean decode, boolean signed) throws IOException {
      long value = readBits(input, 6);
      if (decode) return decode48(value, signed);
      return value;
   }

   private static void writeBit48(DataOutput output, Object value, boolean signed) throws IOException {
      if (value == NONE && !signed) { writeBits(output, 0xFFFFFFFFFFFFL, 6); return; }
      if (value == NONE && signed) { writeBits(output, 0x800000000000L, 6); return; }
      writeBits(output, ((Long)value).longValue(), 6);
   }

   private static void writeBit48(ByteBuffer output, Object value, boolean signed) throws IOException {
      if (value == NONE && !signed) { writeBits(output, 0xFFFFFFFFFFFFL, 6); return; }
      if (value == NONE && signed) { writeBits(output, 0x800000000000L, 6); return; }
      writeBits(output, ((Long)value).longValue(), 6);
   }

   private static void writeBit48(ByteBuf output, Object value, boolean signed) throws IOException {
      if (value == NONE && !signed) { writeBits(output, 0xFFFFFFFFFFFFL, 6); return; }
      if (value == NONE && signed) { writeBits(output, 0x800000000000L, 6); return; }
      writeBits(output, ((Long)value).longValue(), 6);
   }

   private static Object readBit56(DataInput input, boolean decode, boolean signed) throws IOException {
      long value = readBits(input, 7);
      if (decode) return decode56(value, signed);
      return value;
   }

   private static Object readBit56(ByteBuffer input, boolean decode, boolean signed) throws IOException {
      long value = readBits(input, 7);
      if (decode) return decode56(value, signed);
      return value;
   }

   private static Object readBit56(ByteBuf input, boolean decode, boolean signed) throws IOException {
      long value = readBits(input, 7);
      if (decode) return decode56(value, signed);
      return value;
   }

   private static void writeBit56(DataOutput output, Object value, boolean signed) throws IOException {
      if (value == NONE && !signed) { writeBits(output, 0xFFFFFFFFFFFFFFL, 7); return; }
      if (value == NONE && signed) { writeBits(output, 0x80000000000000L, 7); return; }
      writeBits(output, ((Long)value).longValue(), 7);
   }

   private static void writeBit56(ByteBuffer output, Object value, boolean signed) throws IOException {
      if (value == NONE && !signed) { writeBits(output, 0xFFFFFFFFFFFFFFL, 7); return; }
      if (value == NONE && signed) { writeBits(output, 0x80000000000000L, 7); return; }
      writeBits(output, ((Long)value).longValue(), 7);
   }

   private static void writeBit56(ByteBuf output, Object value, boolean signed) throws IOException {
      if (value == NONE && !signed) { writeBits(output, 0xFFFFFFFFFFFFFFL, 7); return; }
      if (value == NONE && signed) { writeBits(output, 0x80000000000000L, 7); return; }
      writeBits(output, ((Long)value).longValue(), 7);
   }

   private static Object readBit64(DataInput input, boolean decode, boolean signed) throws IOException {
      if (decode) return decode(input.readLong(), signed);
      return input.readLong();
   }

   private static Object readBit64(ByteBuffer input, boolean decode, boolean signed) throws IOException {
      if (decode) return decode(input.getLong(), signed);
      return input.getLong();
   }

   private static Object readBit64(ByteBuf input, boolean decode, boolean signed) throws IOException {
      if (decode) return decode(input.readLong(), signed);
      return input.readLong();
   }

   private static void writeBit64(DataOutput output, Object value, boolean signed) throws IOException {
      if (value == NONE && !signed) { output.writeLong(0xFFFFFFFFFFFFFFFFL); return; }
      if (value == NONE && signed) { output.writeLong(0x8000000000000000L); return; }
      output.writeLong(((Long)value).longValue());
   }

   private static void writeBit64(ByteBuffer output, Object value, boolean signed) throws IOException {
      if (value == NONE && !signed) { output.putLong(0xFFFFFFFFFFFFFFFFL); return; }
      if (value == NONE && signed) { output.putLong(0x8000000000000000L); return; }
      output.putLong(((Long)value).longValue());
   }

   private static void writeBit64(ByteBuf output, Object value, boolean signed) throws IOException {
      if (value == NONE && !signed) { output.writeLong(0xFFFFFFFFFFFFFFFFL); return; }
      if (value == NONE && signed) { output.writeLong(0x8000000000000000L); return; }
      output.writeLong(((Long)value).longValue());
   }

   private static long readBits(DataInput input, int bytes) throws IOException {
      long result = 0;
      long shift = 0;
      for (int i = 0; i < bytes; ++i) {
         result = result | ((input.readByte() & 0xFFL) << shift);
         shift += 8;
      }

      return result;
   }

   private static long readBits(ByteBuffer input, int bytes) throws IOException {
      long result = 0;
      if (input.order() == ByteOrder.LITTLE_ENDIAN) {
         long shift = 0;
         for (int i = 0; i < bytes; ++i) {
            result = result | ((input.get() & 0xFFL) << shift);
            shift += 8;
         }
      } else {
         for (int i = 0; i < bytes; ++i) {
            result = (result << 8) | (input.get() & 0xFFL);
         }
      }

      return result;
   }

   private static long readBits(ByteBuf input, int bytes) throws IOException {
      long result = 0;
      if (input.order() == ByteOrder.LITTLE_ENDIAN) {
         long shift = 0;
         for (int i = 0; i < bytes; ++i) {
            result = result | ((input.readByte() & 0xFFL) << shift);
            shift += 8;
         }
      } else {
         for (int i = 0; i < bytes; ++i) {
            result = (result << 8) | (input.readByte() & 0xFFL);
         }
      }

      return result;
   }

   private static void writeBits(DataOutput output, long value, int bytes) throws IOException {
      long result = value;
      for (int i = 0; i < bytes; ++i) {
         output.writeByte((byte)result);
         result = (result >> 8);
      }
   }

   private static void writeBits(ByteBuffer output, long value, int bytes) throws IOException {
      long result = value;
      long shift = 8 * (bytes - 1);
      if (output.order() == ByteOrder.LITTLE_ENDIAN) {
         for (int i = 0; i < bytes; ++i) {
            output.put((byte)result);
            result = (result >> 8);
         }
      } else {
         for (int i = 0; i < bytes; ++i) {
            output.put((byte)((result >> shift) & 0xFFL));
            result = result << 8;
         }
      }
   }

   private static void writeBits(ByteBuf output, long value, int bytes) throws IOException {
      long result = value;
      long shift = 8 * (bytes - 1);
      if (output.order() == ByteOrder.LITTLE_ENDIAN) {
         for (int i = 0; i < bytes; ++i) {
            output.writeByte((byte)result);
            result = (result >> 8);
         }
      } else {
         for (int i = 0; i < bytes; ++i) {
            output.writeByte((byte)((result >> shift) & 0xFFL));
            result = result << 8;
         }
      }
   }

   private static Object readSemiFloat(DataInput input) throws IOException {
      log.warn("cannot currently decode zigbee semi-float data types, skipping");
      input.readShort();
      return NONE;
   }

   private static Object readSemiFloat(ByteBuffer input) throws IOException {
      log.warn("cannot currently decode zigbee semi-float data types, skipping");
      input.getShort();
      return NONE;
   }

   private static Object readSemiFloat(ByteBuf input) throws IOException {
      log.warn("cannot currently decode zigbee semi-float data types, skipping");
      input.readShort();
      return NONE;
   }

   private static void writeSemiFloat(DataOutput output, Object value) throws IOException {
      if (value == NONE) { output.writeShort(0xFFFF); return; }
      log.warn("cannot currently encode zigbee semi-float data types, sending invalid value");
      output.writeShort(0xFFFF);
   }

   private static void writeSemiFloat(ByteBuffer output, Object value) throws IOException {
      if (value == NONE) { output.putShort((short)0xFFFF); return; }
      log.warn("cannot currently encode zigbee semi-float data types, sending invalid value");
      output.putShort((short)0xFFFF);
   }

   private static void writeSemiFloat(ByteBuf output, Object value) throws IOException {
      if (value == NONE) { output.writeShort(0xFFFF); return; }
      log.warn("cannot currently encode zigbee semi-float data types, sending invalid value");
      output.writeShort(0xFFFF);
   }

   private static Object readFloat(DataInput input) throws IOException {
      return decodeFloat(input.readFloat());
   }

   private static Object readFloat(ByteBuffer input) throws IOException {
      return decodeFloat(input.getFloat());
   }

   private static Object readFloat(ByteBuf input) throws IOException {
      return decodeFloat(input.readFloat());
   }

   private static void writeFloat(DataOutput output, Object value) throws IOException {
      if (value == NONE) { output.writeInt(0xFFFFFFFF); return; }
      output.writeFloat(((Float)value).floatValue());
   }

   private static void writeFloat(ByteBuffer output, Object value) throws IOException {
      if (value == NONE) { output.putInt(0xFFFFFFFF); return; }
      output.putFloat(((Float)value).floatValue());
   }

   private static void writeFloat(ByteBuf output, Object value) throws IOException {
      if (value == NONE) { output.writeInt(0xFFFFFFFF); return; }
      output.writeFloat(((Float)value).floatValue());
   }

   private static Object readDouble(DataInput input) throws IOException {
      return decodeDouble(input.readDouble());
   }

   private static Object readDouble(ByteBuffer input) throws IOException {
      return decodeDouble(input.getDouble());
   }

   private static Object readDouble(ByteBuf input) throws IOException {
      return decodeDouble(input.readDouble());
   }

   private static void writeDouble(DataOutput output, Object value) throws IOException {
      if (value == NONE) { output.writeLong(0xFFFFFFFFFFFFFFFFL); return; }
      output.writeDouble(((Double)value).doubleValue());
   }

   private static void writeDouble(ByteBuffer output, Object value) throws IOException {
      if (value == NONE) { output.putLong(0xFFFFFFFFFFFFFFFFL); return; }
      output.putDouble(((Double)value).doubleValue());
   }

   private static void writeDouble(ByteBuf output, Object value) throws IOException {
      if (value == NONE) { output.writeLong(0xFFFFFFFFFFFFFFFFL); return; }
      output.writeDouble(((Double)value).doubleValue());
   }

   private static Object readByteString(DataInput input, int lengthBytes) throws IOException {
      int length;
      if (lengthBytes == 1) {
         Object len = readBit8(input,true,false);
         length = (len == NONE) ? -1 : ((Byte)len).byteValue() & 0xFF;
      } else {
         Object len = readBit16(input,true,false);
         length = (len == NONE) ? -1 : ((Short)len).shortValue() & 0xFFFF;
      }

      if (length < 0) return NONE;
      byte[] result = new byte[length];
      input.readFully(result);
      return result;
   }

   private static Object readByteString(ByteBuffer input, int lengthBytes) throws IOException {
      int length;
      if (lengthBytes == 1) {
         Object len = readBit8(input,true,false);
         length = (len == NONE) ? -1 : ((Byte)len).byteValue() & 0xFF;
      } else {
         Object len = readBit16(input,true,false);
         length = (len == NONE) ? -1 : ((Short)len).shortValue() & 0xFFFF;
      }

      if (length < 0) return NONE;
      byte[] result = new byte[length];
      input.get(result);
      return result;
   }

   private static Object readByteString(ByteBuf input, int lengthBytes) throws IOException {
      int length;
      if (lengthBytes == 1) {
         Object len = readBit8(input,true,false);
         length = (len == NONE) ? -1 : ((Byte)len).byteValue() & 0xFF;
      } else {
         Object len = readBit16(input,true,false);
         length = (len == NONE) ? -1 : ((Short)len).shortValue() & 0xFFFF;
      }

      if (length < 0) return NONE;
      byte[] result = new byte[length];
      input.readBytes(result);
      return result;
   }

   private static void writeByteString(DataOutput output, Object value, int lengthBytes) throws IOException {
      if (value == NONE && lengthBytes == 1) { output.writeByte(0xFF); return; }
      if (value == NONE && lengthBytes == 2) { output.writeShort(0xFFFF); return; }

      byte[] array = (byte[])value;
      int length = array.length;
      if (lengthBytes == 2 && length > 0xFE) {
         log.warn("zigbee byte string too long, truncating");
         length = 0xFE;
      }

      if (lengthBytes == 1 && length > 0xFFFE) {
         log.warn("zigbee byte string too long, truncating");
         length = 0xFFFE;
      }

      if (lengthBytes == 1) {
         output.writeByte(length);
      } else {
         output.writeShort(length);
      }

      output.write(array, 0, length);
   }

   private static void writeByteString(ByteBuffer output, Object value, int lengthBytes) throws IOException {
      if (value == NONE && lengthBytes == 1) { output.put((byte)0xFF); return; }
      if (value == NONE && lengthBytes == 2) { output.putShort((short)0xFFFF); return; }

      byte[] array = (byte[])value;
      int length = array.length;
      if (lengthBytes == 2 && length > 0xFE) {
         log.warn("zigbee byte string too long, truncating");
         length = 0xFE;
      }

      if (lengthBytes == 1 && length > 0xFFFE) {
         log.warn("zigbee byte string too long, truncating");
         length = 0xFFFE;
      }

      if (lengthBytes == 1) {
         output.put((byte)length);
      } else {
         output.putShort((short)length);
      }

      output.put(array, 0, length);
   }

   private static void writeByteString(ByteBuf output, Object value, int lengthBytes) throws IOException {
      if (value == NONE && lengthBytes == 1) { output.writeByte(0xFF); return; }
      if (value == NONE && lengthBytes == 2) { output.writeShort(0xFFFF); return; }

      byte[] array = (byte[])value;
      int length = array.length;
      if (lengthBytes == 2 && length > 0xFE) {
         log.warn("zigbee byte string too long, truncating");
         length = 0xFE;
      }

      if (lengthBytes == 1 && length > 0xFFFE) {
         log.warn("zigbee byte string too long, truncating");
         length = 0xFFFE;
      }

      if (lengthBytes == 1) {
         output.writeByte(length);
      } else {
         output.writeShort(length);
      }

      output.writeBytes(array, 0, length);
   }

   private static Object readString(DataInput input, int lengthBytes) throws IOException {
      Object bytes = readByteString(input, lengthBytes);
      if (bytes == NONE) return NONE;

      // TODO: charset encoding?
      return new String((byte[])bytes, StandardCharsets.UTF_8);
   }

   private static Object readString(ByteBuffer input, int lengthBytes) throws IOException {
      Object bytes = readByteString(input, lengthBytes);
      if (bytes == NONE) return NONE;

      // TODO: charset encoding?
      return new String((byte[])bytes, StandardCharsets.UTF_8);
   }

   private static Object readString(ByteBuf input, int lengthBytes) throws IOException {
      Object bytes = readByteString(input, lengthBytes);
      if (bytes == NONE) return NONE;

      // TODO: charset encoding?
      return new String((byte[])bytes, StandardCharsets.UTF_8);
   }

   private static void writeString(DataOutput output, Object value, int lengthBytes) throws IOException {
      if (value == NONE) { writeByteString(output, value, lengthBytes); return; }

      // TODO: charset encoding?
      String str = (String)value;
      byte[] data = str.getBytes(StandardCharsets.UTF_8);
      writeByteString(output, data, lengthBytes);
   }

   private static void writeString(ByteBuffer output, Object value, int lengthBytes) throws IOException {
      if (value == NONE) { writeByteString(output, value, lengthBytes); return; }

      // TODO: charset encoding?
      String str = (String)value;
      byte[] data = str.getBytes(StandardCharsets.UTF_8);
      writeByteString(output, data, lengthBytes);
   }

   private static void writeString(ByteBuf output, Object value, int lengthBytes) throws IOException {
      if (value == NONE) { writeByteString(output, value, lengthBytes); return; }

      // TODO: charset encoding?
      String str = (String)value;
      byte[] data = str.getBytes(StandardCharsets.UTF_8);
      writeByteString(output, data, lengthBytes);
   }

   private static Object readFixedByteString(DataInput input, int length, boolean decode) throws IOException {
      byte[] result = new byte[length];
      input.readFully(result);
      return decode ? decodeFixed(result) : result;
   }

   private static Object readFixedByteString(ByteBuffer input, int length, boolean decode) throws IOException {
      byte[] result = new byte[length];
      input.get(result);
      return decode ? decodeFixed(result) : result;
   }

   private static Object readFixedByteString(ByteBuf input, int length, boolean decode) throws IOException {
      byte[] result = new byte[length];
      input.readBytes(result);
      return decode ? decodeFixed(result) : result;
   }

   private static Object decodeFixed(byte[] result) {
      boolean bad = true;
      for (int i = 0; i < result.length; ++i) {
         if ((result[i] & 0xFF) != 0xFF) {
            bad = false;
            break;
         }
      }

      return bad ? NONE : result;
   }

   private static void writeFixedByteString(DataOutput output, Object value, int length) throws IOException {
      if (value == NONE) {
         for (int i = 0; i < length; ++i)
            output.writeByte((byte)0xFF);
         return;
      }

      byte[] data = (byte[])value;
      output.write(data);
   }

   private static void writeFixedByteString(ByteBuffer output, Object value, int length) throws IOException {
      if (value == NONE) {
         for (int i = 0; i < length; ++i)
            output.put((byte)0xFF);
         return;
      }

      byte[] data = (byte[])value;
      output.put(data);
   }

   private static void writeFixedByteString(ByteBuf output, Object value, int length) throws IOException {
      if (value == NONE) {
         for (int i = 0; i < length; ++i)
            output.writeByte((byte)0xFF);
         return;
      }

      byte[] data = (byte[])value;
      output.writeBytes(data);
   }

   private static Object readArray(DataInput input) throws IOException {
      log.warn("cannot currently decode zigbee array data types, skipping");

      Object len = readBit16(input, true, false);
      if (len == NONE) return NONE;

      int length = ((Short)len).shortValue() & 0xFFFF;
      input.skipBytes(length);

      return NONE;
   }

   private static Object readArray(ByteBuffer input) throws IOException {
      log.warn("cannot currently decode zigbee array data types, skipping");

      Object len = readBit16(input, true, false);
      if (len == NONE) return NONE;

      int length = ((Short)len).shortValue() & 0xFFFF;
      input.position(input.position() + length);

      return NONE;
   }

   private static Object readArray(ByteBuf input) throws IOException {
      log.warn("cannot currently decode zigbee array data types, skipping");

      Object len = readBit16(input, true, false);
      if (len == NONE) return NONE;

      int length = ((Short)len).shortValue() & 0xFFFF;
      input.skipBytes(length);

      return NONE;
   }

   private static void writeArray(DataOutput output, Object value) throws IOException {
      log.warn("cannot currently encode zigbee array data types, skipping");
      writeBit16(output, 0xFFFF, false);
   }

   private static void writeArray(ByteBuffer output, Object value) throws IOException {
      log.warn("cannot currently encode zigbee array data types, skipping");
      writeBit16(output, 0xFFFF, false);
   }

   private static void writeArray(ByteBuf output, Object value) throws IOException {
      log.warn("cannot currently encode zigbee array data types, skipping");
      writeBit16(output, 0xFFFF, false);
   }

   private static Object readStruct(DataInput input) throws IOException {
      log.warn("cannot currently decode zigbee struct data types, skipping");

      Object len = readBit16(input, true, false);
      if (len == NONE) return NONE;

      int length = ((Short)len).shortValue() & 0xFFFF;
      input.skipBytes(length);

      return NONE;
   }

   private static Object readStruct(ByteBuffer input) throws IOException {
      log.warn("cannot currently decode zigbee struct data types, skipping");

      Object len = readBit16(input, true, false);
      if (len == NONE) return NONE;

      int length = ((Short)len).shortValue() & 0xFFFF;
      input.position(input.position() + length);

      return NONE;
   }

   private static Object readStruct(ByteBuf input) throws IOException {
      log.warn("cannot currently decode zigbee struct data types, skipping");

      Object len = readBit16(input, true, false);
      if (len == NONE) return NONE;

      int length = ((Short)len).shortValue() & 0xFFFF;
      input.skipBytes(length);

      return NONE;
   }

   private static void writeStruct(DataOutput output, Object value) throws IOException {
      log.warn("cannot currently encode zigbee struct data types, skipping");
      writeBit16(output, 0xFFFF, false);
   }

   private static void writeStruct(ByteBuffer output, Object value) throws IOException {
      log.warn("cannot currently encode zigbee struct data types, skipping");
      writeBit16(output, 0xFFFF, false);
   }

   private static void writeStruct(ByteBuf output, Object value) throws IOException {
      log.warn("cannot currently encode zigbee struct data types, skipping");
      writeBit16(output, 0xFFFF, false);
   }

   private static Object readSet(DataInput input) throws IOException {
      log.warn("cannot currently decode zigbee set data types, skipping");

      Object len = readBit16(input, true, false);
      if (len == NONE) return NONE;

      int length = ((Short)len).shortValue() & 0xFFFF;
      input.skipBytes(length);

      return NONE;
   }

   private static Object readSet(ByteBuffer input) throws IOException {
      log.warn("cannot currently decode zigbee set data types, skipping");

      Object len = readBit16(input, true, false);
      if (len == NONE) return NONE;

      int length = ((Short)len).shortValue() & 0xFFFF;
      input.position(input.position() + length);

      return NONE;
   }

   private static Object readSet(ByteBuf input) throws IOException {
      log.warn("cannot currently decode zigbee set data types, skipping");

      Object len = readBit16(input, true, false);
      if (len == NONE) return NONE;

      int length = ((Short)len).shortValue() & 0xFFFF;
      input.skipBytes(length);

      return NONE;
   }

   private static void writeSet(DataOutput output, Object value) throws IOException {
      log.warn("cannot currently encode zigbee set data types, skipping");
      writeBit16(output, 0xFFFF, false);
   }

   private static void writeSet(ByteBuffer output, Object value) throws IOException {
      log.warn("cannot currently encode zigbee set data types, skipping");
      writeBit16(output, 0xFFFF, false);
   }

   private static void writeSet(ByteBuf output, Object value) throws IOException {
      log.warn("cannot currently encode zigbee set data types, skipping");
      writeBit16(output, 0xFFFF, false);
   }

   private static Object readBag(DataInput input) throws IOException {
      log.warn("cannot currently decode zigbee bag data types, skipping");

      Object len = readBit16(input, true, false);
      if (len == NONE) return NONE;

      int length = ((Short)len).shortValue() & 0xFFFF;
      input.skipBytes(length);

      return NONE;
   }

   private static Object readBag(ByteBuffer input) throws IOException {
      log.warn("cannot currently decode zigbee bag data types, skipping");

      Object len = readBit16(input, true, false);
      if (len == NONE) return NONE;

      int length = ((Short)len).shortValue() & 0xFFFF;
      input.position(input.position() + length);

      return NONE;
   }

   private static Object readBag(ByteBuf input) throws IOException {
      log.warn("cannot currently decode zigbee bag data types, skipping");

      Object len = readBit16(input, true, false);
      if (len == NONE) return NONE;

      int length = ((Short)len).shortValue() & 0xFFFF;
      input.skipBytes(length);

      return NONE;
   }

   private static void writeBag(DataOutput output, Object value) throws IOException {
      log.warn("cannot currently encode zigbee bag data types, skipping");
      writeBit16(output, 0xFFFF, false);
   }

   private static void writeBag(ByteBuffer output, Object value) throws IOException {
      log.warn("cannot currently encode zigbee bag data types, skipping");
      writeBit16(output, 0xFFFF, false);
   }

   private static void writeBag(ByteBuf output, Object value) throws IOException {
      log.warn("cannot currently encode zigbee bag data types, skipping");
      writeBit16(output, 0xFFFF, false);
   }

   private static Object readTimeOfDay(DataInput input) throws IOException {
      return toTimeOfDay(readBit32(input, true, false));
   }

   private static Object readTimeOfDay(ByteBuffer input) throws IOException {
      return toTimeOfDay(readBit32(input, true, false));
   }

   private static Object readTimeOfDay(ByteBuf input) throws IOException {
      return toTimeOfDay(readBit32(input, true, false));
   }

   private static Object toTimeOfDay(Object value) throws IOException {
      if (value == NONE) return NONE;

      int time = ((Integer)value).intValue();
      int hours = (time >> 24) & 0xFF;
      int minutes = (time >> 16) & 0xFF;
      int seconds = (time >> 8) & 0xFF;
      int hundredths = time & 0xFF;

      if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59 ||
          seconds < 0 || seconds > 59 || hundredths < 0 || hundredths > 100) {
         log.warn("invalid zigbee time of day {}, discarding", time);
         return NONE;
      }

      Calendar calendar = (Calendar)ZIGBEE_EPOCH.clone();
      calendar.setTimeInMillis(System.currentTimeMillis());
      calendar.set(Calendar.MILLISECOND, hundredths * 10);
      calendar.set(Calendar.SECOND, seconds);
      calendar.set(Calendar.MINUTE, minutes);
      calendar.set(Calendar.HOUR_OF_DAY, hours);
      return calendar.getTime();
   }

   private static void writeTimeOfDay(DataOutput output, Object value) throws IOException {
      if (value == NONE) { output.writeInt(0xFFFFFFFF); return; }
      output.writeInt(fromTimeOfDay((Date)value));
   }

   private static void writeTimeOfDay(ByteBuffer output, Object value) throws IOException {
      if (value == NONE) { output.putInt(0xFFFFFFFF); return; }
      output.putInt(fromTimeOfDay((Date)value));
   }

   private static void writeTimeOfDay(ByteBuf output, Object value) throws IOException {
      if (value == NONE) { output.writeInt(0xFFFFFFFF); return; }
      output.writeInt(fromTimeOfDay((Date)value));
   }

   private static int fromTimeOfDay(Date date) throws IOException {
      Calendar calendar = (Calendar)ZIGBEE_EPOCH.clone();
      calendar.setTime(date);

      int hours = calendar.get(Calendar.HOUR_OF_DAY) & 0xFF;
      int minutes = calendar.get(Calendar.MINUTE) & 0xFF;
      int seconds = calendar.get(Calendar.SECOND) & 0xFF;
      int hundredths = (calendar.get(Calendar.MILLISECOND) / 10) & 0xFF;
      return (hours << 24) | (minutes << 16) | (seconds << 8) | hundredths;
   }

   private static Object readDate(DataInput input) throws IOException {
      return toDate(readBit32(input, true, false));
   }

   private static Object readDate(ByteBuffer input) throws IOException {
      return toDate(readBit32(input, true, false));
   }

   private static Object readDate(ByteBuf input) throws IOException {
      return toDate(readBit32(input, true, false));
   }

   private static Object toDate(Object value) throws IOException {
      if (value == NONE) return NONE;

      int time = ((Integer)value).intValue();

      Calendar calendar = (Calendar)ZIGBEE_EPOCH.clone();
      calendar.setTimeInMillis(System.currentTimeMillis());

      int year = (time >> 24) & 0xFF;
      if (year == 0xFF) {
         year = calendar.get(Calendar.YEAR);
      } else {
         year = year + 1900;
      }

      int month = (time >> 16) & 0xFF;
      if (month == 0xFF) {
         year = calendar.get(Calendar.MONTH);
      } else {
         month = month - 1;
      }

      int date = (time >> 8) & 0xFF;
      if (date == 0xFF) {
         date = calendar.get(Calendar.DATE);
      }

      if (month < 0 || month > 11 || date < 1 || date > 31) {
         log.warn("invalid zigbee date {}, discarding", time);
         return NONE;
      }


      calendar.set(Calendar.MILLISECOND, 0);
      calendar.set(year, month, date, 0, 0, 0);
      return calendar.getTime();
   }

   private static void writeDate(DataOutput output, Object value) throws IOException {
      if (value == NONE) { output.writeInt(0xFFFFFFFF); return; }
      output.writeInt(fromDate((Date)value));
   }

   private static void writeDate(ByteBuffer output, Object value) throws IOException {
      if (value == NONE) { output.putInt(0xFFFFFFFF); return; }
      output.putInt(fromDate((Date)value));
   }

   private static void writeDate(ByteBuf output, Object value) throws IOException {
      if (value == NONE) { output.writeInt(0xFFFFFFFF); return; }
      output.writeInt(fromDate((Date)value));
   }

   private static int fromDate(Date date) throws IOException {
      Calendar calendar = (Calendar)ZIGBEE_EPOCH.clone();
      calendar.setTime(date);

      int year = (calendar.get(Calendar.YEAR) - 1900) & 0xFF;
      int month = (calendar.get(Calendar.MONTH) + 1) & 0xFF;
      int day = calendar.get(Calendar.DATE) & 0xFF;
      int dow;
      switch (calendar.get(Calendar.DAY_OF_WEEK)) {
      case Calendar.MONDAY: dow = 1; break;
      case Calendar.TUESDAY: dow = 2; break;
      case Calendar.WEDNESDAY: dow = 3; break;
      case Calendar.THURSDAY: dow = 4; break;
      case Calendar.FRIDAY: dow = 5; break;
      case Calendar.SATURDAY: dow = 6; break;
      case Calendar.SUNDAY: dow = 7; break;
      default: throw new IllegalStateException("invalid day of week from zigbee date");
      }

      return (year << 24) | (month << 16) | (day << 8) | dow;
   }

   private static Object readUtcTime(DataInput input) throws IOException {
      return toUtcTime(readBit32(input, true, false));
   }

   private static Object readUtcTime(ByteBuffer input) throws IOException {
      return toUtcTime(readBit32(input, true, false));
   }

   private static Object readUtcTime(ByteBuf input) throws IOException {
      return toUtcTime(readBit32(input, true, false));
   }

   private static Object toUtcTime(Object value) throws IOException {
      if (value == NONE) return NONE;

      Calendar calendar = (Calendar)ZIGBEE_EPOCH.clone();
      long seconds = ((Integer)value).intValue() & 0xFFFFFFFFL;
      int days = (int)TimeUnit.DAYS.convert(seconds, TimeUnit.SECONDS);
      seconds = seconds % 86400;

      calendar.add(Calendar.DATE, days);
      calendar.add(Calendar.SECOND, (int)seconds);
      if (calendar.get(Calendar.MILLISECOND) > 0) {
         calendar.add(Calendar.SECOND, 1);
      }

      calendar.set(Calendar.MILLISECOND, 0);
      return calendar.getTime();
   }

   private static void writeUtcTime(DataOutput output, Object value) throws IOException {
      if (value == NONE) { output.writeInt(0xFFFFFFFF); return; }
      output.writeInt(fromUtcTime((Date)value));
   }

   private static void writeUtcTime(ByteBuffer output, Object value) throws IOException {
      if (value == NONE) { output.putInt(0xFFFFFFFF); return; }
      output.putInt(fromUtcTime((Date)value));
   }

   private static void writeUtcTime(ByteBuf output, Object value) throws IOException {
      if (value == NONE) { output.writeInt(0xFFFFFFFF); return; }
      output.writeInt(fromUtcTime((Date)value));
   }

   private static int fromUtcTime(Date date) throws IOException {
      long millisInJavaEpoch = date.getTime();
      long millis = millisInJavaEpoch - ZIGBEE_EPOCH.getTimeInMillis();
      return (int)TimeUnit.SECONDS.convert(millis, TimeUnit.MILLISECONDS);
   }

   static Object decodeBoolean(byte value) {
      if (value == 0) return Boolean.FALSE;
      if (value == 1) return Boolean.TRUE;
      return NONE;
   }

   static Object decode(byte value, boolean signed) {
      if (!signed && (value & 0xFF) == 0xFF) return NONE;
      if (signed && (value & 0xFF) == 0x80) return NONE;
      return value;
   }

   static Object decode(short value, boolean signed) {
      if (!signed && (value & 0xFFFF) == 0xFFFF) return NONE;
      if (signed && (value & 0xFFFF) == 0x8000) return NONE;
      return value;
   }

   static Object decode24(int value, boolean signed) {
      if (!signed && (value & 0xFFFFFF) == 0xFFFFFF) return NONE;
      if (signed && (value & 0xFFFFFF) == 0x800000) return NONE;
      return value;
   }

   static Object decode(int value, boolean signed) {
      if (!signed && (value == 0xFFFFFFFF)) return NONE;
      if (signed && (value == 0x80000000)) return NONE;
      return value;
   }

   static Object decode40(long value, boolean signed) {
      if (!signed && (value == 0xFFFFFFFFFFL)) return NONE;
      if (signed && (value == 0x8000000000L)) return NONE;
      return value;
   }

   static Object decode48(long value, boolean signed) {
      if (!signed && (value == 0xFFFFFFFFFFFFL)) return NONE;
      if (signed && (value == 0x800000000000L)) return NONE;
      return value;
   }

   static Object decode56(long value, boolean signed) {
      if (!signed && (value == 0xFFFFFFFFFFFFFFL)) return NONE;
      if (signed && (value == 0x80000000000000L)) return NONE;
      return value;
   }

   static Object decode(long value, boolean signed) {
      if (!signed && (value == 0xFFFFFFFFFFFFFFFFL)) return NONE;
      if (signed && (value == 0x8000000000000000L)) return NONE;
      return value;
   }

   static Object decodeFloat(float value) {
      if (Float.isNaN(value)) return NONE;
      return value;
   }

   static Object decodeDouble(double value) {
      if (Double.isNaN(value)) return NONE;
      return value;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Random instance support
   /////////////////////////////////////////////////////////////////////////////

   public static int getRandomInstanceType() {
      Random r = ThreadLocalRandom.current();

      // TODO: not generating semi-float, array, struct, set, or bag
      switch (r.nextInt(51)) {
      case 0: return ZB_TYPE_NO_DATA;
      case 1: return ZB_TYPE_8BIT;
      case 2: return ZB_TYPE_16BIT;
      case 3: return ZB_TYPE_24BIT;
      case 4: return ZB_TYPE_32BIT;
      case 5: return ZB_TYPE_40BIT;
      case 6: return ZB_TYPE_48BIT;
      case 7: return ZB_TYPE_56BIT;
      case 8: return ZB_TYPE_64BIT;
      case 9: return ZB_TYPE_BOOLEAN;
      case 10: return ZB_TYPE_BITMAP_8BIT;
      case 11: return ZB_TYPE_BITMAP_16BIT;
      case 12: return ZB_TYPE_BITMAP_24BIT;
      case 13: return ZB_TYPE_BITMAP_32BIT;
      case 14: return ZB_TYPE_BITMAP_40BIT;
      case 15: return ZB_TYPE_BITMAP_48BIT;
      case 16: return ZB_TYPE_BITMAP_56BIT;
      case 17: return ZB_TYPE_BITMAP_64BIT;
      case 18: return ZB_TYPE_UNSIGNED_8BIT;
      case 19: return ZB_TYPE_UNSIGNED_16BIT;
      case 20: return ZB_TYPE_UNSIGNED_24BIT;
      case 21: return ZB_TYPE_UNSIGNED_32BIT;
      case 22: return ZB_TYPE_UNSIGNED_40BIT;
      case 23: return ZB_TYPE_UNSIGNED_48BIT;
      case 24: return ZB_TYPE_UNSIGNED_56BIT;
      case 25: return ZB_TYPE_UNSIGNED_64BIT;
      case 26: return ZB_TYPE_SIGNED_8BIT;
      case 27: return ZB_TYPE_SIGNED_16BIT;
      case 28: return ZB_TYPE_SIGNED_24BIT;
      case 29: return ZB_TYPE_SIGNED_32BIT;
      case 30: return ZB_TYPE_SIGNED_40BIT;
      case 31: return ZB_TYPE_SIGNED_48BIT;
      case 32: return ZB_TYPE_SIGNED_56BIT;
      case 33: return ZB_TYPE_SIGNED_64BIT;
      case 34: return ZB_TYPE_ENUM_8BIT;
      case 35: return ZB_TYPE_ENUM_16BIT;
      //case 36: { dataType = ZB_TYPE_SEMIFLOAT;
      case 36: return ZB_TYPE_FLOAT;
      case 37: return ZB_TYPE_FLOAT;
      case 38: return ZB_TYPE_DOUBLE;
      case 39: return ZB_TYPE_STRING_OCTET;
      case 40: return ZB_TYPE_STRING_CHAR;
      case 41: return ZB_TYPE_LONG_STRING_OCTET;
      case 42: return ZB_TYPE_LONG_STRING_CHAR;
      case 43: return ZB_TYPE_TIME_OF_DAY;
      case 44: return ZB_TYPE_DATE;
      case 45: return ZB_TYPE_UTCTIME;
      case 46: return ZB_TYPE_CLUSTER_ID;
      case 47: return ZB_TYPE_ATTR_ID;
      case 48: return ZB_TYPE_BACNET_OID;
      case 49: return ZB_TYPE_IEEE;
      case 50: return ZB_TYPE_KEY_128BIT;
      case 51: return ZB_TYPE_ARRAY;
      case 52: return ZB_TYPE_STRUCT;
      case 53: return ZB_TYPE_SET;
      case 54: return ZB_TYPE_BAG;
      default: throw new RuntimeException("bad random value");
      }
   }

   public static byte[] getEncodedRandomInstanceValue(byte dataType) {
      try {
         Object dataValue = getRandomInstanceValue(dataType);
         ByteBuf buf = Unpooled.buffer(dataValueSize(dataType,dataValue));
         encode(buf, dataType, dataValue);

         byte[] results = new byte[buf.readableBytes()];
         buf.readBytes(results);
         return results;
      } catch (IOException ex) {
         throw new RuntimeException(ex);
      }
   }

   public static Object getRandomInstanceValue(byte dataType) {
      Random r = ThreadLocalRandom.current();
      switch (dataType) {
      case ZB_TYPE_NO_DATA: return NONE;
      case ZB_TYPE_8BIT: return new Byte((byte)r.nextInt());
      case ZB_TYPE_16BIT: return new Short((short)r.nextInt());
      case ZB_TYPE_24BIT: return new Integer(r.nextInt() & 0xFFFFFF);
      case ZB_TYPE_32BIT: return new Integer(r.nextInt());
      case ZB_TYPE_40BIT: return new Long(r.nextLong() & 0xFFFFFFFFFFL);
      case ZB_TYPE_48BIT: return new Long(r.nextLong() & 0xFFFFFFFFFFFFL);
      case ZB_TYPE_56BIT: return new Long(r.nextLong() & 0xFFFFFFFFFFFFFFL);
      case ZB_TYPE_64BIT: return new Long(r.nextLong());
      case ZB_TYPE_BOOLEAN: return new Boolean(r.nextInt(2) == 0);
      case ZB_TYPE_BITMAP_8BIT: return new Byte((byte)r.nextInt());
      case ZB_TYPE_BITMAP_16BIT: return new Short((short)r.nextInt());
      case ZB_TYPE_BITMAP_24BIT: return new Integer(r.nextInt()) & 0xFFFFFF;
      case ZB_TYPE_BITMAP_32BIT: return new Integer(r.nextInt());
      case ZB_TYPE_BITMAP_40BIT: return new Long(r.nextLong() & 0xFFFFFFFFFFL);
      case ZB_TYPE_BITMAP_48BIT: return new Long(r.nextLong() & 0xFFFFFFFFFFFFL);
      case ZB_TYPE_BITMAP_56BIT: return new Long(r.nextLong() & 0xFFFFFFFFFFFFFFL);
      case ZB_TYPE_BITMAP_64BIT: return new Long(r.nextLong());
      case ZB_TYPE_UNSIGNED_8BIT: return decode((byte)r.nextInt(), false);
      case ZB_TYPE_UNSIGNED_16BIT: return decode((short)r.nextInt(), false);
      case ZB_TYPE_UNSIGNED_24BIT: return decode24(r.nextInt() & 0xFFFFFF, false);
      case ZB_TYPE_UNSIGNED_32BIT: return decode(r.nextInt(), false);
      case ZB_TYPE_UNSIGNED_40BIT: return decode40(r.nextLong() & 0xFFFFFFFFFFL, false);
      case ZB_TYPE_UNSIGNED_48BIT: return decode48(r.nextLong() & 0xFFFFFFFFFFFFL, false);
      case ZB_TYPE_UNSIGNED_56BIT: return decode56(r.nextLong() & 0xFFFFFFFFFFFFFFL, false);
      case ZB_TYPE_UNSIGNED_64BIT: return decode(r.nextLong(), false);
      case ZB_TYPE_SIGNED_8BIT: return decode((byte)r.nextInt(),true);
      case ZB_TYPE_SIGNED_16BIT: return decode((short)r.nextInt(),true);
      case ZB_TYPE_SIGNED_24BIT: return decode24(r.nextInt() & 0xFFFFFF,true);
      case ZB_TYPE_SIGNED_32BIT: return decode(r.nextInt(),true);
      case ZB_TYPE_SIGNED_40BIT: return decode40(r.nextLong() & 0xFFFFFFFFFFL,true);
      case ZB_TYPE_SIGNED_48BIT: return decode48(r.nextLong() & 0xFFFFFFFFFFFFL,true);
      case ZB_TYPE_SIGNED_56BIT: return decode56(r.nextLong() & 0xFFFFFFFFFFFFFFL,true);
      case ZB_TYPE_SIGNED_64BIT: return decode(r.nextLong(),true);
      case ZB_TYPE_ENUM_8BIT: return decode((byte)r.nextInt(),false);
      case ZB_TYPE_ENUM_16BIT: return decode((short)r.nextInt(),false);
      //case ZB_TYPE_SEMIFLOAT: return new Float(r.nextFloat());
      case ZB_TYPE_SEMIFLOAT: return new Float(r.nextFloat());
      case ZB_TYPE_FLOAT: return new Float(r.nextFloat());
      case ZB_TYPE_DOUBLE: return new Double(r.nextDouble());
      case ZB_TYPE_STRING_OCTET: return randomByteString(r);
      case ZB_TYPE_STRING_CHAR: return randomString(r);
      case ZB_TYPE_LONG_STRING_OCTET: return randomByteString(r);
      case ZB_TYPE_LONG_STRING_CHAR: return randomString(r);
      case ZB_TYPE_TIME_OF_DAY: return getRandomTimeOfDay(r);
      case ZB_TYPE_DATE: return getRandomDate(r);
      case ZB_TYPE_UTCTIME: return getRandomUtcTime(r);
      case ZB_TYPE_CLUSTER_ID: return decode(((short)r.nextInt()), false);
      case ZB_TYPE_ATTR_ID: return decode(((short)r.nextInt()), false);
      case ZB_TYPE_BACNET_OID: return decode(r.nextInt(), false);
      case ZB_TYPE_IEEE: return randomByteString(r,8);
      case ZB_TYPE_KEY_128BIT: return randomByteString(r,16);
      case ZB_TYPE_ARRAY: return getRandomArray(r);
      case ZB_TYPE_STRUCT: return getRandomStruct(r);
      case ZB_TYPE_SET: return getRandomSet(r);
      case ZB_TYPE_BAG: return getRandomBag(r);
      default: throw new RuntimeException("bad random value");
      }
   }

   static byte[] randomByteString(Random r) {
      byte[] result = new byte[r.nextInt() & 0x0F];
      for (int i = 0; i < result.length; ++i) {
         result[i] = (byte)r.nextInt();
      }
      return result;
   }

   static byte[] randomByteString(Random r, int length) {
      byte[] result = new byte[length];
      for (int i = 0; i < result.length; ++i) {
         result[i] = (byte)r.nextInt();
      }
      return result;
   }

   static String randomString(Random r) {
      StringBuilder bld = new StringBuilder();
      int numChars = r.nextInt() & 0xF;
      for (int i = 0; i < numChars; ++i) {
         char next = (char)(r.nextInt(126-32+1) + 32);
         bld.append(next);
      }
      return bld.toString();
   }

   static Date getRandomTimeOfDay(Random r) {
      Calendar result = (Calendar)ZIGBEE_EPOCH.clone();
      result.setTimeInMillis(System.currentTimeMillis());
      result.set(Calendar.MILLISECOND, r.nextInt(100) * 10);
      result.set(Calendar.SECOND, r.nextInt(60));
      result.set(Calendar.MINUTE, r.nextInt(60));
      result.set(Calendar.HOUR_OF_DAY, r.nextInt(24));
      return result.getTime();
   }

   static Date getRandomDate(Random r) {
      Calendar result = (Calendar)ZIGBEE_EPOCH.clone();
      result.set(Calendar.MILLISECOND, 0);
      result.set(1900 + r.nextInt(255), r.nextInt(12), r.nextInt(31) + 1, 0, 0, 0);
      return result.getTime();
   }

   static Date getRandomUtcTime(Random r) {
      Calendar result = (Calendar)ZIGBEE_EPOCH.clone();
      result.set(Calendar.MILLISECOND, 0);
      result.set(2000 + r.nextInt(136), r.nextInt(12), r.nextInt(31) + 1, r.nextInt(24), r.nextInt(60), r.nextInt(60));
      return result.getTime();
   }

   static Object getRandomArray(Random r) {
      int num = r.nextInt() & 0xF;
      List<Object> result = new ArrayList<Object>(num);

      int type = r.nextInt(7);
      for (int i = 0; i < num; ++i) {
         result.add(getRandomObject(r,type));
      }

      return result;
   }

   static Object getRandomStruct(Random r) {
      int num = r.nextInt() & 0xF;
      List<Object> result = new ArrayList<Object>(num);
      for (int i = 0; i < num; ++i) {
         result.add(getRandomObject(r));
      }

      return result;
   }

   static Object getRandomSet(Random r) {
      int num = r.nextInt() & 0xF;
      Set<Object> result = new HashSet<Object>(num);
      for (int i = 0; i < num; ++i) {
         result.add(getRandomObject(r));
      }

      return result;
   }

   static Object getRandomBag(Random r) {
      return getRandomStruct(r);
   }

   static Object getRandomObject(Random r) {
      return getRandomObject(r, r.nextInt(7));
   }

   static Object getRandomObject(Random r, int type) {
      switch (type) {
      case 0: return new Byte((byte)r.nextInt());
      case 1: return new Short((short)r.nextInt());
      case 2: return new Integer(r.nextInt());
      case 3: return new Long(r.nextLong());
      case 4: return new Float(r.nextFloat());
      case 5: return new Double(r.nextDouble());
      default: return new Object();
      }
   }
}


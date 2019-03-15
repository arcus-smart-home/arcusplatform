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

import java.util.Arrays;
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
import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.protocol.zigbee.zcl.Alarms.AlarmTableEntry;

import io.netty.buffer.ByteBuf;
import static com.iris.protocol.zigbee.zcl.Constants.*;
import static com.iris.protocol.zigbee.ZclDataUtil.*;

public final class ZclData implements com.iris.protoc.runtime.ProtocStruct {
   private static final Logger log = LoggerFactory.getLogger(ZclData.class);

   public static final int LENGTH_MIN = 1;
   public static final int LENGTH_MAX = -1;

   private final byte dataType;
   private final Object dataValue;

   /////////////////////////////////////////////////////////////////////////////
   // Constructor is private since ZclReadAttributeRecord should only be
   // created through builder methods or through the marshalling code.
   /////////////////////////////////////////////////////////////////////////////

   private ZclData(byte dataType, Object dataValue) {
      this.dataType = dataType;
      this.dataValue = dataValue;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Misc utility methods
   /////////////////////////////////////////////////////////////////////////////

   @Override
   public boolean isFixedSize() {
      return LENGTH_MIN == LENGTH_MAX;
   }

   @Override
   public boolean hasMaxLength() {
      return LENGTH_MAX >= 0;
   }

   @Override
   public int getMinimumSize() {
      return LENGTH_MIN;
   }

   @Override
   public int getMaximumSize() {
      return LENGTH_MAX;
   }

   @Override
   public int getByteSize() {
      return 1 + dataValueSize(dataType, dataValue);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Getters for all fields
   /////////////////////////////////////////////////////////////////////////////

   public int getDataType() {
      return this.dataType & 0xFF;
   }

   public Object getDataValue() {
      return this.dataValue;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Raw accessors for fields that require them
   /////////////////////////////////////////////////////////////////////////////

   public byte rawDataType() {
      return this.dataType;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Hash code and equality methods
   /////////////////////////////////////////////////////////////////////////////

   public boolean equalTo(ZclData other) {
      if (dataType != other.dataType)
         return false;
      if (dataValue instanceof byte[] && other.dataValue instanceof byte[]) {
         if (!Arrays.equals((byte[])dataValue, (byte[])other.dataValue))
            return false;
      } else {
         if (!dataValue.equals(other.dataValue))
            return false;
      }
      return true;
   }

   @Override
   public boolean equals(Object other) {
      if (this == other)
         return true;
      if (other == null)
         return false;
      if (getClass() != other.getClass())
         return false;
      return equalTo((ZclData)other);
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1907475398;
      result = prime * result + this.dataType;
      result = prime * result + this.dataValue.hashCode();
      return result;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Pretty printing toString
   /////////////////////////////////////////////////////////////////////////////

   @Override
   public String toString() {
      if (dataValue instanceof byte[]) {
         return "ZclData [" +
            "dataType=0x" + Integer.toHexString(dataType & 0xFF) +
            ",dataValue=" + Arrays.toString((byte[])dataValue) +
            "]";
      }

      return "ZclData [" +
         "dataType=" + Integer.toHexString(dataType & 0xFF) +
         ",dataValue=" + dataValue +
         "]";
   }

   /////////////////////////////////////////////////////////////////////////////
   // Builders to create new ZclData objects
   /////////////////////////////////////////////////////////////////////////////

   public static Builder builder() {
      return new Builder();
   }

   public static Builder builder(ZclData clone) {
      Builder bld = new Builder();
      return bld;
   }

   public static Builder builder(Builder clone) {
      Builder bld = new Builder();
      return bld;
   }

   public static final class Builder {
      private byte dataType;
      private Object dataValue;

      public Builder setNoData() {
         dataType = ZB_TYPE_NO_DATA;
         dataValue = NONE;
         return this;
      }

      public Builder setNoData(int dataType) {
         dataType = (byte)dataType;
         dataValue = NONE;
         return this;
      }

      public Builder set8Bit(byte value) {
         dataType = ZB_TYPE_8BIT;
         dataValue = value;
         return this;
      }

      public Builder set16Bit(short value) {
         dataType = ZB_TYPE_16BIT;
         dataValue = value;
         return this;
      }

      public Builder set24Bit(int value) {
         dataType = ZB_TYPE_24BIT;
         dataValue = value & 0xFFFFFF;
         return this;
      }

      public Builder set32Bit(int value) {
         dataType = ZB_TYPE_32BIT;
         dataValue = value;
         return this;
      }

      public Builder set40Bit(long value) {
         dataType = ZB_TYPE_40BIT;
         dataValue = value & 0xFFFFFFFFFFL;
         return this;
      }

      public Builder set48Bit(long value) {
         dataType = ZB_TYPE_48BIT;
         dataValue = value & 0xFFFFFFFFFFFFL;
         return this;
      }

      public Builder set56Bit(long value) {
         dataType = ZB_TYPE_56BIT;
         dataValue = value & 0xFFFFFFFFFFFFFFL;
         return this;
      }

      public Builder set64Bit(long value) {
         dataType = ZB_TYPE_64BIT;
         dataValue = value;
         return this;
      }

      public Builder setBoolean(boolean value) {
         dataType = ZB_TYPE_BOOLEAN;
         dataValue = value;
         return this;
      }

      public Builder set8BitBitmap(byte value) {
         dataType = ZB_TYPE_BITMAP_8BIT;
         dataValue = value;
         return this;
      }

      public Builder set16BitBitmap(short value) {
         dataType = ZB_TYPE_BITMAP_16BIT;
         dataValue = value;
         return this;
      }

      public Builder set24BitBitmap(int value) {
         dataType = ZB_TYPE_BITMAP_24BIT;
         dataValue = value & 0xFFFFFF;
         return this;
      }

      public Builder set32BitBitmap(int value) {
         dataType = ZB_TYPE_BITMAP_32BIT;
         dataValue = value;
         return this;
      }

      public Builder set40BitBitmap(long value) {
         dataType = ZB_TYPE_BITMAP_40BIT;
         dataValue = value & 0xFFFFFFFFFFL;
         return this;
      }

      public Builder set48BitBitmap(long value) {
         dataType = ZB_TYPE_BITMAP_48BIT;
         dataValue = value & 0xFFFFFFFFFFFFL;
         return this;
      }

      public Builder set56BitBitmap(long value) {
         dataType = ZB_TYPE_BITMAP_56BIT;
         dataValue = value & 0xFFFFFFFFFFFFFFL;
         return this;
      }

      public Builder set64BitBitmap(long value) {
         dataType = ZB_TYPE_BITMAP_64BIT;
         dataValue = value;
         return this;
      }

      public Builder set8BitUnsigned(byte value) {
         dataType = ZB_TYPE_UNSIGNED_8BIT;
         dataValue = value;
         return this;
      }

      public Builder set16BitUnsigned(short value) {
         dataType = ZB_TYPE_UNSIGNED_16BIT;
         dataValue = value;
         return this;
      }

      public Builder set24BitUnsigned(int value) {
         dataType = ZB_TYPE_UNSIGNED_24BIT;
         dataValue = value & 0xFFFFFF;
         return this;
      }

      public Builder set32BitUnsigned(int value) {
         dataType = ZB_TYPE_UNSIGNED_32BIT;
         dataValue = value;
         return this;
      }

      public Builder set40BitUnsigned(long value) {
         dataType = ZB_TYPE_UNSIGNED_40BIT;
         dataValue = value & 0xFFFFFFFFFFL;
         return this;
      }

      public Builder set48BitUnsigned(long value) {
         dataType = ZB_TYPE_UNSIGNED_48BIT;
         dataValue = value & 0xFFFFFFFFFFFFL;
         return this;
      }

      public Builder set56BitUnsigned(long value) {
         dataType = ZB_TYPE_UNSIGNED_56BIT;
         dataValue = value & 0xFFFFFFFFFFFFFFL;
         return this;
      }

      public Builder set64BitUnsigned(long value) {
         dataType = ZB_TYPE_UNSIGNED_64BIT;
         dataValue = value;
         return this;
      }

      public Builder set8BitSigned(byte value) {
         dataType = ZB_TYPE_SIGNED_8BIT;
         dataValue = value;
         return this;
      }

      public Builder set16BitSigned(short value) {
         dataType = ZB_TYPE_SIGNED_16BIT;
         dataValue = value;
         return this;
      }

      public Builder set24BitSigned(int value) {
         dataType = ZB_TYPE_SIGNED_24BIT;
         dataValue = value & 0xFFFFFF;
         return this;
      }

      public Builder set32BitSigned(int value) {
         dataType = ZB_TYPE_SIGNED_32BIT;
         dataValue = value;
         return this;
      }

      public Builder set40BitSigned(long value) {
         dataType = ZB_TYPE_SIGNED_40BIT;
         dataValue = value & 0xFFFFFFFFFFL;
         return this;
      }

      public Builder set48BitSigned(long value) {
         dataType = ZB_TYPE_SIGNED_48BIT;
         dataValue = value & 0xFFFFFFFFFFFFL;
         return this;
      }

      public Builder set56BitSigned(long value) {
         dataType = ZB_TYPE_SIGNED_56BIT;
         dataValue = value & 0xFFFFFFFFFFFFFFL;
         return this;
      }

      public Builder set64BitSigned(long value) {
         dataType = ZB_TYPE_SIGNED_64BIT;
         dataValue = value;
         return this;
      }

      public Builder set8BitEnum(byte value) {
         dataType = ZB_TYPE_ENUM_8BIT;
         dataValue = value;
         return this;
      }

      public Builder set16BitEnum(short value) {
         dataType = ZB_TYPE_ENUM_16BIT;
         dataValue = value;
         return this;
      }

      public Builder setSemiFloat(float value) {
         throw new UnsupportedOperationException();
         //dataType = ZB_TYPE_SEMIFLOAT;
      }

      public Builder setFloat(float value) {
         dataType = ZB_TYPE_FLOAT;
         dataValue = (Float.isNaN(value)) ? NONE : value;
         return this;
      }

      public Builder setDouble(double value) {
         dataType = ZB_TYPE_DOUBLE;
         dataValue = (Double.isNaN(value)) ? NONE : value;
         return this;
      }

      public Builder setString(byte[] value) {
         if (value != null && value.length >= 0xFF) {
            throw new IllegalArgumentException();
         }

         dataType = ZB_TYPE_STRING_OCTET;
         if (value == null) {
            dataValue = NONE;
         } else {
            dataValue = Arrays.copyOf(value, value.length);
         }

         return this;
      }

      public Builder setString(String value) {
         if (value != null) {
            // TODO: charset encoding?
            byte[] data = value.getBytes(StandardCharsets.UTF_8);
            if (data.length >= 0xFF) {
               throw new IllegalArgumentException();
            }
         }

         dataType = ZB_TYPE_STRING_CHAR;
         if (value == null) {
            dataValue = NONE;
         } else {
            dataValue = value;
         }

         return this;
      }

      public Builder setLongString(byte[] value) {
         if (value != null && value.length >= 0xFFFF) {
            throw new IllegalArgumentException();
         }

         dataType = ZB_TYPE_LONG_STRING_OCTET;
         if (value == null) {
            dataValue = NONE;
         } else {
            dataValue = Arrays.copyOf(value, value.length);
         }

         return this;
      }

      public Builder setLongString(String value) {
         if (value != null) {
            // TODO: charset encoding?
            byte[] data = value.getBytes(StandardCharsets.UTF_8);
            if (data.length >= 0xFFFF) {
               throw new IllegalArgumentException();
            }
         }

         dataType = ZB_TYPE_LONG_STRING_CHAR;
         if (value == null) {
            dataValue = NONE;
         } else {
            dataValue = value;
         }

         return this;
      }

      public Builder setTimeOfDate(Date tod) {
         dataType = ZB_TYPE_TIME_OF_DAY;
         dataValue = tod == null ? NONE : (Date)tod.clone();
         return this;
      }

      public Builder setDate(Date date) {
         dataType = ZB_TYPE_DATE;

         dataValue = NONE;
         if (date != null) {
            Calendar value = (Calendar)ZIGBEE_EPOCH.clone();
            value.setTime(date);
            value.set(Calendar.MILLISECOND, 0);
            value.set(Calendar.SECOND, 0);
            value.set(Calendar.MINUTE, 0);
            value.set(Calendar.HOUR, 0);
            dataValue = value.getTime();
         }

         return this;
      }

      public Builder setUtcTime(Date date) {
         dataType = ZB_TYPE_UTCTIME;

         dataValue = NONE;
         if (date != null) {
            Calendar value = (Calendar)ZIGBEE_EPOCH.clone();
            value.setTime(date);
            value.set(Calendar.MILLISECOND, 0);
            dataValue = value.getTime();
         }

         return this;
      }

      public Builder setClusterId(short id) {
         dataType = ZB_TYPE_CLUSTER_ID;
         dataValue = id;
         return this;
      }

      public Builder setAttributeId(short id) {
         dataType = ZB_TYPE_ATTR_ID;
         dataValue = id;
         return this;
      }

      public Builder setBacNetOid(int oid) {
         dataType = ZB_TYPE_BACNET_OID;
         dataValue = oid;
         return this;
      }

      public Builder setIeee(byte[] ieee) {
         dataType = ZB_TYPE_IEEE;
         dataValue = Arrays.copyOf(ieee, 8);
         return this;
      }

      public Builder setIeee(long ieee) {
         byte[] data = new byte[8];
         ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
         buf.putLong(ieee);

         dataType = ZB_TYPE_IEEE;
         dataValue = buf.array();
         return this;
      }

      public Builder set128BitKey(byte[] key) {
         dataType = ZB_TYPE_KEY_128BIT;
         dataValue = Arrays.copyOf(key, 16);
         return this;
      }

      private Builder set(byte dataType, Object dataValue) {
         this.dataType = dataType;
         this.dataValue = dataValue;
         return this;
      }

      public ZclData create() {
         if (dataValue == null) {
            dataType = ZB_TYPE_NO_DATA;
            dataValue = NONE;
         }

         return new ZclData(dataType, dataValue);
      }
   }


   /////////////////////////////////////////////////////////////////////////////
   // SerDe accessor
   /////////////////////////////////////////////////////////////////////////////

   public static com.iris.protoc.runtime.ProtocSerDe<ZclData> serde() {
      return SerDe.INSTANCE;
   }

   @Override
   public com.iris.protoc.runtime.ProtocSerDe<ZclData> getSerDe() {
      return SerDe.INSTANCE;
   }

   private static enum SerDe implements com.iris.protoc.runtime.ProtocSerDe<ZclData>,
                                        com.iris.protoc.runtime.ProtocSerDe.Io<ZclData>,
                                        com.iris.protoc.runtime.ProtocSerDe.Nio<ZclData>,
                                        com.iris.protoc.runtime.ProtocSerDe.Netty<ZclData> {
      INSTANCE;

      @Override
      public com.iris.protoc.runtime.ProtocSerDe.Io<ZclData> ioSerDe() {
         return this;
      }

      @Override
      public com.iris.protoc.runtime.ProtocSerDe.Nio<ZclData> nioSerDe() {
         return this;
      }

      @Override
      public com.iris.protoc.runtime.ProtocSerDe.Netty<ZclData> nettySerDe() {
         return this;
      }

      @Override
      public ZclData fromBytes(ByteOrder order, byte[] data) {
         return fromBytes(order, data, 0, data.length);
      }

      @Override
      public ZclData fromBytes(ByteOrder order, byte[] data, int offset, int length) {
         try {
            return nioSerDe().decode(ByteBuffer.wrap(data,offset,length).order(order));
         } catch (IOException ex) {
            throw new RuntimeException("could not deserialize {}", ex);
         }
      }

      //////////////////////////////////////////////////////////////////////////
      // Marshalling to/from Java IO streams
      //////////////////////////////////////////////////////////////////////////

      @Override
      public ZclData decode(DataInput input) throws IOException {
         byte dataType = input.readByte();
         Object dataValue = ZclDataUtil.decode(dataType, input);
         return new ZclData(dataType, dataValue);
      }

      @Override
      public void encode(DataOutput output, ZclData value) throws IOException {
         output.writeByte((byte)value.dataType);
         ZclDataUtil.encode(output, value.dataType, value.dataValue);
      }

      //////////////////////////////////////////////////////////////////////////
      // Marshalling to/from Java NIO streams
      //////////////////////////////////////////////////////////////////////////

      @Override
      public ZclData decode(ByteBuffer input) throws IOException {
         byte dataType = input.get();
         Object dataValue = ZclDataUtil.decode(dataType, input);
         return new ZclData(dataType, dataValue);
      }

      @Override
      public void encode(ByteBuffer output, ZclData value) throws IOException {
         output.put((byte)value.dataType);
         ZclDataUtil.encode(output, value.dataType, value.dataValue);
      }

      //////////////////////////////////////////////////////////////////////////
      // Marshalling to/from Netty streams
      //////////////////////////////////////////////////////////////////////////

      @Override
      public ZclData decode(ByteBuf input) throws IOException {
         byte dataType = input.readByte();
         Object dataValue = ZclDataUtil.decode(dataType, input);
         return new ZclData(dataType, dataValue);
      }

      @Override
      public void encode(ByteBuf output, ZclData value) throws IOException {
         output.writeByte((byte)value.dataType);
         ZclDataUtil.encode(output, value.dataType, value.dataValue);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Conversion to byte array
   /////////////////////////////////////////////////////////////////////////////

   @Override
   public byte[] toBytes(ByteOrder order) throws IOException {
      return toByteBuf(order).array();
   }

   @Override
   public ByteBuffer toByteBuffer(ByteOrder order) throws IOException {
      return ByteBuffer.wrap(toBytes(order)).order(order);
   }

   @Override
   public ByteBuf toByteBuf(ByteOrder order) throws IOException {
      int length = getByteSize();
      ByteBuf buffer = io.netty.buffer.Unpooled.buffer(length).order(order);

      serde().nettySerDe().encode(buffer, this);
      return buffer;
   }

   ////////////////////////////////////////////////////////////////////////////
   // ZclData generators
   /////////////////////////////////////////////////////////////////////////////

   public static ZclData getEmptyInstance(byte dataType) {
      return ZclData.builder().set(dataType, ZclDataUtil.NONE).create();
   }

   public static ZclData getEmptyInstance() {
      return ZclData.builder().create();
   }

   public static ZclData getRandomInstance() {
      byte dataType = (byte)ZclDataUtil.getRandomInstanceType();
      Object dataValue = ZclDataUtil.getRandomInstanceValue(dataType);

      ZclData.Builder bld = ZclData.builder();
      bld.set(dataType, dataValue);
      return bld.create();
   }
}


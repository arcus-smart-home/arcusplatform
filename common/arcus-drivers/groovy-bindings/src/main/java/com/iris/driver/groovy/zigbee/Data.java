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
package com.iris.driver.groovy.zigbee;

import groovy.lang.GroovyObjectSupport;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.iris.driver.groovy.zigbee.cluster.alertme.ZigbeeAlertmeDataBinding;
import com.iris.driver.groovy.zigbee.cluster.zcl.ZigbeeZclDataBinding;
import com.iris.driver.groovy.zigbee.cluster.zdp.ZigbeeZdpDataBinding;
import com.iris.protocol.zigbee.ZclData;
import com.iris.protocol.zigbee.zcl.Constants;

public class Data extends GroovyObjectSupport {
   private final static String ZCL_DATA_BINDING_PROPERTY = "Zcl";
   private final static String ZDP_DATA_BINDING_PROPERTY = "Zdp";
   private final static String AME_DATA_BINDING_PROPERTY = "Alertme";

   private final static Map<String, Object> properties = new HashMap<>();

   static {
      Field[] fields = Constants.class.getFields();
      for (Field field : fields) {
         String name = field.getName();
         if (name.startsWith("ZB_TYPE")) {
            try {
               int dataType = field.getByte(null) & 0xFF;
               properties.put(name.substring(3, name.length()), dataType);
            } catch (IllegalArgumentException e) {
               // Skip it, it's a static field so null will always be okay.
            } catch (IllegalAccessException e) {
               // Skip it, it's always going to be public.
            }
         }
      }
      properties.put(ZCL_DATA_BINDING_PROPERTY, new ZigbeeZclDataBinding());
      properties.put(ZDP_DATA_BINDING_PROPERTY, new ZigbeeZdpDataBinding());
      properties.put(AME_DATA_BINDING_PROPERTY, new ZigbeeAlertmeDataBinding());
   }

   @Override
   public Object getProperty(String property) {
      Object value = properties.get(property);
      if (value != null) {
         return value;
      }
      return super.getProperty(property);
   }

   @Override
   public void setProperty(String property, Object newValue) {
      throw new UnsupportedOperationException("Properties cannot be set on the Data object");
   }

   //////////////////
   // Encoding Methods
   //////////////////

   public ZclData encodeNoData() {
      return ZclData.builder().setNoData().create();
   }

   public ZclData encodeNoData(int dataType) {
      return ZclData.builder().setNoData(dataType).create();
   }

   public ZclData encode8Bit(byte value) {
      return ZclData.builder().set8Bit(value).create();
   }

   public ZclData encode8Bit(int value) {
      return ZclData.builder().set8Bit((byte)value).create();
   }

   public ZclData encode16Bit(short value) {
      return ZclData.builder().set16Bit(value).create();
   }

   public ZclData encode16Bit(int value) {
      return ZclData.builder().set16Bit((short)value).create();
   }

   public ZclData encode24Bit(int value) {
      return ZclData.builder().set24Bit(value).create();
   }

   public ZclData encode32Bit(int value) {
      return ZclData.builder().set32Bit(value).create();
   }

   public ZclData encode40Bit(long value) {
      return ZclData.builder().set40Bit(value).create();
   }

   public ZclData encode48Bit(long value) {
      return ZclData.builder().set48Bit(value).create();
   }

   public ZclData encode56Bit(long value) {
      return ZclData.builder().set56Bit(value).create();
   }

   public ZclData encode64Bit(long value) {
      return ZclData.builder().set64Bit(value).create();
   }

   public ZclData encodeBoolean(boolean value) {
      return ZclData.builder().setBoolean(value).create();
   }

   public ZclData encode8BitBitmap(byte value) {
      return ZclData.builder().set8BitBitmap(value).create();
   }

   public ZclData encode8BitBitmap(int value) {
      return ZclData.builder().set8BitBitmap((byte)value).create();
   }

   public ZclData encode16BitBitmap(short value) {
      return ZclData.builder().set16BitBitmap(value).create();
   }

   public ZclData encode16BitBitmap(int value) {
      return ZclData.builder().set16BitBitmap((short)value).create();
   }

   public ZclData encode24BitBitmap(int value) {
      return ZclData.builder().set24BitBitmap(value).create();
   }

   public ZclData encode32BitBitmap(int value) {
      return ZclData.builder().set32BitBitmap(value).create();
   }

   public ZclData encode40BitBitmap(long value) {
      return ZclData.builder().set40BitBitmap(value).create();
   }

   public ZclData encode48BitBitmap(long value) {
      return ZclData.builder().set48BitBitmap(value).create();
   }

   public ZclData encode56BitBitmap(long value) {
      return ZclData.builder().set56BitBitmap(value).create();
   }

   public ZclData encode64BitBitmap(long value) {
      return ZclData.builder().set64BitBitmap(value).create();
   }

   public ZclData encode8BitUnsigned(byte value) {
      return ZclData.builder().set8BitUnsigned(value).create();
   }

   public ZclData encode8BitUnsigned(int value) {
      return ZclData.builder().set8BitUnsigned((byte)value).create();
   }

   public ZclData encode16BitUnsigned(short value) {
      return ZclData.builder().set16BitUnsigned(value).create();
   }

   public ZclData encode16BitUnsigned(int value) {
      return ZclData.builder().set16BitUnsigned((short)value).create();
   }

   public ZclData encode24BitUnsigned(int value) {
      return ZclData.builder().set24BitUnsigned(value).create();
   }

   public ZclData encode32BitUnsigned(int value) {
      return ZclData.builder().set32BitUnsigned(value).create();
   }

   public ZclData encode40BitUnsigned(long value) {
      return ZclData.builder().set40BitUnsigned(value).create();
   }

   public ZclData encode48BitUnsigned(long value) {
      return ZclData.builder().set48BitUnsigned(value).create();
   }

   public ZclData encode56BitUnsigned(long value) {
      return ZclData.builder().set56BitUnsigned(value).create();
   }

   public ZclData encode64BitUnsigned(long value) {
      return ZclData.builder().set64BitUnsigned(value).create();
   }

   public ZclData encode8BitSigned(byte value) {
      return ZclData.builder().set8BitSigned(value).create();
   }

   public ZclData encode8BitSigned(int value) {
      return ZclData.builder().set8BitSigned((byte)value).create();
   }

   public ZclData encode16BitSigned(short value) {
      return ZclData.builder().set16BitSigned(value).create();
   }

   public ZclData encode16BitSigned(int value) {
      return ZclData.builder().set16BitSigned((short)value).create();
   }

   public ZclData encode24BitSigned(int value) {
      return ZclData.builder().set24BitSigned(value).create();
   }

   public ZclData encode32BitSigned(int value) {
      return ZclData.builder().set32BitSigned(value).create();
   }

   public ZclData encode40BitSigned(long value) {
      return ZclData.builder().set40BitSigned(value).create();
   }

   public ZclData encode48BitSigned(long value) {
      return ZclData.builder().set48BitSigned(value).create();
   }

   public ZclData encode56BitSigned(long value) {
      return ZclData.builder().set56BitSigned(value).create();
   }

   public ZclData encode64BitSigned(long value) {
      return ZclData.builder().set64BitSigned(value).create();
   }

   public ZclData encode8BitEnum(byte value) {
      return ZclData.builder().set8BitEnum(value).create();
   }

   public ZclData encode8BitEnum(int value) {
      return ZclData.builder().set8BitEnum((byte)value).create();
   }

   public ZclData encode16BitEnum(short value) {
      return ZclData.builder().set16BitEnum(value).create();
   }

   public ZclData encode16BitEnum(int value) {
      return ZclData.builder().set16BitEnum((short)value).create();
   }

   public ZclData encodeFloat(float value) {
      return ZclData.builder().setFloat(value).create();
   }

   public ZclData encodeDouble(double value) {
      return ZclData.builder().setDouble(value).create();
   }

   public ZclData encodeString(byte[] value) {
      return ZclData.builder().setString(value).create();
   }

   public ZclData encodeString(String value) {
      return ZclData.builder().setString(value).create();
   }

   public ZclData encodeLongString(byte[] value) {
      return ZclData.builder().setLongString(value).create();
   }

   public ZclData encodeLongString(String value) {
      return ZclData.builder().setLongString(value).create();
   }

   public ZclData encodeTimeOfDate(Date tod) {
      return ZclData.builder().setTimeOfDate(tod).create();
   }

   public ZclData encodeDate(Date date) {
      return ZclData.builder().setDate(date).create();
   }

   public ZclData encodeUtcTime(Date date) {
      return ZclData.builder().setUtcTime(date).create();
   }

   public ZclData encodeClusterId(short id) {
      return ZclData.builder().setClusterId(id).create();
   }

   public ZclData encodeClusterId(int id) {
      return ZclData.builder().setClusterId((short)id).create();
   }

   public ZclData encodeAttributeId(short id) {
      return ZclData.builder().setAttributeId(id).create();
   }

   public ZclData encodeAttributeId(int id) {
      return ZclData.builder().setAttributeId((short)id).create();
   }

   public ZclData encodeBacNetOid(int oid) {
      return ZclData.builder().setBacNetOid(oid).create();
   }

   public ZclData encodeIeee(byte[] ieee) {
      return ZclData.builder().setIeee(ieee).create();
   }

   public ZclData encodeIeee(long ieee) {
      return ZclData.builder().setIeee(ieee).create();
   }

   public ZclData encode128BitKey(byte[] key) {
      return ZclData.builder().set128BitKey(key).create();
   }
}


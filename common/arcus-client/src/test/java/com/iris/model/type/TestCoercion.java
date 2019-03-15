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
package com.iris.model.type;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class TestCoercion {

   @Test
   public void testBooleanCoercion() {
      assertNull(BooleanType.INSTANCE.coerce(null));
      assertTrue(BooleanType.INSTANCE.coerce(Boolean.TRUE));
      assertFalse(BooleanType.INSTANCE.coerce(Boolean.FALSE));
      assertTrue(BooleanType.INSTANCE.coerce(true));
      assertFalse(BooleanType.INSTANCE.coerce(false));
      assertTrue(BooleanType.INSTANCE.coerce("true"));
      assertFalse(BooleanType.INSTANCE.coerce("false"));
      assertTrue(BooleanType.INSTANCE.coerce(1));
      assertFalse(BooleanType.INSTANCE.coerce(0));
      assertTrue(BooleanType.INSTANCE.coerce(1.0));
      assertFalse(BooleanType.INSTANCE.coerce(0.0));

      try {
         BooleanType.INSTANCE.coerce(new ArrayList<String>());
         fail("Expected an IllegalArgumentException");
      } catch(IllegalArgumentException iae) { /* ok */ }
   }

   @Test
   public void testByteCoercion() {
      assertNull(ByteType.INSTANCE.coerce(null));
      assertEquals(Byte.valueOf(Byte.MIN_VALUE), ByteType.INSTANCE.coerce(Byte.valueOf(Byte.MIN_VALUE)));
      assertEquals(Byte.valueOf(Byte.MAX_VALUE), ByteType.INSTANCE.coerce(Byte.valueOf(Byte.MAX_VALUE)));
      assertEquals(Byte.valueOf(Byte.MIN_VALUE), ByteType.INSTANCE.coerce(Byte.MIN_VALUE));
      assertEquals(Byte.valueOf(Byte.MAX_VALUE), ByteType.INSTANCE.coerce(Byte.MAX_VALUE));
      assertEquals(Byte.valueOf(Byte.MIN_VALUE), ByteType.INSTANCE.coerce((int) Byte.MIN_VALUE));
      assertEquals(Byte.valueOf(Byte.MAX_VALUE), ByteType.INSTANCE.coerce((int) Byte.MAX_VALUE));
      assertEquals(Byte.valueOf(Byte.MIN_VALUE), ByteType.INSTANCE.coerce((double) Byte.MIN_VALUE));
      assertEquals(Byte.valueOf(Byte.MAX_VALUE), ByteType.INSTANCE.coerce((double) Byte.MAX_VALUE));
      assertEquals(Byte.valueOf(Byte.MIN_VALUE), ByteType.INSTANCE.coerce("-128"));
      assertEquals(Byte.valueOf(Byte.MAX_VALUE), ByteType.INSTANCE.coerce("127"));

      try {
         ByteType.INSTANCE.coerce(new ArrayList<String>());
         fail("Expected an IllegalArgumentException");
      } catch(IllegalArgumentException iae) { /* ok */ }
      
   }

   @Test
   public void testDoubleCoercion() {
      assertNull(DoubleType.INSTANCE.coerce(null));
      assertEquals(Double.valueOf(3.14), DoubleType.INSTANCE.coerce(Double.valueOf(3.14)));
      assertEquals(Double.valueOf(3.14), DoubleType.INSTANCE.coerce(3.14));
      assertEquals(Double.valueOf(3.0), DoubleType.INSTANCE.coerce(3));
      assertEquals(Double.valueOf(3.14), DoubleType.INSTANCE.coerce("3.14"));

      try {
         DoubleType.INSTANCE.coerce(new ArrayList<String>());
         fail("Expected an IllegalArgumentException");
      } catch(IllegalArgumentException iae) { /* ok */ }
   }

   @Test
   public void testEnumCoercion() {
      EnumType enumType = new EnumType("ON", "OFF");

      assertNull(enumType.coerce(null));
      assertEquals("ON", enumType.coerce("ON"));
      assertEquals("OFF", enumType.coerce("OFF"));

      try {
         enumType.coerce("foo");
         fail("Expected an IllegalArgumentException");
      } catch(IllegalArgumentException iae) { /* ok */ }
   }

   @Test
   public void testIntCoercion() {
      assertNull(IntType.INSTANCE.coerce(null));
      assertEquals(Integer.valueOf(Integer.MIN_VALUE), IntType.INSTANCE.coerce(Integer.valueOf(Integer.MIN_VALUE)));
      assertEquals(Integer.valueOf(Integer.MAX_VALUE), IntType.INSTANCE.coerce(Integer.valueOf(Integer.MAX_VALUE)));
      assertEquals(Integer.valueOf(Integer.MIN_VALUE), IntType.INSTANCE.coerce(Integer.MIN_VALUE));
      assertEquals(Integer.valueOf(Integer.MAX_VALUE), IntType.INSTANCE.coerce(Integer.MAX_VALUE));
      assertEquals(Integer.valueOf(Integer.MIN_VALUE), IntType.INSTANCE.coerce((double) Integer.MIN_VALUE));
      assertEquals(Integer.valueOf(Integer.MAX_VALUE), IntType.INSTANCE.coerce((double) Integer.MAX_VALUE));
      assertEquals(Integer.valueOf(Integer.MIN_VALUE), IntType.INSTANCE.coerce((double) Integer.MIN_VALUE));
      assertEquals(Integer.valueOf(Integer.MAX_VALUE), IntType.INSTANCE.coerce((double) Integer.MAX_VALUE));
      assertEquals(Integer.valueOf(Integer.MIN_VALUE), IntType.INSTANCE.coerce(String.valueOf(Integer.MIN_VALUE)));
      assertEquals(Integer.valueOf(Integer.MAX_VALUE), IntType.INSTANCE.coerce(String.valueOf(Integer.MAX_VALUE)));

      try {
         IntType.INSTANCE.coerce(new ArrayList<String>());
         fail("Expected an IllegalArgumentException");
      } catch(IllegalArgumentException iae) { /* ok */ }

      try {
         long min = Integer.MIN_VALUE;
         IntType.INSTANCE.coerce(min - 1);
         fail("Expected an IllegalArgumentException");
      } catch(IllegalArgumentException iae) { /* ok */ }

      try {
         long max = Integer.MAX_VALUE;
         IntType.INSTANCE.coerce(max + 1);
         fail("Expected an IllegalArgumentException");
      } catch(IllegalArgumentException iae) { /* ok */ }

      try {
         IntType.INSTANCE.coerce(3.14);
         fail("Expected an IllegalArgumentException");
      } catch(IllegalArgumentException iae) { /* ok */ }
   }

   @Test
   public void testLongCoercion() {
      assertNull(LongType.INSTANCE.coerce(null));
      assertEquals(Long.valueOf(Long.MIN_VALUE), LongType.INSTANCE.coerce(Long.valueOf(Long.MIN_VALUE)));
      assertEquals(Long.valueOf(Long.MAX_VALUE), LongType.INSTANCE.coerce(Long.valueOf(Long.MAX_VALUE)));
      assertEquals(Long.valueOf(Long.MIN_VALUE), LongType.INSTANCE.coerce(Long.MIN_VALUE));
      assertEquals(Long.valueOf(Long.MAX_VALUE), LongType.INSTANCE.coerce(Long.MAX_VALUE));
      assertEquals(Long.valueOf(Long.MIN_VALUE), LongType.INSTANCE.coerce((double) Long.MIN_VALUE));
      assertEquals(Long.valueOf(Long.MAX_VALUE), LongType.INSTANCE.coerce((double) Long.MAX_VALUE));
      assertEquals(Long.valueOf(Long.MIN_VALUE), LongType.INSTANCE.coerce((double) Long.MIN_VALUE));
      assertEquals(Long.valueOf(Long.MAX_VALUE), LongType.INSTANCE.coerce((double) Long.MAX_VALUE));
      assertEquals(Long.valueOf(Long.MIN_VALUE), LongType.INSTANCE.coerce(String.valueOf(Long.MIN_VALUE)));
      assertEquals(Long.valueOf(Long.MAX_VALUE), LongType.INSTANCE.coerce(String.valueOf(Long.MAX_VALUE)));

      try {
         LongType.INSTANCE.coerce(new ArrayList<String>());
         fail("Expected an IllegalArgumentException");
      } catch(IllegalArgumentException iae) { /* ok */ }
   }

   @Test
   public void testTimestampCoercion() {

      Date timestamp = new Date();
      Calendar tsCal = Calendar.getInstance();
      tsCal.setTime(timestamp);

      assertNull(TimestampType.INSTANCE.coerce(null));
      assertEquals(timestamp, TimestampType.INSTANCE.coerce(timestamp));
      assertEquals(timestamp, TimestampType.INSTANCE.coerce(tsCal));
      assertEquals(timestamp, TimestampType.INSTANCE.coerce(timestamp.getTime()));
      assertEquals(timestamp, TimestampType.INSTANCE.coerce(String.valueOf(timestamp.getTime())));

      try {
         TimestampType.INSTANCE.coerce("2015-02-05 00:00:00");
         fail("Expected an IllegalArgumentException");
      } catch(IllegalArgumentException iae) { /* ok */ }
   }

   @Test
   public void testListCoercionSimple() {
      ListType listType = new ListType(StringType.INSTANCE);

      String[] data = new String[] { "foo", "bar" };
      List<String> dataList = Arrays.asList(data);
      Set<String> dataSet = new LinkedHashSet<>(dataList);


      assertNull(listType.coerce(null));
      assertEquals(dataList, listType.coerce(dataList));
      assertEquals(dataList, listType.coerce(data));
      assertEquals(dataList, listType.coerce(dataSet));

      try {
         listType.coerce("2015-02-05 00:00:00");
         fail("Expected an IllegalArgumentException");
      } catch(IllegalArgumentException iae) { /* ok */ }
   }

   @Test
   public void testListCoercionNested() {
      ListType listType = new ListType(new MapType(StringType.INSTANCE));

      Map[] data = new Map[2];
      data[0] = new HashMap<String,String>();
      data[0].put("foo", "bar");
      data[1] = new HashMap<String,String>();
      data[1].put("fubar", "baz");

      List<Map<String,String>> dataList = new ArrayList<Map<String,String>>();
      for(int i = 0; i < data.length; i++) {
         dataList.add(data[i]);
      }

      Set<Map<String,String>> dataSet = new LinkedHashSet<>(dataList);


      assertNull(listType.coerce(null));
      assertEquals(dataList, listType.coerce(dataList));
      assertEquals(dataList, listType.coerce(data));
      assertEquals(dataList, listType.coerce(dataSet));

      try {
         listType.coerce("2015-02-05 00:00:00");
         fail("Expected an IllegalArgumentException");
      } catch(IllegalArgumentException iae) { /* ok */ }
   }

   @Test
   public void testSetCoercionSimple() {
      SetType setType = new SetType(StringType.INSTANCE);

      String[] data = new String[] { "foo", "bar" };
      List<String> dataList = Arrays.asList(data);
      Set<String> dataSet = new LinkedHashSet<>(dataList);


      assertNull(setType.coerce(null));
      assertEquals(dataSet, setType.coerce(dataList));
      assertEquals(dataSet, setType.coerce(data));
      assertEquals(dataSet, setType.coerce(dataSet));

      try {
         setType.coerce("2015-02-05 00:00:00");
         fail("Expected an IllegalArgumentException");
      } catch(IllegalArgumentException iae) { /* ok */ }
   }

   @Test
   public void testSetCoercionNested() {
      SetType setType = new SetType(new MapType(StringType.INSTANCE));

      Map[] data = new Map[2];
      data[0] = new HashMap<String,String>();
      data[0].put("foo", "bar");
      data[1] = new HashMap<String,String>();
      data[1].put("fubar", "baz");

      List<Map<String,String>> dataList = new ArrayList<Map<String,String>>();
      for(int i = 0; i < data.length; i++) {
         dataList.add(data[i]);
      }

      Set<Map<String,String>> dataSet = new LinkedHashSet<>(dataList);


      assertNull(setType.coerce(null));
      assertEquals(dataSet, setType.coerce(dataList));
      assertEquals(dataSet, setType.coerce(data));
      assertEquals(dataSet, setType.coerce(dataSet));

      try {
         setType.coerce("2015-02-05 00:00:00");
         fail("Expected an IllegalArgumentException");
      } catch(IllegalArgumentException iae) { /* ok */ }
   }

   @Test
   public void testMapCoercionSimple() {
      MapType mapType = new MapType(StringType.INSTANCE);

      Map<String,String> map = new HashMap<>();
      map.put("foo", "bar");
      map.put("fubar", "baz");

      assertNull(mapType.coerce(null));
      assertEquals(map, mapType.coerce(map));

      try {
         mapType.coerce("2015-02-05 00:00:00");
         fail("Expected an IllegalArgumentException");
      } catch(IllegalArgumentException iae) { /* ok */ }
   }

   @Test
   public void testMapCoercionNested() {
      MapType mapType = new MapType(new ListType(StringType.INSTANCE));

      Map<String,List<String>> map = new HashMap<>();
      map.put("foo", Arrays.asList("bar"));
      map.put("fubar", Arrays.asList("baz"));

      assertNull(mapType.coerce(null));
      assertEquals(map, mapType.coerce(map));

      try {
         mapType.coerce("2015-02-05 00:00:00");
         fail("Expected an IllegalArgumentException");
      } catch(IllegalArgumentException iae) { /* ok */ }
   }
}


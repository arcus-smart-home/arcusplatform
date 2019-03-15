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

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class TestTypes {


	@Test
	public void testPrimitiveTypes() {
		assertEquals(Boolean.class, BooleanType.INSTANCE.getJavaType());
		assertEquals("boolean", BooleanType.INSTANCE.getTypeName());

		assertEquals(String.class, StringType.INSTANCE.getJavaType());
		assertEquals("string", StringType.INSTANCE.getTypeName());

		assertEquals(Date.class, TimestampType.INSTANCE.getJavaType());
		assertEquals("timestamp", TimestampType.INSTANCE.getTypeName());

		assertEquals(Double.class, DoubleType.INSTANCE.getJavaType());
		assertEquals("double", DoubleType.INSTANCE.getTypeName());

		assertEquals(Integer.class, IntType.INSTANCE.getJavaType());
		assertEquals("integer", IntType.INSTANCE.getTypeName());

		assertEquals(Long.class, LongType.INSTANCE.getJavaType());
		assertEquals("long", LongType.INSTANCE.getTypeName());

		assertEquals(Byte.class, ByteType.INSTANCE.getJavaType());
      assertEquals("byte", ByteType.INSTANCE.getTypeName());
	}

	@Test
	public void testComplexTypes() {

	   // var args constructor
	   EnumType enumType = new EnumType("a", "b", "c");
      assertEquals(String.class, enumType.getJavaType());
      assertEquals("enum", enumType.getTypeName());
      assertEquals(new HashSet<String>(Arrays.asList("a", "b", "c")), enumType.getValues());

		enumType = new EnumType(Arrays.asList("a", "b", "c"));
		assertEquals(String.class, enumType.getJavaType());
		assertEquals("enum", enumType.getTypeName());
		assertEquals(new HashSet<String>(Arrays.asList("a", "b", "c")), enumType.getValues());

		ListType listType1 = new ListType(StringType.INSTANCE);
		assertEquals(List.class, listType1.getJavaType());
		assertEquals("list<string>", listType1.getTypeName());
		assertEquals(StringType.INSTANCE, listType1.getContainedType());

		ListType contained = new ListType(StringType.INSTANCE);
		ListType listType2 = new ListType(contained);
		assertEquals(List.class, listType2.getJavaType());
		assertEquals("list<list<string>>", listType2.getTypeName());
		assertEquals(contained, listType2.getContainedType());

		MapType mapType1 = new MapType(BooleanType.INSTANCE);
		assertEquals(Map.class, mapType1.getJavaType());
		assertEquals("map<string,boolean>", mapType1.getTypeName());
		assertEquals(BooleanType.INSTANCE, mapType1.getContainedType());

		ListType listContained = new ListType(BooleanType.INSTANCE);
		MapType mapContained = new MapType(listContained);
		MapType mapType2 = new MapType(mapContained);
		assertEquals(Map.class, mapType2.getJavaType());
		assertEquals("map<string,map<string,list<boolean>>>", mapType2.getTypeName());
		assertEquals(mapContained, mapType2.getContainedType());

		SetType setType1 = new SetType(StringType.INSTANCE);
      assertEquals(Set.class, setType1.getJavaType());
      assertEquals("set<string>", setType1.getTypeName());
      assertEquals(StringType.INSTANCE, setType1.getContainedType());

      contained = new ListType(StringType.INSTANCE);
      SetType setType2 = new SetType(contained);
      assertEquals(Set.class, setType2.getJavaType());
      assertEquals("set<list<string>>", setType2.getTypeName());
      assertEquals(contained, setType2.getContainedType());
	}

}


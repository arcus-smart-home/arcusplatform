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
package com.iris.capability.definition;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.iris.capability.definition.AttributeType.CollectionType;
import com.iris.capability.definition.AttributeType.RawType;

public class TestCollectionAttributeType extends Assert {

   @Test
   public void testSet() {
      AttributeType type = AttributeTypes.setOf(AttributeTypes.anyType());
      assertEquals(RawType.SET, type.getRawType());
      assertEquals("set<any>", type.getRepresentation());
      assertEquals(false, type.isPrimitive());
      assertEquals(false, type.isEnum());
      assertEquals(true, type.isCollection());
      assertEquals(false, type.isObject());
      assertEquals(null, type.asEnum());
      assertEquals(type, type.asCollection());
      assertEquals(null, type.asObject());
      
      ParameterizedType javaType = (ParameterizedType) type.getJavaType();
      assertEquals(Set.class, javaType.getRawType());
      assertEquals(Object.class, javaType.getActualTypeArguments()[0]);

      CollectionType collection = type.asCollection();
      assertEquals(AttributeTypes.anyType(), collection.getContainedType());
      
      assertEquals(type, AttributeTypes.parse("set"));
   }

   @Test
   public void testList() {
      AttributeType type = AttributeTypes.listOf(AttributeTypes.anyType());
      assertEquals(RawType.LIST, type.getRawType());
      assertEquals("list<any>", type.getRepresentation());
      assertEquals(false, type.isPrimitive());
      assertEquals(false, type.isEnum());
      assertEquals(true, type.isCollection());
      assertEquals(false, type.isObject());
      assertEquals(null, type.asEnum());
      assertEquals(type, type.asCollection());
      assertEquals(null, type.asObject());
      
      ParameterizedType javaType = (ParameterizedType) type.getJavaType();
      assertEquals(List.class, javaType.getRawType());
      assertEquals(Object.class, javaType.getActualTypeArguments()[0]);

      CollectionType collection = type.asCollection();
      assertEquals(AttributeTypes.anyType(), collection.getContainedType());
      
      assertEquals(type, AttributeTypes.parse("list"));
   }

   @Test
   public void testMap() {
      AttributeType type = AttributeTypes.mapOf(AttributeTypes.anyType());
      assertEquals(RawType.MAP, type.getRawType());
      assertEquals("map<any>", type.getRepresentation());
      assertEquals(false, type.isPrimitive());
      assertEquals(false, type.isEnum());
      assertEquals(true, type.isCollection());
      assertEquals(false, type.isObject());
      assertEquals(null, type.asEnum());
      assertEquals(type, type.asCollection());
      assertEquals(null, type.asObject());
      
      ParameterizedType javaType = (ParameterizedType) type.getJavaType();
      assertEquals(Map.class, javaType.getRawType());
      assertEquals(String.class, javaType.getActualTypeArguments()[0]);
      assertEquals(Object.class, javaType.getActualTypeArguments()[1]);

      CollectionType collection = type.asCollection();
      assertEquals(AttributeTypes.anyType(), collection.getContainedType());
      
      assertEquals(type, AttributeTypes.parse("map"));
   }


}


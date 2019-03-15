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
/**
 * 
 */
package com.iris.capability.definition;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import com.iris.capability.definition.AttributeType.RawType;

/**
 * 
 */
public class TestPrimitiveAttributeType extends Assert {

   @Test
   public void testBoolean() {
      AttributeType type = AttributeTypes.booleanType();
      assertEquals(Boolean.class, type.getJavaType());
      assertEquals(RawType.BOOLEAN, type.getRawType());
      assertEquals("boolean", type.getRepresentation());
      assertEquals(true, type.isPrimitive());
      assertEquals(false, type.isEnum());
      assertEquals(false, type.isCollection());
      assertEquals(false, type.isObject());
      assertEquals(null, type.asEnum());
      assertEquals(null, type.asCollection());
      assertEquals(null, type.asObject());
      
      assertEquals(type, AttributeTypes.parse("boolean"));
   }

   @Test
   public void testByte() {
      AttributeType type = AttributeTypes.byteType();
      assertEquals(Byte.class, type.getJavaType());
      assertEquals(RawType.BYTE, type.getRawType());
      assertEquals("byte", type.getRepresentation());
      assertEquals(true, type.isPrimitive());
      assertEquals(false, type.isEnum());
      assertEquals(false, type.isCollection());
      assertEquals(false, type.isObject());
      assertEquals(null, type.asEnum());
      assertEquals(null, type.asCollection());
      assertEquals(null, type.asObject());
      
      assertEquals(type, AttributeTypes.parse("byte"));
   }

   @Test
   public void testInt() {
      AttributeType type = AttributeTypes.intType();
      assertEquals(Integer.class, type.getJavaType());
      assertEquals(RawType.INT, type.getRawType());
      assertEquals("int", type.getRepresentation());
      assertEquals(true, type.isPrimitive());
      assertEquals(false, type.isEnum());
      assertEquals(false, type.isCollection());
      assertEquals(false, type.isObject());
      assertEquals(null, type.asEnum());
      assertEquals(null, type.asCollection());
      assertEquals(null, type.asObject());
      
      assertEquals(type, AttributeTypes.parse("int"));
   }

   @Test
   public void testLong() {
      AttributeType type = AttributeTypes.longType();
      assertEquals(Long.class, type.getJavaType());
      assertEquals(RawType.LONG, type.getRawType());
      assertEquals("long", type.getRepresentation());
      assertEquals(true, type.isPrimitive());
      assertEquals(false, type.isEnum());
      assertEquals(false, type.isCollection());
      assertEquals(false, type.isObject());
      assertEquals(null, type.asEnum());
      assertEquals(null, type.asCollection());
      assertEquals(null, type.asObject());
      
      assertEquals(type, AttributeTypes.parse("long"));
   }

   @Test
   public void testDouble() {
      AttributeType type = AttributeTypes.doubleType();
      assertEquals(Double.class, type.getJavaType());
      assertEquals(RawType.DOUBLE, type.getRawType());
      assertEquals("double", type.getRepresentation());
      assertEquals(true, type.isPrimitive());
      assertEquals(false, type.isEnum());
      assertEquals(false, type.isCollection());
      assertEquals(false, type.isObject());
      assertEquals(null, type.asEnum());
      assertEquals(null, type.asCollection());
      assertEquals(null, type.asObject());
      
      assertEquals(type, AttributeTypes.parse("double"));
   }

   @Test
   public void testTimestamp() {
      AttributeType type = AttributeTypes.timestampType();
      assertEquals(Date.class, type.getJavaType());
      assertEquals(RawType.TIMESTAMP, type.getRawType());
      assertEquals("timestamp", type.getRepresentation());
      assertEquals(true, type.isPrimitive());
      assertEquals(false, type.isEnum());
      assertEquals(false, type.isCollection());
      assertEquals(false, type.isObject());
      assertEquals(null, type.asEnum());
      assertEquals(null, type.asCollection());
      assertEquals(null, type.asObject());
      
      assertEquals(type, AttributeTypes.parse("timestamp"));
   }

   @Test
   public void testString() {
      AttributeType type = AttributeTypes.stringType();
      assertEquals(String.class, type.getJavaType());
      assertEquals(RawType.STRING, type.getRawType());
      assertEquals("string", type.getRepresentation());
      assertEquals(true, type.isPrimitive());
      assertEquals(false, type.isEnum());
      assertEquals(false, type.isCollection());
      assertEquals(false, type.isObject());
      assertEquals(null, type.asEnum());
      assertEquals(null, type.asCollection());
      assertEquals(null, type.asObject());
      
      assertEquals(type, AttributeTypes.parse("string"));
   }

}


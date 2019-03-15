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

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.iris.capability.definition.AttributeType.EnumType;
import com.iris.capability.definition.AttributeType.RawType;

/**
 * 
 */
public class TestEnumAttributeType extends Assert {

   @Test
   public void testEmptyEnum() {
      List<String> enumValues = Arrays.<String>asList();
      
      AttributeType type = AttributeTypes.enumOf(enumValues);
      assertEquals(String.class, type.getJavaType());
      assertEquals(RawType.ENUM, type.getRawType());
      assertEquals("enum<>", type.getRepresentation());
      assertEquals(false, type.isPrimitive());
      assertEquals(true, type.isEnum());
      assertEquals(false, type.isCollection());
      assertEquals(false, type.isObject());
      assertEquals(type, type.asEnum());
      assertEquals(null, type.asCollection());
      assertEquals(null, type.asObject());
      
      assertTrue(enumValues.containsAll(type.asEnum().getValues()));
      assertTrue(type.asEnum().getValues().containsAll(enumValues));
      
      assertEquals(type, AttributeTypes.parse("enum"));
   }

   @Test
   public void testNonEmptyEnum() {
      List<String> enumValues = Arrays.<String>asList("alpha", "beta", "gamma");
      
      AttributeType type = AttributeTypes.enumOf(enumValues);
      assertEquals(String.class, type.getJavaType());
      assertEquals(RawType.ENUM, type.getRawType());
      assertEquals("enum<alpha,beta,gamma>", type.getRepresentation());
      assertEquals(false, type.isPrimitive());
      assertEquals(true, type.isEnum());
      assertEquals(false, type.isCollection());
      assertEquals(false, type.isObject());
      assertEquals(type, type.asEnum());
      assertEquals(null, type.asCollection());
      assertEquals(null, type.asObject());
      
      EnumType ent = (EnumType) type;
      assertEquals(1, ent.ordinal("beta"));
      
      assertTrue(enumValues.containsAll(type.asEnum().getValues()));
      assertTrue(type.asEnum().getValues().containsAll(enumValues));
      
      assertEquals(type, AttributeTypes.parse("enum<alpha,beta,gamma>"));
   }

}


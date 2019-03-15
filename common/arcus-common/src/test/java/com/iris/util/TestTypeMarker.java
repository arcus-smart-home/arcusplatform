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

import static org.junit.Assert.assertEquals;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class TestTypeMarker {

   @Test
   public void testSimpleType() {
      TypeMarker<Integer> marker = new TypeMarker<Integer>() {};
      assertEquals(Integer.class, marker.getType());
   }

   @Test
   public void testNestedType() {
      TypeMarker<List<String>> marker = new TypeMarker<List<String>>() {};
      ParameterizedType type = (ParameterizedType) marker.getType();
      assertEquals(List.class, type.getRawType());
      assertEquals(1, type.getActualTypeArguments().length);
      assertEquals(String.class, type.getActualTypeArguments()[0]);
   }

   @Test
   public void testDeeplyNestedType() {
      TypeMarker<Map<String, List<String>>> marker = new TypeMarker<Map<String, List<String>>>() {};
      ParameterizedType type = (ParameterizedType) marker.getType();
      assertEquals(Map.class, type.getRawType());
      assertEquals(2, type.getActualTypeArguments().length);
      assertEquals(String.class, type.getActualTypeArguments()[0]);
      
      ParameterizedType valueType = (ParameterizedType) type.getActualTypeArguments()[1];
      assertEquals(List.class, valueType.getRawType());
      assertEquals(1, valueType.getActualTypeArguments().length);
      assertEquals(String.class, valueType.getActualTypeArguments()[0]);
   }
   
   @Test
   public void testHelpers() {
      assertEquals(new TypeMarker<Integer>() {}, TypeMarker.wrap(Integer.class));
      assertEquals(new TypeMarker<List<String>>() {}, TypeMarker.listOf(String.class));
      assertEquals(new TypeMarker<Map<String, Integer>>() {}, TypeMarker.mapOf(Integer.class));
   }

}


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
package com.iris.model.query.expression;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;

/**
 * 
 */
public class TestConstantExpressions {
   private Model model = new SimpleModel(ImmutableMap.<String, Object>of());

   private boolean test(String expression) {
      return ExpressionCompiler.compile(expression).apply(model);
   }
   
   @Test
   public void testBoolean() {
      assertTrue(test("true"));
      assertFalse(test("false"));
   }
   
   @Test
   public void testEquals() {
      // boolean
      assertTrue(test("true == true"));
      assertFalse(test("true == false"));
      assertFalse(test("false == true"));
      assertTrue(test("false == false"));
      
      // numeric
      assertTrue(test("1 == 1"));
      assertTrue(test("0.9 == 0.9"));
      assertFalse(test("1 == 0.9"));
      
      // string
      assertTrue(test("'a value' == 'a value'"));
      assertFalse(test("'a value' == 'a different value'"));

      // mixed
      assertFalse(test("true == 'true'"));
      assertFalse(test("1 == '1'"));
      assertFalse(test("true == 1"));
      assertFalse(test("false == 0"));
   }
   
   @Test
   public void testNotEquals() {
      // boolean
      assertFalse(test("true != true"));
      assertTrue(test("true != false"));
      assertTrue(test("false != true"));
      assertFalse(test("false != false"));
      
      // numeric
      assertFalse(test("1 != 1"));
      assertFalse(test("0.9 != 0.9"));
      assertTrue(test("1 != 0.9"));
      
      // string
      assertFalse(test("'a value' != 'a value'"));
      assertTrue(test("'a value' != 'a different value'"));
      
   }
   
   @Test
   public void testLike() {
      assertTrue(test("'a value' like '.*'"));
      assertFalse(test("'a' like '[^abc]*'"));
      assertFalse(test("'xxxaxxx' like '[^abc]*'"));
      assertTrue(test("'xyz' like '[^abc]*'"));
   }

   @Test
   public void testComparison() {
      // integers
      assertTrue(test("2 > 1"));
      assertTrue(test("2 >= 1"));
      assertTrue(test("1 >= 1"));
      assertTrue(test("1 <= 1"));
      assertTrue(test("1 <= 2"));
      assertTrue(test("1 < 2"));
      assertFalse(test("1 > 2"));
      assertFalse(test("1 >= 2"));
      assertFalse(test("2 <= 1"));
      assertFalse(test("2 < 1"));

      // float
      assertTrue(test("1.1 > 0.9"));
      assertTrue(test("1.1 >= 0.9"));
      assertTrue(test("0.9 >= 0.9"));
      assertTrue(test("0.9 <= 0.9"));
      assertTrue(test("0.9 <= 1.1"));
      assertTrue(test("0.9 < 1.1"));
      assertFalse(test("0.9 > 1.1"));
      assertFalse(test("0.9 >= 1.1"));
      assertFalse(test("1.1 <= 0.9"));
      assertFalse(test("1.1 < 0.9"));

      // mixed
      assertTrue(test("1.1 > 1"));
      assertTrue(test("1.1 >= 1"));
      assertTrue(test("1.0 >= 1"));
      assertTrue(test("1.0 <= 1"));
      assertTrue(test("0.9 <= 2"));
      assertTrue(test("0.9 < 2"));
      assertFalse(test("0.9 > 2"));
      assertFalse(test("0.9 >= 2"));
      assertFalse(test("1.1 <= 1"));
      assertFalse(test("1.1 < 1"));
      assertTrue(test("2 > .9"));
      assertTrue(test("2 >= .9"));
      assertTrue(test("1 >= 1.0"));
      assertTrue(test("1 <= 1.0"));
      assertTrue(test("1 <= 1.1"));
      assertTrue(test("1 < 1.1"));
      assertFalse(test("1 > 1.1"));
      assertFalse(test("1 >= 1.1"));
      assertFalse(test("2 <= .9"));
      assertFalse(test("2 < .9"));
   }

}


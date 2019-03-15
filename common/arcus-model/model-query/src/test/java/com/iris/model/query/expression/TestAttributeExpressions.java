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

import org.junit.Test;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;

/**
 * 
 */
public class TestAttributeExpressions {

   private Predicate<Model> compile(String expression) {
      return ExpressionCompiler.compile(expression);
   }
   
   private boolean test(Predicate<Model> predicate, Object value) {
      return predicate.apply(new SimpleModel(ImmutableMap.of("test:attr", value)));
   }

   private boolean test(String expression, Object value) {
      return test(compile(expression), value);
   }

   @Test
   public void testEqualsTrue() {
      Predicate<Model> predicate = ExpressionCompiler.compile("test:attr == true");
      
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", true))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", false))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", "true"))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of())));
   }

   @Test
   public void testEqualsFalse() {
      Predicate<Model> predicate = ExpressionCompiler.compile("test:attr == false");
      
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", false))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", true))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", "false"))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of())));
   }

   @Test
   public void testEqualsNumber() {
      Predicate<Model> predicate = ExpressionCompiler.compile("test:attr == -0.9");
      
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", Double.valueOf(-0.9)))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", "-0.9"))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of())));
   }

   @Test
   public void testEqualsString() {
      Predicate<Model> predicate = ExpressionCompiler.compile("test:attr == 'a value'");
      
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", "a value"))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", "a different value"))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of())));
   }
   
   @Test
   public void testLeftHandedComparison() {
      // integers
      assertTrue(test("2 > test:attr", 1));
      assertTrue(test("2 >= test:attr", 1));
      assertTrue(test("1 >= test:attr", 1));
      assertTrue(test("1 <= test:attr", 1));
      assertTrue(test("1 <= test:attr", 2));
      assertTrue(test("1 < test:attr", 2));
      assertFalse(test("1 > test:attr", 2));
      assertFalse(test("1 >= test:attr", 2));
      assertFalse(test("2 <= test:attr", 1));
      assertFalse(test("2 < test:attr", 1));

      // float
      assertTrue(test("1.1 > test:attr", 0.9));
      assertTrue(test("1.1 >= test:attr", 0.9));
      assertTrue(test("0.9 >= test:attr", 0.9));
      assertTrue(test("0.9 <= test:attr", 0.9));
      assertTrue(test("0.9 <= test:attr", 1.1));
      assertTrue(test("0.9 < test:attr", 1.1));
      assertFalse(test("0.9 > test:attr", 1.1));
      assertFalse(test("0.9 >= test:attr", 1.1));
      assertFalse(test("1.1 <= test:attr", 0.9));
      assertFalse(test("1.1 < test:attr", 0.9));

      // mixed
      assertTrue(test("1.1 > test:attr", 1));
      assertTrue(test("1.1 >= test:attr", 1));
      assertTrue(test("1.0 >= test:attr", 1));
      assertTrue(test("1.0 <= test:attr", 1));
      assertTrue(test("0.9 <= test:attr", 2));
      assertTrue(test("0.9 < test:attr", 2));
      assertFalse(test("0.9 > test:attr", 2));
      assertFalse(test("0.9 >= test:attr", 2));
      assertFalse(test("1.1 <= test:attr", 1));
      assertFalse(test("1.1 < test:attr", 1));
      assertTrue(test("2 > test:attr", .9));
      assertTrue(test("2 >= test:attr", .9));
      assertTrue(test("1 >= test:attr", 1.0));
      assertTrue(test("1 <= test:attr", 1.0));
      assertTrue(test("1 <= test:attr", 1.1));
      assertTrue(test("1 < test:attr", 1.1));
      assertFalse(test("1 > test:attr", 1.1));
      assertFalse(test("1 >= test:attr", 1.1));
      assertFalse(test("2 <= test:attr", .9));
      assertFalse(test("2 < test:attr", .9));
   }

   @Test
   public void testRightHandedComparison() {
      // integers
      assertTrue(test("2 > test:attr", 1));
      assertTrue(test("2 >= test:attr", 1));
      assertTrue(test("1 >= test:attr", 1));
      assertTrue(test("1 <= test:attr", 1));
      assertTrue(test("1 <= test:attr", 2));
      assertTrue(test("1 < test:attr", 2));
      assertFalse(test("1 > test:attr", 2));
      assertFalse(test("1 >= test:attr", 2));
      assertFalse(test("2 <= test:attr", 1));
      assertFalse(test("2 < test:attr", 1));

      // float
      assertTrue(test("1.1 > test:attr", 0.9));
      assertTrue(test("1.1 >= test:attr", 0.9));
      assertTrue(test("0.9 >= test:attr", 0.9));
      assertTrue(test("0.9 <= test:attr", 0.9));
      assertTrue(test("0.9 <= test:attr", 1.1));
      assertTrue(test("0.9 < test:attr", 1.1));
      assertFalse(test("0.9 > test:attr", 1.1));
      assertFalse(test("0.9 >= test:attr", 1.1));
      assertFalse(test("1.1 <= test:attr", 0.9));
      assertFalse(test("1.1 < test:attr", 0.9));

      // mixed
      assertTrue(test("1.1 > test:attr", 1));
      assertTrue(test("1.1 >= test:attr", 1));
      assertTrue(test("1.0 >= test:attr", 1));
      assertTrue(test("1.0 <= test:attr", 1));
      assertTrue(test("0.9 <= test:attr", 2));
      assertTrue(test("0.9 < test:attr", 2));
      assertFalse(test("0.9 > test:attr", 2));
      assertFalse(test("0.9 >= test:attr", 2));
      assertFalse(test("1.1 <= test:attr", 1));
      assertFalse(test("1.1 < test:attr", 1));
      assertTrue(test("2 > test:attr", .9));
      assertTrue(test("2 >= test:attr", .9));
      assertTrue(test("1 >= test:attr", 1.0));
      assertTrue(test("1 <= test:attr", 1.0));
      assertTrue(test("1 <= test:attr", 1.1));
      assertTrue(test("1 < test:attr", 1.1));
      assertFalse(test("1 > test:attr", 1.1));
      assertFalse(test("1 >= test:attr", 1.1));
      assertFalse(test("2 <= test:attr", .9));
      assertFalse(test("2 < test:attr", .9));
   }

   @Test
   public void testContainsTrue() {
      Predicate<Model> predicate = ExpressionCompiler.compile("test:attr contains true");
      
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", ImmutableSet.of(true)))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", ImmutableSet.of(false)))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", ImmutableSet.of("true")))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", true))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of())));
   }

   @Test
   public void testContainsFalse() {
      Predicate<Model> predicate = ExpressionCompiler.compile("test:attr contains false");
      
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", ImmutableSet.of(false)))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", ImmutableSet.of(true)))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", ImmutableSet.of("false")))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", false))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of())));
   }

   @Test
   public void testContainsNumber() {
      Predicate<Model> predicate = ExpressionCompiler.compile("test:attr contains -0.9");
      
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", ImmutableSet.of(-0.9)))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", ImmutableSet.of()))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", -0.9))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of())));
   }

   @Test
   public void testContainsString() {
      Predicate<Model> predicate = ExpressionCompiler.compile("test:attr contains 'a value'");
      
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", ImmutableSet.of("a value")))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", ImmutableSet.of("a different value")))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", "a value"))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of())));
   }

   @Test
   public void testLikeAny() {
      Predicate<Model> predicate = ExpressionCompiler.compile("test:attr like '.*'");
      
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", "a value"))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", true))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", -0.9))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of())));
   }

   @Test
   public void testLikeComplex() {
      Predicate<Model> predicate = ExpressionCompiler.compile("test:attr like '[^abc]*'");
      
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", "a"))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", "xxxaxxx"))));
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", "xyz"))));
   }

   @Test
   public void testIsSupported() {
      Predicate<Model> predicate = ExpressionCompiler.compile("test:attr is supported");
      
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", true))));
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", -0.9))));
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", "a string"))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of())));
   }

}


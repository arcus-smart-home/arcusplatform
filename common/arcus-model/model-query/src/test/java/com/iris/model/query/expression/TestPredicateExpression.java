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
package com.iris.model.query.expression;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;

public class TestPredicateExpression {

   @Test
   public void testAddress() {
      Predicate<Model> predicate = ExpressionCompiler.compile("base:address == 'DRIV:dev:616b21fb-4db5-4727-b8fd-cafc4adedb0f'");      
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("base:address", "DRIV:dev:616b21fb-4db5-4727-b8fd-cafc4adedb0f", "swit:state", "ON"))));
   }
   
   @Test
   public void testDeviceAndAttribute() {
      Predicate<Model> predicate = ExpressionCompiler.compile("base:address == 'DRIV:dev:616b21fb-4db5-4727-b8fd-cafc4adedb0f' and swit:state == 'ON'");
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("base:address", "DRIV:dev:616b21fb-4db5-4727-b8fd-cafc4adedb0f", "swit:state", "ON"))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("base:address", "DRIV:dev:616b21fb-4db5-4727-b8fd-cafc4adedb0f", "swit:state", "OFF"))));
      
      predicate = ExpressionCompiler.compile("base:address == 'DRIV:dev:616b21fb-4db5-4727-b8fd-cafc4adedb0f' and swit:state != 'ON'");
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("base:address", "DRIV:dev:616b21fb-4db5-4727-b8fd-cafc4adedb0f", "swit:state", "ON"))));
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("base:address", "DRIV:dev:616b21fb-4db5-4727-b8fd-cafc4adedb0f", "swit:state", "OFF"))));
   }

   @Test
   public void testNotSupported() {
      Predicate<Model> predicate = ExpressionCompiler.compile("!(test:attr is supported)");
      
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", true))));
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of())));
   }
   
   @Test
   public void testNotEqualsOperator() {
      Predicate<Model> predicate = ExpressionCompiler.compile("test:attr != 'value'");
      
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", "value"))));
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of())));
   }
   
   @Test
   public void testNotNotEqualsOperator() {
      Predicate<Model> predicate = ExpressionCompiler.compile("not test:attr != 'value'");
      
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", "value"))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of())));
   }

   @Test
   public void testGreaterThanOperator() {
      Predicate<Model> predicate = ExpressionCompiler.compile("test:attr > 5");
      
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", 6.0))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", 5.0))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", 4.0))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of())));
   }

   @Test
   public void testGreaterThanEqualToOperator() {
      Predicate<Model> predicate = ExpressionCompiler.compile("test:attr >= 5");
      
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", 6.0))));
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", 5.0))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", 4.0))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of())));
   }

   @Test
   public void testLessThanOperator() {
      Predicate<Model> predicate = ExpressionCompiler.compile("test:attr < 5");
      
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", 6.0))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", 5.0))));
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", 4.0))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of())));
   }

   @Test
   public void testLessThanEqualToOperator() {
      Predicate<Model> predicate = ExpressionCompiler.compile("test:attr <= 5");
      
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", 6.0))));
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", 5.0))));
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", 4.0))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of())));
   }
   
   @Test
   public void testNotEquals() {
      Predicate<Model> predicate = ExpressionCompiler.compile("not test:attr == 'value'");
      
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:attr", "value"))));
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of())));
   }

   @Test
   public void testAndNotSupported() {
      Predicate<Model> predicate = ExpressionCompiler.compile("test:a == 'value' and not test:b is supported");
      
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:a", "value"))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:a", "value", "test:b", "bar"))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of())));
   }

   @Test
   public void testNotSupportedAnd() {
      Predicate<Model> predicate = ExpressionCompiler.compile("(not (test:a is supported)) and test:b == 'value'");
      
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:b", "value"))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:a", "foo", "test:b", "value"))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of())));
   }

   @Test
   public void testOperatorCaseInsensitivity() {
      Predicate<Model> predicate = ExpressionCompiler.compile("test:a EQ 'value' OR (NOT (test:a is supported) AND (test:b > 0))");
      
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:a", "value"))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:a", "foo", "test:b", 1))));
      assertTrue(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of("test:b", 1))));
      assertFalse(predicate.apply(new SimpleModel(ImmutableMap.<String, Object>of())));
   }

   @Test
   public void testUngroupedAnd() {
      Predicate<Model> predicate = ExpressionCompiler.compile("test:a == 'a' and test:b == 'b' and test:c == 'c'");
      
      assertTrue(predicate.apply(new SimpleModel(
            ImmutableMap.<String, Object>of("test:a", "a", "test:b", "b", "test:c", "c")
      )));
      assertFalse(predicate.apply(new SimpleModel(
            ImmutableMap.<String, Object>of("test:a", "a", "test:b", "b")
      )));
   }

   @Test
   public void testUngroupedOr() {
      Predicate<Model> predicate = ExpressionCompiler.compile("test:a == 'a' or test:b == 'b' or test:c == 'c'");
      
      assertTrue(predicate.apply(new SimpleModel(
            ImmutableMap.<String, Object>of("test:a", "a")
      )));
      assertTrue(predicate.apply(new SimpleModel(
            ImmutableMap.<String, Object>of("test:b", "b")
      )));
      assertTrue(predicate.apply(new SimpleModel(
            ImmutableMap.<String, Object>of("test:c", "c")
      )));
      assertFalse(predicate.apply(new SimpleModel(
            ImmutableMap.<String, Object>of("test:a", "c", "test:b", "a", "test:c", "d")
      )));
   }

   @Test
   public void testGrouped1() {
      Predicate<Model> predicate = ExpressionCompiler.compile("(test:a == 'a' and test:b == 'b') or (test:a == '1' and test:b == '2')");
      
      assertTrue(predicate.apply(new SimpleModel(
            ImmutableMap.<String, Object>of("test:a", "a", "test:b", "b")
      )));
      assertTrue(predicate.apply(new SimpleModel(
            ImmutableMap.<String, Object>of("test:a", "1", "test:b", "2")
      )));
      assertFalse(predicate.apply(new SimpleModel(
            ImmutableMap.<String, Object>of("test:a", "a", "test:b", "2")
      )));
   }

   @Test
   public void testGrouped1WithWhitespace() {
      Predicate<Model> predicate = ExpressionCompiler.compile(" \t \r\n \n ( \t \r\n \n test:a \t \r\n \n == \t \r\n \n 'a' \t \r\n \n and \t \r\n \n test:b \t \r\n \n == \t \r\n \n 'b' \t \r\n \n ) \t \r\n \n or \t \r\n \n ( \t \r\n \n test:a \t \r\n \n == \t \r\n \n '1' \t \r\n \n and \t \r\n \n test:b \t \r\n \n == \t \r\n \n '2' \t \r\n \n ) \t \r\n \n ");
      
      assertTrue(predicate.apply(new SimpleModel(
            ImmutableMap.<String, Object>of("test:a", "a", "test:b", "b")
      )));
      assertTrue(predicate.apply(new SimpleModel(
            ImmutableMap.<String, Object>of("test:a", "1", "test:b", "2")
      )));
      assertFalse(predicate.apply(new SimpleModel(
            ImmutableMap.<String, Object>of("test:a", "a", "test:b", "2")
      )));
   }

}


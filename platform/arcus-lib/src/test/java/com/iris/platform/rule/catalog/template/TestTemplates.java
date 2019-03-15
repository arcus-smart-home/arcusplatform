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
package com.iris.platform.rule.catalog.template;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.address.Address;

public class TestTemplates {
   private final static String ADDRESS_STRING = "DRIV:dev:616b21fb-4db5-4727-b8fd-cafc4adedb0f";
   private final static String TED = "ted";
   private final static Address ADDRESS = Address.fromString(ADDRESS_STRING);
   private final static Integer ONE = 1;
   
   
   private static Map<String, Object> variables = ImmutableMap.of(
            "bill", TED,
            "address", ADDRESS,
            "one", ONE
         );
   
   @Test
   public void testOnlyValueString() {
      TemplatedValue<Object> value = TemplatedValue.parse("${bill}");
      Assert.assertEquals(TED, value.apply(variables));
   }
   
   @Test
   public void testOnlyValueAddress() {
      TemplatedValue<Object> value = TemplatedValue.parse("${address}");
      Assert.assertEquals(ADDRESS, value.apply(variables));
   }
   
   @Test
   public void testOnlyValueInteger() {
      TemplatedValue<Object> value = TemplatedValue.parse("${address}");
      Assert.assertEquals(ADDRESS, value.apply(variables));
   }
   
   @Test 
   public void testStringValueInString() {
      TemplatedValue<Object> value = TemplatedValue.parse("That is bogus ${bill}");
      Assert.assertEquals("That is bogus ted", value.apply(variables));
   }
   
   @Test
   public void testIntValueInString() {
      TemplatedValue<Object> value = TemplatedValue.parse("This is the sound of ${one} voice");
      Assert.assertEquals("This is the sound of 1 voice", value.apply(variables));
   }
   
   @Test
   public void testAddressValueInString() {
      TemplatedValue<Object> value = TemplatedValue.parse("${address} is the address");
      Assert.assertEquals(ADDRESS_STRING + " is the address", value.apply(variables));
   }
   
   @Test
   public void testMultipleValuesInString() {
      TemplatedValue<Object> value = TemplatedValue.parse("There is ${one} device named ${bill} at ${address}");
      Assert.assertEquals(
            "There is 1 device named ted at " + ADDRESS_STRING, value.apply(variables));
   }
}


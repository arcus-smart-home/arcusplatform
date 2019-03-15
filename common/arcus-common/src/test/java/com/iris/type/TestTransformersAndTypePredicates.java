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
package com.iris.type;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.iris.messages.address.Address;

import static com.iris.type.TypeFixtures.*;

@RunWith(Parameterized.class)
public class TestTransformersAndTypePredicates extends TypeCoercerTestCase {
   
   @Parameters(name="{index}:{3}")
   public static Collection<Object[]> cases() {
      return Arrays.asList(new Object[][] {
            { Address.class, TEST_ADDRESS, TEST_ADDRESS, "Address -> Address"},
            { Address.class, TEST_ADDRESS, TEST_STRING_ADDRESS, "String -> Address" },
            { Boolean.class, Boolean.TRUE, TEST_STRING_BOOL_TRUE, "String -> Bool" },
            { Boolean.class, Boolean.FALSE, TEST_STRING_BOOL_FALSE, "String -> Bool" },
            { Boolean.class, Boolean.TRUE, TEST_INT_BOOL_TRUE, "Int -> Bool"},
            { Boolean.class, Boolean.FALSE, TEST_INT_BOOL_FALSE, "Int -> Bool"},
            { Boolean.class, Boolean.TRUE, TEST_LONG_BOOL_TRUE, "Long -> Bool"},
            { Boolean.class, Boolean.FALSE, TEST_LONG_BOOL_FALSE, "Long -> Bool" },
            { Boolean.class, Boolean.TRUE, TEST_DOUBLE_BOOL_TRUE, "Double -> Bool"},
            { Boolean.class, Boolean.FALSE, TEST_DOUBLE_BOOL_FALSE, "Double -> Bool"},
            { Boolean.class, Boolean.TRUE, TEST_BYTE_BOOL_TRUE, "Byte -> Bool"},
            { Boolean.class, Boolean.FALSE, TEST_BYTE_BOOL_FALSE, "Byte -> Bool"},
            { Boolean.class, Boolean.TRUE, TEST_STRING_BOOL_TRUE, "String -> Bool"},
            { Boolean.class, Boolean.FALSE, TEST_STRING_BOOL_FALSE, "String -> Bool"},
            { Boolean.class, Boolean.TRUE, Boolean.TRUE, "Bool -> Bool"},
            { Boolean.class, Boolean.FALSE, Boolean.FALSE, "Bool -> Bool"},
            { Byte.class, TEST_BYTE_BYTE, TEST_BYTE_BYTE, "Byte -> Byte"},
            { Byte.class, TEST_BYTE_BYTE, TEST_INT_BYTE, "Int -> Byte"},
            { Byte.class, TEST_BYTE_BYTE, TEST_LONG_BYTE, "Long -> Byte"},
            { Byte.class, TEST_BYTE_BYTE, TEST_DOUBLE_BYTE, "Double -> Byte"},
            { Byte.class, TEST_BYTE_BYTE, TEST_STRING_BYTE, "String -> Byte"},
            { Byte.class, TEST_BYTE_NEG, TEST_STRING_BYTE_NEG, "String Negative -> Byte"},
            { Byte.class, TEST_BYTE_BYTE, TEST_STRING_BYTE_HEX, "String Hex -> Byte"},
            { Date.class, TEST_DATE_DATE, new Date(TEST_DATE_DATE.getTime()), "Date -> Date"},
            { Date.class, TEST_DATE_DATE, TEST_LONG_DATE, "Long -> Date"},
            { Date.class, TEST_DATE_DATE, TEST_DOUBLE_DATE, "Double -> Date"},
            { Date.class, TEST_DATE_DATE, TEST_CAL_DATE, "Calendar -> Date"},
            { Date.class, TEST_DATE_DATE, TEST_STRING_DATE, "String -> Date"},
            { Double.class, TEST_DOUBLE_DOUBLE, TEST_DOUBLE_DOUBLE, "Double -> Double"},
            { Double.class, TEST_DOUBLE_DOUBLE, TEST_INT_DOUBLE, "Int -> Double"},
            { Double.class, TEST_DOUBLE_STRING, TEST_STRING_DOUBLE, "String -> Double"},
            { Double.class, TEST_DOUBLE_DOUBLE, TEST_LONG_DOUBLE, "Long -> Double"},
            { Double.class, TEST_DOUBLE_DOUBLE, TEST_BYTE_DOUBLE, "Byte -> Double"},
            { Integer.class, TEST_INT_INT, TEST_INT_INT, "Int -> Int" },
            { Integer.class, TEST_INT_INT, TEST_LONG_INT, "Long -> Int" },
            { Integer.class, TEST_INT_INT, TEST_DOUBLE_INT, "Double -> Int" },
            { Integer.class, TEST_INT_BYTE, TEST_BYTE_BYTE, "Byte -> Int" },
            { Integer.class, TEST_INT_INT, TEST_STRING_INT, "String -> Int" },
            { Long.class, TEST_LONG_LONG, TEST_LONG_LONG, "Long -> Long" },
            { Long.class, TEST_LONG_LONG, TEST_DOUBLE_LONG, "Double -> Long" },
            { Long.class, TEST_LONG_LONG, TEST_STRING_LONG, "String -> Long" },
            { Long.class, TEST_LONG_INT, TEST_INT_INT, "Int -> Long" },
            { Long.class, TEST_LONG_BYTE, TEST_BYTE_BYTE, "Byte -> Long" },
            { String.class, TEST_STRING_STRING, TEST_STRING_STRING, "String -> String" },
            { String.class, TEST_STRING_LONG, TEST_LONG_LONG, "Long -> String" },
            { String.class, TEST_STRING_DOUBLE, TEST_DOUBLE_STRING, "Double -> String" },
            { String.class, TEST_STRING_INT, TEST_INT_INT, "String -> String" }     
      });
   }
   
   private Class<?> target;
   private Object expected;
   private Object test; 
   
   public TestTransformersAndTypePredicates(Class<?> target, Object expected, Object test, String name) {
      this.target = target;
      this.expected = expected;
      this.test = test;
   }

   @Test
   public void testTransformer() {
      System.out.println("Test transformer " + test.getClass().getName() + " to " + expected.getClass().getName());
      Object converted = typeCoercer.createTransformer(target).apply(test);
      Assert.assertEquals(expected, converted);
   }
   
   @Test
   public void testSupportedPredicate() {
      System.out.println("Test supported predicate " + test.getClass().getName() + " to " + expected.getClass().getName());
      boolean supported = typeCoercer.createSupportedTypePredicate(target).apply(test.getClass());
      Assert.assertTrue(supported);
   }
}


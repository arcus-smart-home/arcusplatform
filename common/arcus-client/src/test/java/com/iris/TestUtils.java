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
package com.iris;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 *
 */
public class TestUtils {

   @Test
   public void testIsNamespaced() throws Exception {
      assertFalse(Utils.isNamespaced(null));
      assertFalse(Utils.isNamespaced(""));
      assertFalse(Utils.isNamespaced("name"));
      assertFalse(Utils.isNamespaced("nom nom nom"));
      assertTrue(Utils.isNamespaced("a:b"));
      assertTrue(Utils.isNamespaced("a:b:c"));
   }

   @Test
   public void testGetNamespace() throws Exception {
      try {
         Utils.getNamespace(null);
         fail("Retrieved a null namespace");
      }
      catch(IllegalArgumentException e) {
         // expected
      }
      try {
         Utils.getNamespace("notnamespaced");
         fail("Retrieved an empty namespace");
      }
      catch(IllegalArgumentException e) {
         // expected
      }
      assertEquals("a", Utils.getNamespace("a:b"));
      assertEquals("a", Utils.getNamespace("a:b:c"));
   }

   @Test
   public void testNamespace() throws Exception {
      try {
         Utils.namespace(null, null);
         fail("Created a null namespace");
      }
      catch(IllegalArgumentException e) {
         // expected
      }
      try {
         Utils.namespace("namespace", null);
         fail("Created a null namespace");
      }
      catch(IllegalArgumentException e) {
         // expected
      }
      try {
         Utils.namespace(null, "name");
         fail("Created a null namespace");
      }
      catch(IllegalArgumentException e) {
         // expected
      }
      assertEquals("a:b", Utils.namespace("a", "b"));
      assertEquals("a:b:c", Utils.namespace("a", "b:c"));
      try {
         Utils.namespace("a:b", "c");
         fail("Allowed invalid namespace");
      }
      catch(IllegalArgumentException e) {
         // expected
      }
   }

   @Test
   public void testAssertFalse() throws Exception {
      Utils.assertFalse(false);
      try {
         Utils.assertFalse(true);
         fail();
      }
      catch(IllegalArgumentException e) {
         // expected
         assertNotNull(e.getMessage());
      }
   }

   @Test
   public void testAssertFalse_pass() throws Exception {
      TestObject to = new TestObject();
      Utils.assertFalse(false, to);
      assertFalse(to.wasToStringInvoked());
   }

   @Test
   public void testAssertFalse_fail() throws Exception {
      TestObject to = new TestObject();
      try {
         Utils.assertFalse(true, to);
         fail();
      }
      catch(IllegalArgumentException e) {
         assertEquals(to.getMessage(), e.getMessage());
      }
      assertTrue(to.wasToStringInvoked());
   }

   @Test
   public void testAssertTrue() throws Exception {
      Utils.assertTrue(true);
      try {
         Utils.assertTrue(false);
         fail();
      }
      catch(IllegalArgumentException e) {
         // expected
         assertNotNull(e.getMessage());
      }
   }

   @Test
   public void testAssertTrue_pass() throws Exception {
      TestObject to = new TestObject();
      Utils.assertTrue(true, to);
      assertFalse(to.wasToStringInvoked());
   }

   @Test
   public void testAssertTrue_fail() throws Exception {
      TestObject to = new TestObject();
      try {
         Utils.assertTrue(false, to);
         fail();
      }
      catch(IllegalArgumentException e) {
         assertEquals(to.getMessage(), e.getMessage());
      }
      assertTrue(to.wasToStringInvoked());
   }

   @Test
   public void testAssertNotNull() throws Exception {
      Utils.assertNotNull(new Object());
      try {
         Utils.assertNotNull(null);
         fail();
      }
      catch(IllegalArgumentException e) {
         // expected
         assertNotNull(e.getMessage());
      }
   }

   @Test
   public void testAssertNotNull_pass() throws Exception {
      TestObject to = new TestObject();
      Utils.assertNotNull(new Object(), to);
      assertFalse(to.wasToStringInvoked());
   }

   @Test
   public void testAssertNotNull_fail() throws Exception {
      TestObject to = new TestObject();
      try {
         Utils.assertNotNull(null, to);
         fail();
      }
      catch(IllegalArgumentException e) {
         assertEquals(to.getMessage(), e.getMessage());
      }
      assertTrue(to.wasToStringInvoked());
   }

   @Test
   public void testUnmodifiableCopy() throws Exception {
      assertEquals(Collections.emptyMap(), Utils.unmodifiableCopy(null));
      assertEquals(Collections.emptyMap(), Utils.unmodifiableCopy(new HashMap<>()));

      Map<String, String> map = new HashMap<String, String>();
      map.put("a", "1");
      map.put("b", "2");
      Map<String, String> copy = Utils.unmodifiableCopy(map);
      assertEquals(map, copy);
      map.put("c", "3");
      assertFalse(map.equals(copy));
      assertFalse(copy.containsKey("c"));
   }

   @Test
   public void testBase64Decode() {
      System.out.println(Arrays.toString(Utils.b64Decode("AQAAAAceYgIAAAAA")));
   }

   static class TestObject {
      private boolean toStringInvoked = false;
      private String message = "test message";

      public boolean wasToStringInvoked() {
         return toStringInvoked;
      }

      public String getMessage() {
         return message;
      }

      public void setMessage(String message) {
         this.message = message;
      }

      @Override
      public String toString() {
         toStringInvoked = true;
         return message;
      }
   }

}


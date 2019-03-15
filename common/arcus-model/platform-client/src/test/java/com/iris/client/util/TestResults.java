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
package com.iris.client.util;

import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 */
public class TestResults extends Assert {

   @Test
   public void testValue() throws Exception {
      Result<String> value = Results.fromValue("test");
      
      assertTrue(value.isValue());
      assertFalse(value.isError());
      assertEquals("test", value.getValue());
      assertEquals(null, value.getError());
      assertEquals("test", value.get());
   }

   @Test
   public void testNullValue() throws Exception {
      Result<String> value = Results.fromValue(null);
      
      assertTrue(value.isValue());
      assertFalse(value.isError());
      assertEquals(null, value.getValue());
      assertEquals(null, value.getError());
      assertEquals(null, value.get());
   }

   @Test
   public void testException() throws Exception {
      RuntimeException ex = new RuntimeException();
      Result<String> value = Results.fromError(ex);
      
      assertFalse(value.isValue());
      assertTrue(value.isError());
      assertEquals(null, value.getValue());
      assertEquals(ex, value.getError());
      try {
         value.get();
         fail();
      }
      catch(ExecutionException e) {
         assertEquals(ex, e.getCause());
      }
   }

}


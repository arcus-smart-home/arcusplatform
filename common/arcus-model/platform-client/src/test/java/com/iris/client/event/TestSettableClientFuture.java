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
package com.iris.client.event;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;

import com.iris.client.util.Results;

public class TestSettableClientFuture extends Assert {

   @Test
   public void testNoValueSet() throws Exception {
      SettableClientFuture<Boolean> future = new SettableClientFuture<>();
      assertFalse(future.isCancelled());
      assertFalse(future.isDone());
      assertFalse(future.isError());
      
      try {
         future.get(0, TimeUnit.MILLISECONDS);
      }
      catch(TimeoutException e) {
         // expected
      }
      
      try {
         future.getResult();
      }
      catch(IllegalStateException e) {
         // expected
      }
   }

   @Test
   public void testValueSet() throws Exception {
      SettableClientFuture<Boolean> future = new SettableClientFuture<>();
      future.setValue(true);;
      
      assertFalse(future.isCancelled());
      assertTrue(future.isDone());
      assertFalse(future.isError());
      
      assertTrue(future.get(0, TimeUnit.MILLISECONDS));
      assertEquals(Results.fromValue(true), future.getResult());
   }

   @Test
   public void testValueErrored() throws Exception {
      RuntimeException e = new RuntimeException("error");
      SettableClientFuture<Boolean> future = new SettableClientFuture<>();
      future.setError(e);
      
      assertFalse(future.isCancelled());
      assertTrue(future.isDone());
      assertTrue(future.isError());
      
      try {
         assertTrue(future.get(0, TimeUnit.MILLISECONDS));
      }
      catch(ExecutionException ee) {
         assertEquals(e, ee.getCause());
      }
      assertEquals(Results.<Boolean>fromError(e), future.getResult());
   }

   @Test
   public void testValueCancelled() throws Exception {
      RuntimeException e = new RuntimeException("error");
      SettableClientFuture<Boolean> future = new SettableClientFuture<>();
      future.cancel(true);
      
      assertTrue(future.isCancelled());
      assertTrue(future.isDone());
      assertTrue(future.isError());
      
      try {
         assertTrue(future.get(0, TimeUnit.MILLISECONDS));
      }
      catch(CancellationException ce) {
         // expected
      }
      assertTrue(future.getResult().isError());
   }

}


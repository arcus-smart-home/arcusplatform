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
package com.iris.oculus.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mockito.Mockito;

import com.iris.client.event.ListenerRegistration;

/**
 * 
 */
public class TestCallbackRegistration {
   private CallbackRegistration<TestInterface> uut = new CallbackRegistration<>(TestInterface.class);
   
   @Test
   public void testEmpty() {
      // no-ops
      uut.delegate().noargs();
      uut.delegate().someargs("test", true);
      assertNotNull(uut.delegate().toString());
      assertTrue(uut.delegate().equals(uut.delegate()));
   }
   
   @Test
   public void testMockThrows() {
      TestInterface mock1 = Mockito.mock(TestInterface.class);
      TestInterface mock2 = Mockito.mock(TestInterface.class);
      mock1.noargs();
      Mockito.doThrow(new RuntimeException("BOOM!"));
      
      uut.register(mock1);
      uut.register(mock2);
      
      uut.delegate().noargs();
      uut.delegate().someargs("test", true);
      
      Mockito.verify(mock1).noargs();
      Mockito.verify(mock1).someargs("test", true);
      Mockito.verify(mock2).noargs();
      Mockito.verify(mock2).someargs("test", true);
   }
   
   @Test
   public void testUnregisterMock() {
      TestInterface mock = Mockito.mock(TestInterface.class);
      
      uut.register(mock).remove();
      
      uut.delegate().noargs();
      uut.delegate().someargs("test", false);
      
      Mockito.verifyZeroInteractions(mock);
   }
   
   public interface TestInterface {
      void noargs();
      
      void someargs(String arg1, boolean arg2);
      
   }
}


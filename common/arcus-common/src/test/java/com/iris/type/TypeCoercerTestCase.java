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

import org.junit.Assert;
import org.junit.Before;

public class TypeCoercerTestCase {
   protected TypeCoercer typeCoercer;

   @Before
   public void setUp() {
      typeCoercer = new TypeCoercerImpl();
   }
   
   protected void testUnsupported(Class<?> target, Object test) {
      Assert.assertFalse(msgNotSupported(target, test), typeCoercer.isCoercible(target, test));
      Assert.assertFalse(msgNotSupported(target, test), typeCoercer.isSupportedType(target, test.getClass()));
      try {
         typeCoercer.coerce(target, test);
      }
      catch(IllegalArgumentException iae) {
         // Expected Behavior
         return;
      }
      Assert.fail("Should have thrown an IllegalArgumentException instead.");
   }
   
   protected void testSupported(Class<?> target, Object expected, Object test) {
      Assert.assertTrue(msgSupported(target, test), typeCoercer.isCoercible(target, test));
      Assert.assertTrue(msgSupported(target, test), typeCoercer.isSupportedType(target, test.getClass()));
      Object converted = typeCoercer.coerce(target, test);
      Assert.assertEquals(expected, converted);
   }
   
   private String msgSupported(Class<?> target, Object test) {
      return test.getClass().getName() + " should be coerible to " + target.getName();
   }
   
   private String msgNotSupported(Class<?> target, Object test) {
      return test.getClass().getName() + " should not be coerible to " + target.getName();
   }
   
}


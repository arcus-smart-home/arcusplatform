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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.iris.messages.address.Address;

import static com.iris.type.TypeFixtures.*;

@RunWith(Parameterized.class)
public class TestUnsupportedTypeCoercer extends TypeCoercerTestCase {

   @Parameters(name="{index}:{2}")
   public static Collection<Object[]> cases() {
      return Arrays.asList(new Object[][] {
            { Address.class, TEST_INT_INT, "Int -> Address" },
            { Boolean.class, TEST_DATE_DATE, "Date -> Bool" },
            { Byte.class, TEST_CAL_DATE, "Calendar -> Byte" },
            { Date.class, Boolean.TRUE, "Bool -> Date" },
            { Double.class, TEST_ADDRESS, "Address -> Double" },
            { Integer.class, TEST_CAL_DATE, "Calendar -> Int" },
            { Long.class, TEST_DATE_DATE, "Date -> Long" }
      });
   }
   
   private Class<?> target;
   private Object test;
   
   public TestUnsupportedTypeCoercer(Class<?> target, Object test, String name) {
      this.target = target;
      this.test = test;
   }
   
   @Test
   public void testUnsupportedCoerce() throws Exception {
      System.out.println("Test coerce unsupported class " + test.getClass().getName() + " to " + target.getName());
      testUnsupported(target, test);
   }
}


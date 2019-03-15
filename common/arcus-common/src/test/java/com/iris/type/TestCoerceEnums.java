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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class TestCoerceEnums extends TypeCoercerTestCase {
   public enum Stuff { FISH, APPLE, BIKE, BEAR }
   
   private static final String[] testArray = new String[] { "apple", "Apple", "fish", "BEAR" };
   
   @Test
   public void testStuffEnum() {
      Stuff stuff = typeCoercer.coerce(Stuff.class, Stuff.FISH);
      Assert.assertEquals(Stuff.FISH, stuff);
      
      stuff = typeCoercer.coerce(Stuff.class, "APPLE");
      Assert.assertEquals(Stuff.APPLE, stuff);
      
      stuff = typeCoercer.coerce(Stuff.class, "bike");
      Assert.assertEquals(Stuff.BIKE, stuff);
      
      stuff = typeCoercer.coerce(Stuff.class, "Bear");
      Assert.assertEquals(Stuff.BEAR, stuff);
      
      List<Stuff> stuffs = typeCoercer.coerceList(Stuff.class, testArray);
      Assert.assertEquals(Stuff.APPLE, stuffs.get(0));
      Assert.assertEquals(Stuff.APPLE, stuffs.get(1));
      Assert.assertEquals(Stuff.FISH, stuffs.get(2));
      Assert.assertEquals(Stuff.BEAR, stuffs.get(3));
   }
}


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
package com.iris.common.rule.time;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestTimeRangeFromString {
   private static int[] START = new int[] {4,8,15};
   private static int[] END = new int[] {16,23,42};
   
   @Parameters(name="{index}:{0}")
   public static Collection<Object[]> cases() {
      return Arrays.asList(new Object[][] {
            { "4:08:15-16:23:42", START, END },
            { "04:08:15-16:23:42", START, END },
            { "4:08:15 - 16:23:42", START, END },
            { "4:08:15  -16:23:42", START, END },
            { " 4:08:15 - 16:23:42 ", START, END },
            { "-16:23:42", null, END },
            { "4:08:15-", START, null },
            { "4:08:15 - ", START, null },
            { " - 16:23:42", null, END }
      });
   }
   
   private int[] start;
   private int[] end;
   private String input;
   
   public TestTimeRangeFromString(String input, int[] start, int[] end) {
      this.input = input;
      this.start = start;
      this.end = end;
   }
   
   @Test
   public void testFromString() throws Exception {
      TimeRange range = TimeRange.fromString(input);
      verify(range);
      TimeRange range2 = TimeRange.fromString(range.getRepresentation());
      verify(range2);
   }
   
   private void verify(TimeRange range) {
      if (start == null) {
         Assert.assertNull(range.getStart());
      }
      else {
         Assert.assertEquals(start[0], range.getStart().getHours());
         Assert.assertEquals(start[1], range.getStart().getMinutes());
         Assert.assertEquals(start[2], range.getStart().getSeconds());
      }
      if (end == null) {
         Assert.assertNull(range.getEnd());
      }
      else {
         Assert.assertEquals(end[0], range.getEnd().getHours());
         Assert.assertEquals(end[1], range.getEnd().getMinutes());
         Assert.assertEquals(end[2], range.getEnd().getSeconds());
      }
   }
}


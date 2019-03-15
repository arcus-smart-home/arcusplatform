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
package com.iris.util;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class TestTimeUtil
{
   @Test
   public void toFriendlyDuration_0_seconds()
   {
      toFriendlyDuration(0, "0 seconds");
   }

   @Test
   public void toFriendlyDuration_1_second()
   {
      toFriendlyDuration(1, "1 second");
   }

   @Test
   public void toFriendlyDuration_1001_milliseconds()
   {
      toFriendlyDuration(1001, MILLISECONDS, "1 second");
   }

   @Test
   public void toFriendlyDuration_2_seconds()
   {
      toFriendlyDuration(2, "2 seconds");
   }

   @Test
   public void toFriendlyDuration_1_minute()
   {
      toFriendlyDuration(60, "1 minute");
   }

   @Test
   public void toFriendlyDuration_1_minute_1_second()
   {
      toFriendlyDuration(61, "1 minute 1 second");
   }

   @Test
   public void toFriendlyDuration_1_minute_2_seconds()
   {
      toFriendlyDuration(62, "1 minute 2 seconds");
   }

   @Test
   public void toFriendlyDuration_2_minutes()
   {
      toFriendlyDuration(120, "2 minutes");
   }

   @Test
   public void toFriendlyDuration_60_weeks_4_days_5_hours_6_minutes_7_seconds()
   {
      toFriendlyDuration(
         60 * DAYS.toSeconds(7) +
            DAYS.toSeconds(4) +
            HOURS.toSeconds(5) +
            MINUTES.toSeconds(6) +
            7,
         "60 weeks 4 days 5 hours 6 minutes 7 seconds");
   }

   private void toFriendlyDuration(long durationSecs, String expectedOutput)
   {
      String actualOutput = TimeUtil.toFriendlyDuration(durationSecs);

      assertThat(actualOutput, equalTo(expectedOutput));
   }

   private void toFriendlyDuration(long duration, TimeUnit timeUnit, String expectedOutput)
   {
      String actualOutput = TimeUtil.toFriendlyDuration(duration, timeUnit);

      assertThat(actualOutput, equalTo(expectedOutput));
   }
}


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
package com.iris.test.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class TestUtils {

   public static boolean verifyDate(int expYear, int expMonth, int expDay, int expHour, int expMin, int expSec, Date value) {
      GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
      gc.setTime(value);
      // 2015-04-23T18:23:09.123
      return ((expYear == gc.get(Calendar.YEAR)) &&
            (expMonth - 1 == gc.get(Calendar.MONTH)) &&
            (expDay == gc.get(Calendar.DAY_OF_MONTH)) &&
            (expHour == gc.get(Calendar.HOUR_OF_DAY)) &&
            (expMin == gc.get(Calendar.MINUTE)) &&
            (expSec == gc.get(Calendar.SECOND)));
   }

   public static void appendLinesToFile(File file, String data[]) throws Exception {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))){
         for (String line : data){
            writer.write(line);
            writer.newLine();
         }
      }
   }
}


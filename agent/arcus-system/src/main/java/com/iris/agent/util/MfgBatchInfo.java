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
package com.iris.agent.util;

import java.util.Calendar;
import java.util.Date;

public class MfgBatchInfo {
   private int model = DEF_HW_MODEL;
   private int hwVersion = DEF_HW_VERSION;
   private int dateMM = DEF_MFG_MONTH;
   private int dateDD = DEF_MFG_DAY;
   private int dateYYYY = DEF_MFG_YEAR;
   private int factoryId = DEF_CENTRALITE_FACTORY;
   private int testStation = DEF_TEST_STATION;
   private int testerNumber = DEF_TESTER;

   private static final int DEF_HW_MODEL = 1;
   private static final int DEF_HW_VERSION = 1;
   private static final int DEF_MFG_MONTH = 1;
   private static final int DEF_MFG_DAY = 1;
   private static final int DEF_MFG_YEAR = 2015;
   private static final int DEF_CENTRALITE_FACTORY = 0;
   private static final int DEF_JABIL_FACTORY = 3;
   private static final int DEF_TEST_STATION = 1;
   private static final int DEF_TESTER = 1;

   public MfgBatchInfo(String batchNumber) {

      if ((batchNumber == null) || batchNumber.isEmpty()) {
         // No data, may be an early prototype - return defaults
      } else if (batchNumber.startsWith("10")) {
         // Devices made by Jabil have their manufacturing data format, not ours which is only on the label not
         //  stored in the device.
         //  Their data is in the format:
         //   DEVICE_ID|BUILD_YEAR|BUILD_WEEK:UNIT_NO
         //   So, 101162514221 would decode as:
         //   DEVICE_ID = 101
         //   BUILD_YEAR = 16
         //   BUILD_WEEK = 25
         //   UNIT_NO = 14221
         String buildWW = batchNumber.substring(5,7);
         String buildYY = "20" + batchNumber.substring(3,5);
         Calendar cal = Calendar.getInstance();
         cal.set(Calendar.YEAR, Integer.parseInt(buildYY));
         cal.set(Calendar.WEEK_OF_YEAR, Integer.parseInt(buildWW));
         this.dateMM = cal.get(Calendar.MONTH) + 1;
         this.dateDD = cal.get(Calendar.DAY_OF_MONTH);
         this.dateYYYY = cal.get(Calendar.YEAR);
         this.factoryId = DEF_JABIL_FACTORY;
      } else {
         long batch = Long.parseLong(batchNumber);

         // Translate batch value based on https://eyeris.atlassian.net/wiki/spaces/I2D/pages/5439711/Hub+Label
         this.testerNumber = (int)((batch >> 0) & 0x3FF);
         this.testStation = (int)((batch >> 10) & 0x7F);
         this.factoryId = (int)((batch >> 17) & 0x1F);
         this.dateYYYY = 2000 + (int)((batch >> 22) & 0x1F);
         this.dateDD = (int)((batch >> 27) & 0x1F);
         this.dateMM = 1 + (int)((batch >> 32) & 0x0F);
         this.hwVersion = (int)((batch >> 36) & 0x07);
         this.model = (int)((batch >> 39) & 0x07);
      }
   }

   public int getMfgModel() {
      return model;
   }

   public int getMfgHwVersion() {
      return hwVersion;
   }

   public int getMfgMonth() {
      return dateMM;
   }

   public int getMfgDay() {
      return dateDD;
   }

   public int getMfgYear() {
      return dateYYYY;
   }

   public Date getMfgDate() {
      Calendar cal = Calendar.getInstance();
      cal.set(dateYYYY, dateMM - 1, dateDD, 0, 0);
      return cal.getTime();
   }

   public int getMfgFactoryID() {
      return factoryId;
   }

   public int getMfgTestStation() {
      return testStation;
   }

   public int getMfgTester() {
      return testerNumber;
   }

}



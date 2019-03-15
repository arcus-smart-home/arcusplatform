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

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import com.iris.messages.address.Address;

public class TypeFixtures {
   
   public static final String TEST_UUID = "65cbd103-1310-4d92-a5c9-85bb54ffa3f0";
   
   public static final Address TEST_ADDRESS = Address.platformDriverAddress(UUID.fromString(TEST_UUID));
   public static final Address TEST_ADDRESS_SERVICE = Address.platformService("bogus");
   
   public static final String TEST_STRING_ADDRESS = TEST_ADDRESS.getRepresentation();
   public static final String TEST_STRING_ADDRESS_SERVICE = TEST_ADDRESS_SERVICE.getRepresentation();
   public static final String TEST_STRING_STRING = "This is a simple string."; 
   public static final String TEST_STRING_BOOL_FALSE = "false";
   public static final String TEST_STRING_BOOL_TRUE = "true";
   public static final String TEST_STRING_BYTE = "17";
   public static final String TEST_STRING_BYTE_HEX = "0x11";
   public static final String TEST_STRING_BYTE_NEG = "-17";
   public static final String TEST_STRING_INT = "233123";
   public static final String TEST_STRING_LONG = "41000000000";
   public static final String TEST_STRING_DOUBLE = "123.45";
   public static final String TEST_STRING_DATE = "1438976178325";
   
   public static final long TEST_LONG_DATE = 1438976178325l;
   public static final long TEST_LONG_LONG = 41000000000l;
   public static final long TEST_LONG_DOUBLE = 42l;
   public static final long TEST_LONG_BYTE = 17l;
   public static final long TEST_LONG_INT = 233123l;
   public static final long TEST_LONG_BOOL_TRUE = 1l;
   public static final long TEST_LONG_BOOL_FALSE = 0l;
   
   public static final int TEST_INT_DOUBLE = 42;
   public static final int TEST_INT_BYTE = 17;
   public static final int TEST_INT_INT = 233123;
   public static final int TEST_INT_BOOL_TRUE = 1;
   public static final int TEST_INT_BOOL_FALSE = 0;
   
   public static final byte TEST_BYTE_BYTE = 0x11;
   public static final byte TEST_BYTE_NEG = -0x11;
   public static final byte TEST_BYTE_DOUBLE = 42;
   public static final byte TEST_BYTE_BOOL_TRUE = 1;
   public static final byte TEST_BYTE_BOOL_FALSE = 0;
   
   public static final double TEST_DOUBLE_DATE = 1438976178325d;
   public static final double TEST_DOUBLE_DOUBLE = 42d;
   public static final double TEST_DOUBLE_STRING = 123.45;
   public static final double TEST_DOUBLE_LONG = 41000000000d;
   public static final double TEST_DOUBLE_INT = 233123d;
   public static final double TEST_DOUBLE_BYTE = 17d;
   public static final double TEST_DOUBLE_BOOL_TRUE = 1d;
   public static final double TEST_DOUBLE_BOOL_FALSE = 0d;
   
   public static final Date TEST_DATE_DATE = new Date(TEST_LONG_DATE);
   
   public static final Calendar TEST_CAL_DATE = getCal(TEST_DATE_DATE);
         
   public static Calendar getCal(Date date) {
      Calendar cal = Calendar.getInstance();
      cal.setTime(date);
      return cal;
   }
}


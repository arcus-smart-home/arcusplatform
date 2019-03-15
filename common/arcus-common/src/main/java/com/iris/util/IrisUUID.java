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

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.ISAACRandom;

public final class IrisUUID {
   private static final ThreadLocal<RandomGenerator> RANDOM = new ThreadLocal<RandomGenerator>() {
      @Override
      protected RandomGenerator initialValue() {
         return new ISAACRandom((new SecureRandom()).nextLong());
      }
   };

   private static final UUID NIL_UUID = new UUID(0L, 0L);
   private static final long TIME_OFFSET = getStartEpoch();
   private static final UUID MAX_TIME_UUID = new UUID(0xFFFFFFFFFFFF1FFFL, 0xBFFFFFFFFFFFFFFFL);
   private static final UUID MIN_TIME_UUID = new UUID(0x0000000000001000L, 0x8000010000000000L);

   private IrisUUID() {
   }

   public static UUID nilUUID() {
      return NIL_UUID;
   }
   
   public static UUID minTimeUUID() {
      return MIN_TIME_UUID;
   }
   
   public static UUID maxTimeUUID() {
      return MAX_TIME_UUID;
   }

   public static UUID timeUUID() {
      return timeUUID(System.currentTimeMillis());
   }

   public static UUID timeUUID(Date time) {
      return timeUUID(time.getTime());
   }

   public static UUID timeUUID(long time) {
      return timeUUID(time, RANDOM.get().nextLong());
   }

   public static UUID timeUUID(Date time, long random) {
      return timeUUID(time.getTime(), random);
   }

   public static UUID timeUUID(long time, long random) {
      long msb = timeMsb((time-TIME_OFFSET)*10000);
      long lsb = (random & 0x3FFFFFFFFFFFFFFFL) | 0x8000010000000000L;
      return new UUID(msb, lsb);
   }
   
   public static Comparator<UUID> ascTimeUUIDComparator() {
      return TimeUUIDComparator.AscComparator;
   }
   
   public static Comparator<UUID> descTimeUUIDComparator() {
      return TimeUUIDComparator.DescComparator;
   }

   public static UUID randomUUID() {
      long msb = (RANDOM.get().nextLong() & 0xFFFFFFFFFFFF0FFFL) | 0x0000000000004000L;
      long lsb = (RANDOM.get().nextLong() & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
      return new UUID(msb, lsb);
   }

   public static boolean isNil(UUID uuid) {
      return uuid.getMostSignificantBits() == 0 && uuid.getLeastSignificantBits() == 0;
   }

   public static boolean isTime(UUID uuid) {
      return uuid.variant() == 2 && uuid.version() == 1;
   }

   public static long timeof(UUID uuid) {
      return (uuid.timestamp()/10000) + TIME_OFFSET;
   }
   
   private static long timeMsb(long ts) {
      long msb = 0L;
      msb |= (0x00000000ffffffffL & ts) << 32;
      msb |= (0x0000ffff00000000L & ts) >>> 16;
      msb |= (0x0fff000000000000L & ts) >>> 48;
      msb |= 0x0000000000001000L;
      return msb;
   }

   private static long getStartEpoch() {
      Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT-0"));
      c.set(Calendar.YEAR, 1582);
      c.set(Calendar.MONTH, Calendar.OCTOBER);
      c.set(Calendar.DAY_OF_MONTH, 15);
      c.set(Calendar.HOUR_OF_DAY, 0);
      c.set(Calendar.MINUTE, 0);
      c.set(Calendar.SECOND, 0);
      c.set(Calendar.MILLISECOND, 0);
      return c.getTimeInMillis();
   }

   /////////////////////////////////////////////////////////////////////////////
   // UUID Parsing Replacement for Inefficient Java 8 Parsing
   //
   // see:
   // http://bugs.java.com/bugdatabase/view_bug.do?bug_id=8006627
   // http://hg.openjdk.java.net/jdk9/jdk9/jdk/rev/3b298c230549
   // https://datastax-oss.atlassian.net/browse/JAVA-272
   /////////////////////////////////////////////////////////////////////////////
   
   public static UUID fromString(String value) {
      if (value.length() != 36) {
         throw new IllegalArgumentException("Invalid UUID: " + value);
      }

      long lo = 0;
      long hi = 0;
      for (int i=0; i<36; i+=4) {
         if (i == 8 || i == 13 || i == 18 || i == 23) {
            if (value.charAt(i) != '-') {
               throw new IllegalArgumentException("Cannot parse UUID: " + value);
            }
            ++i;
         }

         int curr;
         int c1 = value.charAt(i);
         int c2 = value.charAt(i+1);
         int c3 = value.charAt(i+2);
         int c4 = value.charAt(i+3);

         if (c1 >= 48 && c1 <= 57) curr = (c1 - 48) << 12;
         else if (c1 >= 97 && c1 <= 102) curr = (c1 - 87) << 12;
         else if (c1 >= 65 && c1 <= 70) curr = (c1 - 55) << 12;
         else throw new IllegalArgumentException("Cannot parse UUID: " + value);

         if (c2>=48 && c2<=57)      curr |= (c2 - 48) << 8;
         else if (c2>=97 && c2<=102) curr |= (c2 - 87) << 8;
         else if (c2>=65 && c2<=70) curr |= (c2 - 55) << 8;
         else throw new IllegalArgumentException("Cannot parse UUID: " + value);

         if (c3>=48 && c3<=57)      curr |= (c3 - 48) << 4;
         else if (c3>=97 && c3<=102) curr |= (c3 - 87) << 4;
         else if (c3>=65 && c3<=70) curr |= (c3 - 55) << 4;
         else throw new IllegalArgumentException("Cannot parse UUID: " + value);

         if (c4>=48 && c4<=57)      curr |= (c4 - 48);
         else if (c4>=97 && c4<=102) curr |= (c4 - 87);
         else if (c4>=65 && c4<=70) curr |= (c4 - 55);
         else throw new IllegalArgumentException("Cannot parse UUID: " + value);

         if (i < 19) hi = (hi << 16) | curr;
         else lo = (lo << 16) | curr;
      }

      return new UUID(hi, lo);
   }

   public static String toString(UUID uuid) {
      return toString(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
   }
 
   private static String toString(long msb, long lsb) {
      char[] uuidChars = new char[36];

      hexDigits(uuidChars, 0, 8, msb >> 32);
      uuidChars[8] = '-';
      hexDigits(uuidChars, 9, 4, msb >> 16);
      uuidChars[13] = '-';
      hexDigits(uuidChars, 14, 4, msb);
      uuidChars[18] = '-';
      hexDigits(uuidChars, 19, 4, lsb >> 48);
      uuidChars[23] = '-';
      hexDigits(uuidChars, 24, 12, lsb);

      return new String(uuidChars);
   }
 
   private static void hexDigits(char[] dest, int offset, int digits, long val) {
      long hi = 1L << (digits * 4);
      toUnsignedString(dest, offset, digits, hi | (val & (hi - 1)), 4);
   }

   private final static char[] HEX_DIGITS = {
      '0' , '1' , '2' , '3' , '4' , '5' ,
      '6' , '7' , '8' , '9' , 'a' , 'b' ,
      'c' , 'd' , 'e' , 'f'
   };

   private static void toUnsignedString(char[] dest, int offset, int len, long i, int shift) {
      int charPos = len;
      int radix = 1 << shift;
      long mask = radix - 1;
      do {
         dest[offset + --charPos] = HEX_DIGITS[(int)(i & mask)];
         i >>>= shift;
      } while (i != 0 && charPos > 0);
   }

   private static class TimeUUIDComparator implements Comparator<UUID> {
      private static final TimeUUIDComparator AscComparator = new TimeUUIDComparator(-1, 1);
      private static final TimeUUIDComparator DescComparator = new TimeUUIDComparator(1, -1);
      
      private final int lessThan;
      private final int greaterThan;
      
      private TimeUUIDComparator(int lessThan, int greaterThan) {
         this.lessThan = lessThan;
         this.greaterThan = greaterThan;
      }

      @Override
      public int compare(UUID o1, UUID o2) {
         if(o1 == o2) {
            return 0;
         }
         // always sort null to the end
         else if(o1 == null) {
            return 1;
         }
         else if(o2 == null) {
            return -1;
         }
         
         long ts1 = o1.timestamp();
         long ts2 = o2.timestamp();
         if(ts1 == ts2) {
            long lsb1 = o1.getLeastSignificantBits();
            long lsb2 = o2.getLeastSignificantBits();
            if(lsb1 == lsb2) {
               return 0;
            }
            else if(lsb1 < lsb2) {
               return lessThan;
            }
            else {
               return greaterThan;
            }
         }
         else if(ts1 < ts2) {
            return lessThan;
         }
         else {
            return greaterThan;
         }
      }
   }

}


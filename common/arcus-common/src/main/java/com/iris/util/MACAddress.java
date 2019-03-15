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

public class MACAddress {
	@Deprecated
	public static long toLong(String mac) {
	   return macToLong(mac);
	}

	public static long macToLong(String mac) {
		int length = mac.length();
      if (length == 12) {
         return (hexDigitValue(mac.charAt(0)) << 44L) |
                (hexDigitValue(mac.charAt(1)) << 40L) |
                (hexDigitValue(mac.charAt(2)) << 36L) |
                (hexDigitValue(mac.charAt(3)) << 32L) |
                (hexDigitValue(mac.charAt(4)) << 28L) |
                (hexDigitValue(mac.charAt(5)) << 24L) |
                (hexDigitValue(mac.charAt(6)) << 20L) |
                (hexDigitValue(mac.charAt(7)) << 16L) |
                (hexDigitValue(mac.charAt(8)) << 12L) |
                (hexDigitValue(mac.charAt(9)) <<  8L) |
                (hexDigitValue(mac.charAt(10)) << 4L) |
                hexDigitValue(mac.charAt(11));
      }

      if (length == 17) {
         return (hexDigitValue(mac.charAt(0)) << 44L) |
                (hexDigitValue(mac.charAt(1)) << 40L) |
                (hexDigitValue(mac.charAt(3)) << 36L) |
                (hexDigitValue(mac.charAt(4)) << 32L) |
                (hexDigitValue(mac.charAt(6)) << 28L) |
                (hexDigitValue(mac.charAt(7)) << 24L) |
                (hexDigitValue(mac.charAt(9)) << 20L) |
                (hexDigitValue(mac.charAt(10)) << 16L) |
                (hexDigitValue(mac.charAt(12)) << 12L) |
                (hexDigitValue(mac.charAt(13)) <<  8L) |
                (hexDigitValue(mac.charAt(15)) << 4L) |
                hexDigitValue(mac.charAt(16));
      }

      throw new RuntimeException("invalid mac format: " + mac);
   }

    private static long hexDigitValue(char ch) {
       switch (ch) {
       case '0': return 0;
       case '1': return 1;
       case '2': return 2;
       case '3': return 3;
       case '4': return 4;
       case '5': return 5;
       case '6': return 6;
       case '7': return 7;
       case '8': return 8;
       case '9': return 9;
       case 'a': case 'A': return 10;
       case 'b': case 'B': return 11;
       case 'c': case 'C': return 12;
       case 'd': case 'D': return 13;
       case 'e': case 'E': return 14;
       case 'f': case 'F': return 15;
       default: throw new RuntimeException("not a hex digit: " + ch);
       }
    }
}


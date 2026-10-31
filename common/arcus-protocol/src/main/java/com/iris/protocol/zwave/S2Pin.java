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
package com.iris.protocol.zwave;

import java.util.regex.Pattern;

/**
 * The formats shared by the platform and the hub for Z-Wave S2 PIN entry.
 *
 * A DSK is the first 16 bytes of a node's public key, as eight groups of
 * five decimal digits with each group holding two bytes. The first group is
 * the PIN, which the node blanks over the air. Both values are secrets: never
 * log them, and don't put either in an exception message.
 */
public final class S2Pin {
   private static final int MAX_GROUP = 0xFFFF;
   private static final int GROUP_LENGTH = 5;

   private static final Pattern PIN = Pattern.compile("[0-9]{5}");
   private static final Pattern BLANKED_DSK = Pattern.compile("00000(-[0-9]{5}){7}");

   private S2Pin() {
   }

   /**
    * True for exactly five decimal digits that fit the two bytes they stand
    * for. A larger value is a typo that the hub could only truncate.
    */
   public static boolean isValidPin(Object value) {
      if (!(value instanceof String)) {
         return false;
      }

      String pin = (String) value;
      return PIN.matcher(pin).matches() && Integer.parseInt(pin) <= MAX_GROUP;
   }

   /**
    * True for a DSK with the PIN blanked, as a joining node sends it. This
    * refuses a filled in first group, so a PIN can't travel inside a DSK.
    */
   public static boolean isValidBlankedDsk(Object value) {
      if (!(value instanceof String)) {
         return false;
      }

      String dsk = (String) value;
      if (!BLANKED_DSK.matcher(dsk).matches()) {
         return false;
      }

      for (int start = 0; start < dsk.length(); start += GROUP_LENGTH + 1) {
         if (Integer.parseInt(dsk.substring(start, start + GROUP_LENGTH)) > MAX_GROUP) {
            return false;
         }
      }
      return true;
   }
}

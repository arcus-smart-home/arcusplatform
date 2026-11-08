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
package com.iris.oculus.modules.pairing.wizard;

import java.util.Arrays;
import java.util.Date;

/**
 * What the S2 PIN prompt shows. See docs/zwave-s2-pin-pairing.md. The PIN itself
 * is checked with S2Pin.isValidPin, the same rule the platform uses.
 */
final class S2Pins {

   private S2Pins() {
   }

   /**
    * Whether s2PinRequestId names a request. It is empty once a request ends,
    * since a null never reaches clients, and absent before the first one.
    */
   static boolean isWaiting(Object requestId) {
      return requestId != null && !requestId.toString().isEmpty();
   }

   /**
    * The part of a DSK the user checks against the label. The first group is the
    * PIN, which the device left blank and which the user types in; checking the
    * other seven is what protects the pairing from a man in the middle.
    */
   static String groupsToCheck(String dsk) {
      String[] groups = dsk.split("-");
      if (groups.length < 2) {
         return dsk;
      }
      return String.join("-", Arrays.copyOfRange(groups, 1, groups.length));
   }

   static long secondsLeft(Date expires, long now) {
      return expires == null ? 0 : Math.max(0, (expires.getTime() - now + 999) / 1000);
   }
}

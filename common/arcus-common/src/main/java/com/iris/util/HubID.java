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

public class HubID {
    private static final char[] ALLOWED_CHARS = "ABCDEFGHJKLNPQRSTUVWXYZ".toCharArray();
    private static final long ALLOWED_SIZE = ALLOWED_CHARS.length;

    public static String fromMac(String mac) {
       return fromMac(MACAddress.macToLong(mac));
    }

    public static String fromMac(long mac) {
       long macl = mac >> 1;
       long digits = (macl % 10000L) & 0xFFFF;
       long remainder = (macl / 10000L);
       int index;
       index = (int) (remainder % ALLOWED_SIZE);
       char thd = ALLOWED_CHARS[index];
       remainder = remainder / ALLOWED_SIZE;
       index = (int) (remainder % ALLOWED_SIZE);
       char snd = ALLOWED_CHARS[index];
       remainder = remainder / ALLOWED_SIZE;
       index = (int) (remainder % ALLOWED_SIZE);
       char fst = ALLOWED_CHARS[index];

       StringBuilder bld = new StringBuilder(8);
       bld.append(fst);
       bld.append(snd);
       bld.append(thd);
       bld.append('-');

       if (digits < 10) bld.append("000").append(digits);
       else if (digits < 100) bld.append("00").append(digits);
       else if (digits < 1000) bld.append("0").append(digits);
       else bld.append(digits);

       return bld.toString();
    }
}


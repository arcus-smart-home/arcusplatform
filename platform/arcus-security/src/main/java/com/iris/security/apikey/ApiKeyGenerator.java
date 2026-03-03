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
package com.iris.security.apikey;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class ApiKeyGenerator {

   private static final String PREFIX = "arcus_sk_";
   private static final int KEY_BYTES = 16; // 128 bits
   private static final int DISPLAY_PREFIX_LENGTH = 17; // "arcus_sk_" + 8 hex chars

   private static final SecureRandom RANDOM = new SecureRandom();

   public static String generate() {
      byte[] bytes = new byte[KEY_BYTES];
      RANDOM.nextBytes(bytes);
      return PREFIX + toHex(bytes);
   }

   public static String extractPrefix(String rawKey) {
      if (rawKey == null || rawKey.length() < DISPLAY_PREFIX_LENGTH) {
         throw new IllegalArgumentException("Invalid API key format");
      }
      return rawKey.substring(0, DISPLAY_PREFIX_LENGTH);
   }

   public static String hashKey(String rawKey) {
      try {
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         byte[] hash = digest.digest(rawKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
         return toHex(hash);
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException("SHA-256 not available", e);
      }
   }

   private static String toHex(byte[] bytes) {
      StringBuilder sb = new StringBuilder(bytes.length * 2);
      for (byte b : bytes) {
         sb.append(String.format("%02x", b & 0xff));
      }
      return sb.toString();
   }
}

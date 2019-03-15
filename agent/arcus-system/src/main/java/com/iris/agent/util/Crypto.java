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

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.jdt.annotation.Nullable;

public final class Crypto {
   private static final Random random = new SecureRandom();

   public static long benchmarkMessageDigest(String algorithm, int blocks, @Nullable String provider) throws Exception {
      byte[] data = getRandomData(1024);

      MessageDigest md;
      if (provider == null) {
         md = MessageDigest.getInstance(algorithm);
      } else {
         md = MessageDigest.getInstance(algorithm, provider);
      }

      long start = System.nanoTime();
      for (int i = 0; i < blocks; ++i) {
         md.update(data);
      }

      md.digest();
      return System.nanoTime() - start;
   }

   public static long benchmarkAes(int blocks, @Nullable String provider) throws Exception {
      byte[] salt = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };

      SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      KeySpec spec = new PBEKeySpec("testpassword".toCharArray(), salt, 65536, 128);
      SecretKey tmp = factory.generateSecret(spec);
      SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");

      Cipher cipher;
      if (provider == null) {
         cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      } else {
         cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", provider);
      }

      cipher.init(Cipher.ENCRYPT_MODE, secret);
      //AlgorithmParameters params = cipher.getParameters();
      //byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();

      byte[] data = getRandomData(1024);
      long start = System.nanoTime();
      for (int i = 0; i < blocks; ++i) {
         cipher.update(data);
      }

      cipher.doFinal();
      return System.nanoTime() - start;
   }

   private static byte[] getRandomData(int size) {
      byte[] data = new byte[size];
      random.nextBytes(data);

      return data;
   }
}


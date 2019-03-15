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
package com.iris.security.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.apache.shiro.crypto.hash.Sha256Hash;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.Utils;

/**
 * This class requires the Unlimited Strength Jurisdiction Policy installed.  See:
 * http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html
 */
public class AES {

   private static final String KEY_ALG = "AES";
   private static final String CIPHER_ALG = "AES/CBC/PKCS5Padding";

   @Inject
   @Named("iris.aes.secret")
   private String secret;

   @Inject
   @Named("iris.aes.iv")
   private String iv;

   public AES() {
   }

   public AES(String secret, String iv) {
      this.secret = secret;
      this.iv = iv;
   }

   /**
    * @param key
    * @param value
    * @return
    *    Encrypted value base 64 encoded
    */
   public String encrypt(String key, String value) {
      Preconditions.checkNotNull(key, "key cannot be null");
      if(StringUtils.isBlank(value)) {
         return null;
      }

      try {
         SecretKey secretKey = new SecretKeySpec(sha1(key, secret), KEY_ALG);
         IvParameterSpec ivParamSpec = new IvParameterSpec(md5(iv));
         Cipher cipher = Cipher.getInstance(CIPHER_ALG);
         cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParamSpec);
         byte[] encrypted = cipher.doFinal(value.getBytes("UTF-8"));
         return Utils.b64Encode(encrypted);
      } catch(Exception e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * @param key
    * @param value base 64 encoded
    * @return
    */
   public String decrypt(String key, String value) {
      Preconditions.checkNotNull(key, "key cannot be null");
      if(StringUtils.isBlank(value)) {
         return null;
      }

      try {
         SecretKey secretKey = new SecretKeySpec(sha1(key, secret), KEY_ALG);
         IvParameterSpec ivParamSpec = new IvParameterSpec(md5(iv));
         Cipher cipher = Cipher.getInstance(CIPHER_ALG);
         cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParamSpec);
         byte[] encrypted = cipher.doFinal(Utils.b64Decode(value));
         return new String(encrypted, "UTF-8");
      } catch(Exception e) {
         throw new RuntimeException(e);
      }
   }

   private byte[] sha1(String key, String salt) throws Exception {
      return new Sha256Hash(key, salt).getBytes();
   }

   private byte[] md5(String str) throws Exception {
      return new Md5Hash(str).getBytes();
   }
}


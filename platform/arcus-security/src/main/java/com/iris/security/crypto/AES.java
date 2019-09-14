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
import javax.crypto.spec.GCMParameterSpec;

import com.google.common.primitives.Bytes;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.apache.shiro.crypto.hash.Sha256Hash;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * This class requires the Unlimited Strength Jurisdiction Policy installed.  See:
 * http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html
 */
public class AES {
   private static final Logger logger = LoggerFactory.getLogger(AES.class);

   private static final String KEY_ALG = "AES";
   private static final String CIPHER_ALG = "AES/CBC/PKCS5Padding";
   private static final int GCM_IV_LENGTH = 12;

   @Inject
   @Named("iris.aes.secret")
   private String secret;

   @Inject
   @Named("iris.aes.iv")
   private String unsafeIV;

   private SecureRandom random;

   public AES() throws NoSuchAlgorithmException {
      this.random = SecureRandom.getInstanceStrong();
   }

   public AES(String secret, String unsafeIV) throws NoSuchAlgorithmException {
      this.secret = secret;
      this.unsafeIV = unsafeIV;

      this.random = SecureRandom.getInstanceStrong();
   }

   /**
    * @param key
    * @param value
    * @return Encrypted value base64 encoded
    */
   public String encrypt(String key, String value) {
      return encryptSafe(key, value);
   }

   /**
    * This is the old version of the Arcus crypto implementation.
    * AES CBC is subject to padding vulnerabilities and fallen into decline.
    *
    * TODO: this should be deleted, as we shouldn't encrypt this way anymore.
    *
    * @param key
    * @param value
    * @return
    */
   protected String encryptUnsafe(String key, String value) {
      Preconditions.checkNotNull(key, "key cannot be null");
      if (StringUtils.isBlank(value)) {
         return null;
      }

      try {
         SecretKey secretKey = new SecretKeySpec(sha1(key, secret), KEY_ALG);
         IvParameterSpec ivParamSpec = new IvParameterSpec(md5(unsafeIV));

         Cipher cipher = Cipher.getInstance(CIPHER_ALG);
         cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParamSpec);

         byte[] encrypted = cipher.doFinal(value.getBytes("UTF-8"));
         return Utils.b64Encode(encrypted);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * "Safe" encryptor that uses AES-GCM
    *
    * @param key
    * @param value
    * @return
    */
   protected String encryptSafe(String key, String value) {
      Preconditions.checkNotNull(key, "key cannot be null");
      if (StringUtils.isBlank(value)) {
         return null;
      }

      try {
         SecretKeySpec keySpec = new SecretKeySpec(sha1(key, secret), KEY_ALG);
         byte[] iv = new byte[GCM_IV_LENGTH];
         this.random.nextBytes(iv);

         Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
         IvParameterSpec ivParamSpec = new IvParameterSpec(iv);

         cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParamSpec);

         byte data[] = cipher.doFinal(value.getBytes("UTF-8"));

         // Store the iv before the ciphertext
         byte[] ciphertext = Bytes.concat(iv, data);
         return Utils.b64Encode(ciphertext);
      } catch (NoSuchProviderException e) {
         logger.error("Couldn't find BouncyCastle Provider!", e);
         throw new RuntimeException(e);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * @param key
    * @param value base 64 encoded
    * @return
    */
   public String decrypt(String key, String value) {
      try {
         return decryptSafe(key, value);
      } catch (Exception e) {
         return decryptUnsafe(key, value);
      }
   }

   /**
    * Unsafe AES-CBC encryption
    *
    * @param key
    * @param value
    * @return
    */
   protected String decryptUnsafe(String key, String value) {
      Preconditions.checkNotNull(key, "key cannot be null");
      if (StringUtils.isBlank(value)) {
         return null;
      }

      try {
         SecretKey secretKey = new SecretKeySpec(sha1(key, secret), KEY_ALG);
         IvParameterSpec ivParamSpec = new IvParameterSpec(md5(unsafeIV));
         Cipher cipher = Cipher.getInstance(CIPHER_ALG);
         cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParamSpec);
         byte[] encrypted = cipher.doFinal(Utils.b64Decode(value));
         return new String(encrypted, "UTF-8");
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * Decrypt data encrypted with encryptSafe()
    *
    * @param key
    * @param value
    * @return
    */
   protected String decryptSafe(String key, String value) {
      Preconditions.checkNotNull(key, "key cannot be null");
      if (StringUtils.isBlank(value)) {
         return null;
      }

      try {
         byte[] decoded = Utils.b64Decode(value);

         SecretKey secretKey = new SecretKeySpec(sha1(key, secret), KEY_ALG);
         IvParameterSpec ivParamSpec = new IvParameterSpec(Arrays.copyOfRange(decoded, 0, GCM_IV_LENGTH));

         Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
         cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParamSpec);

         byte[] plaintext = cipher.doFinal(Arrays.copyOfRange(decoded, GCM_IV_LENGTH, decoded.length));
         return new String(plaintext, "UTF-8");
      } catch (NoSuchProviderException e) {
         logger.error("Couldn't find BouncyCastle Provider!", e);
         throw new RuntimeException(e);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   // XXX: why is this called sha1?
   private byte[] sha1(String key, String salt) throws Exception {
      return new Sha256Hash(key, salt).getBytes();
   }

   private byte[] md5(String str) throws Exception {
      return new Md5Hash(str).getBytes();
   }
}
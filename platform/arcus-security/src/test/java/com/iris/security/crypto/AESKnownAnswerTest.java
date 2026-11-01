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

import java.security.Security;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Known-answer tests for the two digests {@link AES} uses to derive its AES key
 * and its AES-CBC initialization vector.
 *
 * <p>
 * These are not round-trip tests. Every expected value below is a literal
 * captured from the Apache Shiro 1.13.0 implementation ({@code Md5Hash} /
 * {@code Sha256Hash}) before the Shiro 2.x migration, which replaced
 * {@code Md5Hash} -- removed in Shiro 2 -- with a plain
 * {@code MessageDigest.getInstance("MD5")}. The derived key and IV decrypt data
 * that is already encrypted at rest, so any change to these bytes silently
 * breaks decryption of existing rows. If a change to {@link AES} makes a test
 * here fail, the change is wrong -- do not update the expected values.
 * </p>
 */
public class AESKnownAnswerTest {

   /** Same test vectors as {@link TestAES}. */
   private static final String SECRET = "ylntN5dSPuTQoWga7TArBdlMhEibX+bG4h6sit2Jfq4=";
   private static final String UNSAFE_IV = "8e2DM7K/G2I=";

   private AES aes;

   @Before
   public void setUp() throws Exception {
      Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
      aes = new AES(SECRET, UNSAFE_IV);
   }

   private static String hex(byte[] bytes) {
      StringBuilder sb = new StringBuilder(bytes.length * 2);
      for (byte b : bytes) {
         sb.append(String.format("%02x", b));
      }
      return sb.toString();
   }

   private static void assertMd5(String expectedHex, String input) throws Exception {
      byte[] actual = AES.md5(input);
      Assert.assertEquals("MD5 digest length changed for input [" + input + "]", 16, actual.length);
      Assert.assertEquals("MD5 digest changed for input [" + input + "]", expectedHex, hex(actual));
   }

   @Test
   public void testMd5KnownAnswersAscii() throws Exception {
      assertMd5("d41d8cd98f00b204e9800998ecf8427e", "");
      assertMd5("0cc175b9c0f1b6a831c399e269772661", "a");
      assertMd5("900150983cd24fb0d6963f7d28e17f72", "abc");
      assertMd5("4032af8d61035123906e58e067140cc5", "0123456789abcdef");
      assertMd5("6dcfdad2ddbcc56d1b18f566bafa0e48", "iris-aes-iv-sample");
   }

   @Test
   public void testMd5KnownAnswerForDefaultTestIv() throws Exception {
      // The actual IV material used by TestAES, i.e. the real code path.
      assertMd5("c389620344cb8b9c66cdfc8cd0e6df8d", UNSAFE_IV);
   }

   @Test
   public void testMd5KnownAnswersNonAscii() throws Exception {
      // Shiro's CodecSupport hashed Strings as UTF-8; MessageDigest must be fed
      // UTF-8 bytes explicitly to match.
      assertMd5("5529220e24f1479a9c2133843dd39690", "café über");
      assertMd5("65396ee4aad0b4f17aacd1c6112ee364", "你好世界");
   }

   @Test
   public void testSha256SaltedKnownAnswer() throws Exception {
      Assert.assertEquals("3c09f454ed350907b74c7a75ada2c9812269fa477ee60f192872c7cc73d068aa",
            hex(AES.sha1("foo", SECRET)));
   }

   /**
    * End-to-end: ciphertext produced by the Shiro 1.13.0 build must still
    * decrypt. This covers the salted SHA-256 key derivation and the MD5 IV
    * derivation together, with no reliance on package-private helpers.
    */
   @Test
   public void testDecryptLiteralCbcCiphertextFromShiro113() {
      Assert.assertEquals("foo1", aes.decryptUnsafe("foo", "LeVKGYRgTzvwLc7XReyjfw=="));
      Assert.assertEquals("a very secret secret",
            aes.decryptUnsafe("key-2", "XYo0KeoCcrVm+YhxqZvBq097UT3t0b35PIzXygAvzN4="));
      Assert.assertEquals("café über 你好",
            aes.decryptUnsafe("key-3", "93Rzw/dgfQHMlhZKMrVZde/Jk0qmjg9/ZkszDzdIBvk="));
   }

   /**
    * AES-CBC here uses a fixed, MD5-derived IV, so encryption is deterministic:
    * the ciphertext must stay byte-identical as well.
    */
   @Test
   public void testEncryptLiteralCbcCiphertextMatchesShiro113() {
      Assert.assertEquals("LeVKGYRgTzvwLc7XReyjfw==", aes.encryptUnsafe("foo", "foo1"));
      Assert.assertEquals("XYo0KeoCcrVm+YhxqZvBq097UT3t0b35PIzXygAvzN4=",
            aes.encryptUnsafe("key-2", "a very secret secret"));
      Assert.assertEquals("93Rzw/dgfQHMlhZKMrVZde/Jk0qmjg9/ZkszDzdIBvk=",
            aes.encryptUnsafe("key-3", "café über 你好"));
   }

   /**
    * AES-GCM uses a random IV, so only decryption of a captured literal is
    * deterministic. This pins the salted SHA-256 key derivation for the "safe"
    * path.
    */
   @Test
   public void testDecryptLiteralGcmCiphertextFromShiro113() {
      Assert.assertEquals("a very secret secret",
            aes.decryptSafe("foo", "cuvL/2K+BK1wb/vUjmreXnM0VvO9t9nGtNbt/1AmlAMTPhLMmV6VyEISVRPAnNRn"));
   }

   /** The public dispatcher must pick the right path for both literals. */
   @Test
   public void testDecryptDispatchesForBothLiterals() {
      Assert.assertEquals("foo1", aes.decrypt("foo", "LeVKGYRgTzvwLc7XReyjfw=="));
      Assert.assertEquals("a very secret secret",
            aes.decrypt("foo", "cuvL/2K+BK1wb/vUjmreXnM0VvO9t9nGtNbt/1AmlAMTPhLMmV6VyEISVRPAnNRn"));
   }
}

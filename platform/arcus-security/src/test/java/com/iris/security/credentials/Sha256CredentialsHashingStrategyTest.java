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
package com.iris.security.credentials;

import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.lang.util.ByteSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Stored password hashes have to keep verifying across a Shiro upgrade. Shiro 2
 * reworked the hashing and {@code HashService} APIs and added the
 * {@code shiro-hashes-*} modules, but {@code Sha256CredentialsHashingStrategy}
 * still drives the same salted SHA-256 at 1024 iterations with base64-encoded
 * stored credentials. The hash and salt below were produced by the Shiro 1.13.0
 * build and are asserted as literals.
 */
public class Sha256CredentialsHashingStrategyTest {

   private static final String PASSWORD = "correct horse battery staple";

   /** Base64 of the 16 salt bytes 0x01..0x10, as stored in Cassandra. */
   private static final String SALT_BASE64 = "AQIDBAUGBwgJCgsMDQ4PEA==";

   /** Shiro 1.13.0: base64 of Sha256Hash(PASSWORD, salt, 1024). */
   private static final String HASH_BASE64 = "/WDKFqwbQSY7liVnGl90B6RE0PPBU365AAzfqpUiiDU=";

   private Sha256CredentialsHashingStrategy strategy;

   @Before
   public void setUp() {
      strategy = new Sha256CredentialsHashingStrategy();
   }

   @Test
   public void testHashOfKnownPasswordAndSaltIsUnchanged() {
      Assert.assertEquals(HASH_BASE64, strategy.hashCredentials(PASSWORD, strategy.saltAsBytes(SALT_BASE64)));
   }

   @Test
   public void testSaltRoundTripsThroughBase64() {
      ByteSource salt = strategy.saltAsBytes(SALT_BASE64);
      Assert.assertEquals(16, salt.getBytes().length);
      Assert.assertEquals(SALT_BASE64, strategy.saltAsString(salt));
   }

   /** A stored hash from the previous release must still authenticate. */
   @Test
   public void testMatcherAcceptsCredentialsStoredByShiro113() {
      SimpleAuthenticationInfo info =
            new SimpleAuthenticationInfo("user", HASH_BASE64.toCharArray(), "IrisRealm");
      info.setCredentialsSalt(strategy.saltAsBytes(SALT_BASE64));

      Assert.assertTrue(strategy.getCredentialsMatcher()
            .doCredentialsMatch(new UsernamePasswordToken("user", PASSWORD), info));
      Assert.assertFalse(strategy.getCredentialsMatcher()
            .doCredentialsMatch(new UsernamePasswordToken("user", "not the password"), info));
   }

   @Test
   public void testGeneratedSaltIsUsable() {
      ByteSource salt = strategy.generateSalt();
      Assert.assertNotNull(salt);
      Assert.assertTrue(salt.getBytes().length > 0);

      String hash = strategy.hashCredentials(PASSWORD, salt);
      SimpleAuthenticationInfo info = new SimpleAuthenticationInfo("user", hash.toCharArray(), "IrisRealm");
      info.setCredentialsSalt(strategy.saltAsBytes(strategy.saltAsString(salt)));
      Assert.assertTrue(strategy.getCredentialsMatcher()
            .doCredentialsMatch(new UsernamePasswordToken("user", PASSWORD), info));
   }

   @Test
   public void testIsSalted() {
      Assert.assertTrue(strategy.isSalted());
   }

   @Test
   public void testNullsAreTolerated() {
      Assert.assertNull(strategy.saltAsBytes(null));
      Assert.assertNull(strategy.hashCredentials(null, strategy.saltAsBytes(SALT_BASE64)));
      Assert.assertNull(strategy.hashCredentials(PASSWORD, null));
   }
}

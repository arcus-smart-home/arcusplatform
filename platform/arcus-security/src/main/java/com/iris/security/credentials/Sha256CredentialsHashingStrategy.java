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

import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.crypto.RandomNumberGenerator;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.util.ByteSource;

import com.google.inject.Singleton;

@Singleton
public class Sha256CredentialsHashingStrategy implements CredentialsHashingStrategy {

   private static final int ITERATIONS = 1024;

   private final CredentialsMatcher credentialsMatcher;

   public Sha256CredentialsHashingStrategy() {
      HashedCredentialsMatcher hashMatcher = new HashedCredentialsMatcher();
      hashMatcher.setHashAlgorithmName(Sha256Hash.ALGORITHM_NAME);
      hashMatcher.setHashIterations(ITERATIONS);
      hashMatcher.setStoredCredentialsHexEncoded(false);
      credentialsMatcher = hashMatcher;
   }

   @Override
   public boolean isSalted() {
      return true;
   }

   @Override
   public CredentialsMatcher getCredentialsMatcher() {
      return credentialsMatcher;
   }

   @Override
   public String saltAsString(ByteSource salt) {
      return salt.toBase64();
   }

   @Override
   public ByteSource saltAsBytes(String salt) {
      return salt == null ? null : ByteSource.Util.bytes(Base64.decode(salt));
   }

   @Override
   public String hashCredentials(String credentials, ByteSource salt) {
      if(credentials == null || salt == null) {
         return null;
      }

      return new Sha256Hash(credentials, salt, ITERATIONS).toBase64();
   }

   @Override
   public ByteSource generateSalt() {
      RandomNumberGenerator rng = new SecureRandomNumberGenerator();
      return rng.nextBytes();
   }
}


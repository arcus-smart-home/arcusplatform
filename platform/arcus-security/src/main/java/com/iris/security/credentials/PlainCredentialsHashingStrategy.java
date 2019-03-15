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
import org.apache.shiro.authc.credential.SimpleCredentialsMatcher;
import org.apache.shiro.util.ByteSource;

import com.google.inject.Singleton;

@Singleton
public class PlainCredentialsHashingStrategy implements CredentialsHashingStrategy {

   private final CredentialsMatcher credentialsMatcher;

   public PlainCredentialsHashingStrategy() {
      credentialsMatcher = new SimpleCredentialsMatcher();
   }

   @Override
   public boolean isSalted() {
      return false;
   }

   @Override
   public CredentialsMatcher getCredentialsMatcher() {
      return credentialsMatcher;
   }

   @Override
   public String saltAsString(ByteSource salt) {
      return null;
   }

   @Override
   public ByteSource saltAsBytes(String salt) {
      return null;
   }

   @Override
   public String hashCredentials(String credentials, ByteSource salt) {
      return credentials;
   }

   @Override
   public ByteSource generateSalt() {
      return null;
   }
}


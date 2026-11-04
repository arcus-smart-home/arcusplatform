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
package com.iris.security.passkey;

import java.time.Instant;
import java.util.UUID;

/**
 * A WebAuthn credential (passkey) registered to a person. Binary values are
 * stored base64url-encoded.
 */
public class Passkey {
   private UUID personId;
   private String credentialId;
   private String userHandle;
   private String publicKey;
   private long signCount;
   private String name;
   private Instant created;
   private Instant lastUsed;

   public UUID getPersonId() {
      return personId;
   }

   public void setPersonId(UUID personId) {
      this.personId = personId;
   }

   public String getCredentialId() {
      return credentialId;
   }

   public void setCredentialId(String credentialId) {
      this.credentialId = credentialId;
   }

   public String getUserHandle() {
      return userHandle;
   }

   public void setUserHandle(String userHandle) {
      this.userHandle = userHandle;
   }

   /** The credential's COSE public key */
   public String getPublicKey() {
      return publicKey;
   }

   public void setPublicKey(String publicKey) {
      this.publicKey = publicKey;
   }

   public long getSignCount() {
      return signCount;
   }

   public void setSignCount(long signCount) {
      this.signCount = signCount;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public Instant getCreated() {
      return created;
   }

   public void setCreated(Instant created) {
      this.created = created;
   }

   public Instant getLastUsed() {
      return lastUsed;
   }

   public void setLastUsed(Instant lastUsed) {
      this.lastUsed = lastUsed;
   }

   @Override
   public String toString() {
      return "Passkey [personId=" + personId + ", credentialId=" + credentialId + ", name=" + name + "]";
   }
}

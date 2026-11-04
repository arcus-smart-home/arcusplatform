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
package com.iris.core.dao;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.security.passkey.Passkey;

public interface PasskeyDAO {

   /**
    * @return false if the person already has a passkey with that credential id, in which
    *    case nothing is written
    */
   boolean save(Passkey passkey);

   List<Passkey> findByPerson(UUID personId);

   @Nullable
   Passkey findByCredentialId(String credentialId);

   /**
    * Records a login with the passkey. Only changes a passkey that still exists, so a
    * login racing a delete can't bring back part of the deleted row.
    *
    * @return false if the passkey no longer exists
    */
   boolean updateUsage(UUID personId, String credentialId, long signCount, Instant lastUsed);

   /**
    * @return false if the passkey was already gone, such as when two requests delete it
    *    at once
    */
   boolean delete(UUID personId, String credentialId);

   /** Deletes all of a person's passkeys, for when the person is deleted. */
   void deleteForPerson(UUID personId);

   /**
    * Stores an in-progress registration or login request (the WebAuthn options
    * sent to the browser) until the browser responds.
    *
    * @param personId the person registering a passkey, or null for a login
    */
   void saveRequest(String requestId, @Nullable UUID personId, String request, int ttlSeconds);

   /**
    * Returns and removes a stored request, so each one can only be completed once.
    */
   Optional<PasskeyRequest> takeRequest(String requestId);

   public static class PasskeyRequest {
      private final @Nullable UUID personId;
      private final String request;

      public PasskeyRequest(@Nullable UUID personId, String request) {
         this.personId = personId;
         this.request = request;
      }

      @Nullable
      public UUID getPersonId() {
         return personId;
      }

      public String getRequest() {
         return request;
      }
   }
}

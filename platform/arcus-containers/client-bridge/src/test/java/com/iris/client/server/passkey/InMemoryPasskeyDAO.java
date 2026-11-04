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
package com.iris.client.server.passkey;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.iris.core.dao.PasskeyDAO;
import com.iris.security.passkey.Passkey;

class InMemoryPasskeyDAO implements PasskeyDAO {
   final Map<String, Passkey> passkeys = new HashMap<>();
   final Map<String, PasskeyRequest> requests = new HashMap<>();

   @Override
   public boolean save(Passkey passkey) {
      return passkeys.putIfAbsent(passkey.getCredentialId(), passkey) == null;
   }

   @Override
   public List<Passkey> findByPerson(UUID personId) {
      return new ArrayList<>(passkeys.values().stream().filter(p -> p.getPersonId().equals(personId)).collect(Collectors.toList()));
   }

   @Override
   public Passkey findByCredentialId(String credentialId) {
      return passkeys.get(credentialId);
   }

   @Override
   public boolean updateUsage(UUID personId, String credentialId, long signCount, Instant lastUsed) {
      Passkey passkey = passkeys.get(credentialId);
      if (passkey == null) {
         return false;
      }
      passkey.setSignCount(signCount);
      passkey.setLastUsed(lastUsed);
      return true;
   }

   @Override
   public boolean delete(UUID personId, String credentialId) {
      return passkeys.remove(credentialId) != null;
   }

   @Override
   public void deleteForPerson(UUID personId) {
      passkeys.values().removeIf(passkey -> passkey.getPersonId().equals(personId));
   }

   @Override
   public void saveRequest(String requestId, UUID personId, String request, int ttlSeconds) {
      requests.put(requestId, new PasskeyRequest(personId, request));
   }

   @Override
   public Optional<PasskeyRequest> takeRequest(String requestId) {
      return Optional.ofNullable(requests.remove(requestId));
   }
}

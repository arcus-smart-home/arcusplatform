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
package com.iris.security.apikey;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ApiKey {

   private UUID id;
   private UUID placeId;
   private UUID personId;
   private UUID accountId;
   private String label;
   private String keyPrefix;
   private String keyHash;
   private final Set<String> permissions = new HashSet<>();
   private Instant created;
   private Instant lastUsed;
   private Instant expiresAt;

   public UUID getId() {
      return id;
   }

   public void setId(UUID id) {
      this.id = id;
   }

   public UUID getPlaceId() {
      return placeId;
   }

   public void setPlaceId(UUID placeId) {
      this.placeId = placeId;
   }

   public UUID getPersonId() {
      return personId;
   }

   public void setPersonId(UUID personId) {
      this.personId = personId;
   }

   public UUID getAccountId() {
      return accountId;
   }

   public void setAccountId(UUID accountId) {
      this.accountId = accountId;
   }

   public String getLabel() {
      return label;
   }

   public void setLabel(String label) {
      this.label = label;
   }

   public String getKeyPrefix() {
      return keyPrefix;
   }

   public void setKeyPrefix(String keyPrefix) {
      this.keyPrefix = keyPrefix;
   }

   public String getKeyHash() {
      return keyHash;
   }

   public void setKeyHash(String keyHash) {
      this.keyHash = keyHash;
   }

   public Set<String> getPermissions() {
      return Collections.unmodifiableSet(permissions);
   }

   public void setPermissions(Set<String> permissions) {
      this.permissions.clear();
      if (permissions != null) {
         this.permissions.addAll(permissions);
      }
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

   public Instant getExpiresAt() {
      return expiresAt;
   }

   public void setExpiresAt(Instant expiresAt) {
      this.expiresAt = expiresAt;
   }

   public boolean isExpired() {
      return expiresAt != null && Instant.now().isAfter(expiresAt);
   }

   @Override
   public String toString() {
      return "ApiKey [id=" + id + ", placeId=" + placeId + ", label=" + label
            + ", keyPrefix=" + keyPrefix + ", personId=" + personId
            + ", accountId=" + accountId + ", permissions=" + permissions
            + ", created=" + created + ", lastUsed=" + lastUsed
            + ", expiresAt=" + expiresAt + "]";
   }
}

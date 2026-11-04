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

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.iris.core.dao.PasskeyDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.messages.model.Person;
import com.iris.security.passkey.Passkey;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.exception.Base64UrlException;

/**
 * Exposes stored passkeys to the WebAuthn library. A person's WebAuthn user handle
 * is their person id; the username is their login email.
 */
class PasskeyCredentialRepository implements CredentialRepository {
   private final PasskeyDAO passkeyDao;
   private final PersonDAO personDao;

   PasskeyCredentialRepository(PasskeyDAO passkeyDao, PersonDAO personDao) {
      this.passkeyDao = passkeyDao;
      this.personDao = personDao;
   }

   static ByteArray userHandle(UUID personId) {
      ByteBuffer bytes = ByteBuffer.allocate(16);
      bytes.putLong(personId.getMostSignificantBits());
      bytes.putLong(personId.getLeastSignificantBits());
      return new ByteArray(bytes.array());
   }

   static Optional<UUID> personId(ByteArray userHandle) {
      if (userHandle == null || userHandle.size() != 16) {
         return Optional.empty();
      }
      ByteBuffer bytes = ByteBuffer.wrap(userHandle.getBytes());
      return Optional.of(new UUID(bytes.getLong(), bytes.getLong()));
   }

   @Override
   public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
      Person person = personDao.findByEmail(username);
      if (person == null) {
         return Collections.emptySet();
      }
      return passkeyDao.findByPerson(person.getId()).stream()
            .map(passkey -> PublicKeyCredentialDescriptor.builder().id(decode(passkey.getCredentialId())).build())
            .collect(Collectors.toSet());
   }

   @Override
   public Optional<ByteArray> getUserHandleForUsername(String username) {
      Person person = personDao.findByEmail(username);
      return person == null ? Optional.empty() : Optional.of(userHandle(person.getId()));
   }

   @Override
   public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
      return personId(userHandle)
            .map(personDao::findById)
            .map(Person::getEmail);
   }

   @Override
   public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
      Passkey passkey = passkeyDao.findByCredentialId(credentialId.getBase64Url());
      if (passkey == null || !userHandle.getBase64Url().equals(passkey.getUserHandle())) {
         return Optional.empty();
      }
      return Optional.of(toRegisteredCredential(passkey));
   }

   @Override
   public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
      Passkey passkey = passkeyDao.findByCredentialId(credentialId.getBase64Url());
      return passkey == null ? Collections.emptySet() : Collections.singleton(toRegisteredCredential(passkey));
   }

   private static RegisteredCredential toRegisteredCredential(Passkey passkey) {
      return RegisteredCredential.builder()
            .credentialId(decode(passkey.getCredentialId()))
            .userHandle(decode(passkey.getUserHandle()))
            .publicKeyCose(decode(passkey.getPublicKey()))
            .signatureCount(passkey.getSignCount())
            .build();
   }

   static ByteArray decode(String base64Url) {
      try {
         return ByteArray.fromBase64Url(base64Url);
      }
      catch (Base64UrlException e) {
         throw new IllegalStateException("Stored passkey value isn't base64url", e);
      }
   }
}

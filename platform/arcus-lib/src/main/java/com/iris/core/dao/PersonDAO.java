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

import java.util.UUID;
import java.util.stream.Stream;

import com.iris.core.dao.exception.EmailInUseException;
import com.iris.core.dao.exception.EmailMismatchException;
import com.iris.core.dao.exception.PinNotUniqueAtPlaceException;
import com.iris.messages.address.Address;
import com.iris.messages.model.Person;
import com.iris.platform.PagedQuery;
import com.iris.platform.PagedResults;
import com.iris.security.dao.AuthenticationDAO;

public interface PersonDAO extends AuthenticationDAO, UpdateFlag<UUID> {

   Person findByEmail(String email);

   /**
    * Creates a new person with a password
    * @param person
    * @param password
    * @return
    */
   Person create(Person person, String password) throws EmailInUseException;

   /**
    * Creates a new person that can not login.
    * @param person
    * @return
    */
   Person createPersonWithNoLogin(Person person);

   /**
    * Saves a person without modifying their password.
    * @param person
    * @return
    */
   Person update(Person person) throws EmailMismatchException;

   Person findById(UUID id);

   Person findByAddress(Address addr);

   void delete(Person entity);

   /**
    * @param email
    *    The login email
    * @param oldPassword
    *    The old password
    * @param newPassword
    *    The new password
    *
    * @return true if the update succeeded, false otherwise (oldPassword != stored password)
    */
   boolean updatePassword(String email, String oldPassword, String newPassword);

   String generatePasswordResetToken(String email);

   enum ResetPasswordResult { SUCCESS, FAILURE, TOKEN_FAILURE };
   ResetPasswordResult resetPassword(String email, String token, String password);

   Person updatePinAtPlace(Person person, UUID placeId, String pin) throws PinNotUniqueAtPlaceException;

   Person deletePinAtPlace(Person person, UUID placeId);

   /**
    * Updates the user object with any pending changes
    * when given the old email address.  This call *MUST* be
    * used for users with logins as it updates the login
    * information as well.
    * @param person
    *    The attributes to update
    * @param currentLoginEmail
    *    The current login email.
    * @return The mutated person
    */
   Person updatePersonAndEmail(Person person, String currentLoginEmail) throws EmailInUseException, EmailMismatchException;

   PagedResults<Person> listPersons(PersonQuery query);

   public Stream<Person> streamAll();

   public static class PersonQuery extends PagedQuery {}
}


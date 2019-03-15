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
package com.iris.core.dao.decorators;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.dao.exception.PinNotUniqueAtPlaceException;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.model.Person;
import com.iris.platform.PagedResults;
import com.iris.security.Login;

@Singleton
public class PersonSource implements PersonDAO {
   private final PersonDAO personDao;
   private final PersonPlaceAssocDAO personPlaceAssocDao;
   private final BeanAttributesTransformer<Person> personTransformer;

   @Inject
   public PersonSource(PersonDAO personDao, PersonPlaceAssocDAO personPlaceAssocDao, BeanAttributesTransformer<Person> personTransformer) {
      this.personDao = personDao;
      this.personPlaceAssocDao = personPlaceAssocDao;
      this.personTransformer = personTransformer;
   }

   public List<Map<String, Object>> listAttributesByPlace(UUID placeId, boolean filterHasLogin) {
      Set<UUID> personIds = personPlaceAssocDao.findPersonIdsByPlace(placeId);
      List<Map<String,Object>> persons = new LinkedList<>();
      for (UUID personId : personIds) {
         Person person = personDao.findById(personId);
         if (person != null) {
            persons.add(personTransformer.transform(person));
         }
      }

      if(filterHasLogin) {
         return persons.stream().filter((m) -> {
            return !StringUtils.isBlank((String) m.get(PersonCapability.ATTR_EMAIL)) || !StringUtils.isBlank((String) m.get(PersonCapability.ATTR_MOBILENUMBER));
         }).collect(Collectors.toList());
      }

      return persons;
   }

   /**
    * @param person
    * @param password
    * @return
    * @see com.iris.core.dao.PersonDAO#create(com.iris.messages.model.Person, java.lang.String)
    */
   public Person create(Person person, String password) {
      return personDao.create(person, password);
   }

   /**
    * @param person
    * @return
    * @see com.iris.core.dao.PersonDAO#createPersonWithNoLogin(com.iris.messages.model.Person)
    */
   public Person createPersonWithNoLogin(Person person) {
      return personDao.createPersonWithNoLogin(person);
   }

   /**
    * @param person
    * @return
    * @see com.iris.core.dao.PersonDAO#update(com.iris.messages.model.Person)
    */
   public Person update(Person person) {
      return personDao.update(person);
   }

   @Override
   public Person findById(UUID id) {
      return personDao.findById(id);
   }

   @Override
   public Person findByAddress(Address addr) {
      return personDao.findByAddress(addr);
   }

   @Override
   public void delete(Person entity) {
      personDao.delete(entity);
   }

   @Override
   public Login findLogin(String username) {
      return personDao.findLogin(username);
   }

   @Override
   public Person findByEmail(String email) {
      return personDao.findByEmail(email);
   }

   @Override
   public boolean updatePassword(String email, String oldPassword, String newPassword) {
      return personDao.updatePassword(email, oldPassword, newPassword);
   }

   @Override
   public String generatePasswordResetToken(String email) {
      return personDao.generatePasswordResetToken(email);
   }

   @Override
   public ResetPasswordResult resetPassword(String email, String token, String password) {
      return personDao.resetPassword(email, token, password);
   }

   @Override
   public Person updatePinAtPlace(Person person, UUID placeId, String pin) throws PinNotUniqueAtPlaceException
   {
      return personDao.updatePinAtPlace(person, placeId, pin);
   }

   @Override
   public Person deletePinAtPlace(Person person, UUID placeId)
   {
      return personDao.deletePinAtPlace(person, placeId);
   }

   @Override
   public Person updatePersonAndEmail(Person person, String newLoginEmail) {
      return personDao.updatePersonAndEmail(person, newLoginEmail);
   }

   @Override
   public void setUpdateFlag(UUID id, boolean updateFlag) {
      personDao.setUpdateFlag(id, updateFlag);
   }

   @Override
   public boolean getUpdateFlag(UUID id) {
      return personDao.getUpdateFlag(id);
   }

	@Override
	public PagedResults<Person> listPersons(PersonQuery query) {
		return personDao.listPersons(query);
	}

	@Override
	public Stream<Person> streamAll() {
		return personDao.streamAll();
	}

}


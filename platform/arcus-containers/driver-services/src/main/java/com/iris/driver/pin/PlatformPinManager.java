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
package com.iris.driver.pin;

import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.messages.model.Person;

public class PlatformPinManager extends AbstractPinManager {
   private final PersonDAO personDao;
   private final PersonPlaceAssocDAO personPlaceAssocDao;

   public PlatformPinManager(PersonDAO personDao, PersonPlaceAssocDAO personPlaceAssocDao) {
      super();
      this.personDao = personDao;
      this.personPlaceAssocDao = personPlaceAssocDao;
   }

   // package scope so the timeout can be decreased in unit test
   PlatformPinManager(PersonDAO personDao, PersonPlaceAssocDAO personPlaceAssocDao, long timeout) {
      super(timeout);
      this.personDao = personDao;
      this.personPlaceAssocDao = personPlaceAssocDao;
   }

   @Override
   protected String doGetPin(UUID placeId, UUID personId) {
      Person person = personDao.findById(personId);
      return person == null ? null : person.getPinAtPlace(placeId);
   }

   @Override
   protected UUID doValidatePin(UUID placeId, String pin) {
      if(placeId == null || StringUtils.isBlank(pin)) {
         return null;
      }

      Set<UUID> peopleWithAccess = personPlaceAssocDao.findPersonIdsByPlace(placeId);

      // TODO:  this will return the first person that matches the pin.  if there are multiple
      // people that have access to the home with the same pin there is no way to distinguish
      // who it is
      for(UUID personId : peopleWithAccess) {
         Person person = personDao.findById(personId);
         String thisPin = person.getPinAtPlace(placeId);
         if(thisPin != null && pin.endsWith(thisPin)) {
            return personId;
         }
      }
      return null;
   }
}


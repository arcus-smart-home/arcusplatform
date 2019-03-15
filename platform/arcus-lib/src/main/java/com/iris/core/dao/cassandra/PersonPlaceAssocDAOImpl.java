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
package com.iris.core.dao.cassandra;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.AuthorizationGrantDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.messages.type.PersonAccessDescriptor;
import com.iris.messages.type.PlaceAccessDescriptor;
import com.iris.security.authz.AuthorizationGrant;

@Singleton
public class PersonPlaceAssocDAOImpl implements PersonPlaceAssocDAO {
	private static final Logger log = LoggerFactory.getLogger(PersonPlaceAssocDAOImpl.class);

	private static final Timer listPlaceAccessForPersonTimer = DaoMetrics.readTimer(PersonPlaceAssocDAO.class, "listPlaceAccessForPerson");
	private static final Timer listPersonAccessForPlaceTimer = DaoMetrics.readTimer(PersonPlaceAssocDAO.class, "listPersonAccessForPlace");
	private static final Timer findPersonIdsByPlaceTimer = DaoMetrics.readTimer(PersonPlaceAssocDAO.class, "findPersonIdsByPlace");
	private static final Timer findPlaceIdsByPersonTimer = DaoMetrics.readTimer(PersonPlaceAssocDAO.class, "findPlaceIdsByPerson");
	
   private final AuthorizationGrantDAO grantDao;
   private final PlaceDAO placeDao;
   private final PersonDAO personDao;
   private final BeanAttributesTransformer<Person> personTransformer;


   @Inject
   public PersonPlaceAssocDAOImpl(AuthorizationGrantDAO grantDao, PlaceDAO placeDao,
         PersonDAO personDao, BeanAttributesTransformer<Person> personTransformer) {
      this.grantDao = grantDao;
      this.placeDao = placeDao;
      this.personDao = personDao;
      this.personTransformer = personTransformer;
   }
   

   @Override
   public List<PlaceAccessDescriptor> listPlaceAccessForPerson(UUID personId) {
      try(Context ctxt = listPlaceAccessForPersonTimer.time()) {
         List<AuthorizationGrant> grants = grantDao.findForEntity(personId);
         Map<UUID, Place> placesById = placeDao.findByPlaceIDIn(grants.stream().map(AuthorizationGrant::getPlaceId).collect(Collectors.toSet()))
               .stream().collect(Collectors.toMap(Place::getId, Function.identity()));

         ArrayList<PlaceAccessDescriptor> pads = new ArrayList<PlaceAccessDescriptor>();
         for (AuthorizationGrant g : grants) {
            Place p = placesById.get(g.getPlaceId());
            if (p == null) {
               log.warn("Null place access granted for [{}] ", personId);
               continue;
            }
            PlaceAccessDescriptor pad = new PlaceAccessDescriptor();
            pad.setCity(p.getCity());
            pad.setName(p.getName());
            pad.setPlaceId(String.valueOf(g.getPlaceId()));
            pad.setRole(g.getRole());
            pad.setPrimary(g.isAccountOwner() ? p.isPrimary() : false);
            pad.setState(p.getState());
            pad.setStreetAddress1(p.getStreetAddress1());
            pad.setStreetAddress2(p.getStreetAddress2());
            pad.setZipCode(p.getZipCode());
            pads.add(pad);
         }
         return pads;
      }
   }

   @Override
   public List<PersonAccessDescriptor> listPersonAccessForPlace(UUID placeId) {
      try(Context ctxt = listPersonAccessForPlaceTimer.time()) {
         List<AuthorizationGrant> grants = grantDao.findForPlace(placeId);
         return grants.stream()
               .map((g) -> {
                  Person p = personDao.findById(g.getEntityId());
                  if (p == null) {
                     log.warn("Error listPersonAccessForPlace for place {} and person {}", placeId, g.getEntityId());
                     return null;
                  } else {
                     PersonAccessDescriptor pad = new PersonAccessDescriptor();
                     pad.setPerson(personTransformer.transform(p));
                     pad.setRole(g.getRole());
                     return pad;
                  }
               })
               .filter(java.util.Objects::nonNull)
               .collect(Collectors.toList());
      }
   }

   @Override
   public Set<UUID> findPersonIdsByPlace(UUID placeId) {
      try(Context ctxt = findPersonIdsByPlaceTimer.time()) {
         List<AuthorizationGrant> grantsForPlace = grantDao.findForPlace(placeId);
         return grantsForPlace.stream().map(AuthorizationGrant::getEntityId).collect(Collectors.toSet());
      }
   }

   @Override
   public Set<UUID> findPlaceIdsByPerson(UUID personId) {
      try(Context ctxt = findPlaceIdsByPersonTimer.time()) {
         return 
            grantDao
               .findForEntity(personId)
               .stream()
               .filter(Objects::nonNull)
               .map(AuthorizationGrant::getPlaceId)
               .filter(Objects::nonNull)
               .collect(Collectors.toSet());
      }
   }

}


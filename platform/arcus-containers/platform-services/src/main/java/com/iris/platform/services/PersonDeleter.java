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
package com.iris.platform.services;

import java.util.List;
import java.util.UUID;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.AuthorizationGrantDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PreferencesDAO;
import com.iris.core.notification.Notifications;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Account;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.messages.services.PlatformConstants;
import com.iris.messages.type.EmailRecipient;
import com.iris.platform.person.InvitationHandlerHelper;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.security.authz.AuthorizationGrant;

@Singleton
public class PersonDeleter {

   private final AccountDAO accountDao;
   private final PersonDAO personDao;
   private final PlaceDAO placeDao;
   private final AuthorizationGrantDAO grantDao;
   private final PreferencesDAO preferencesDao;
   private final PlatformMessageBus bus;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public PersonDeleter(AccountDAO accountDao, PersonDAO personDao, PlaceDAO placeDao, AuthorizationGrantDAO grantDao,
   	PreferencesDAO preferencesDao, PlatformMessageBus bus, PlacePopulationCacheManager populationCacheMgr) {
      this.accountDao = accountDao;
      this.personDao = personDao;
      this.placeDao = placeDao;
      this.grantDao = grantDao;
      this.preferencesDao = preferencesDao;
      this.bus = bus;
      this.populationCacheMgr = populationCacheMgr;
   }

   public void removePersonFromPlace(UUID placeId, Person person, Address eventActor, boolean sendNotification) {
      Place p = placeDao.findById(placeId);
      if(p == null) {
         return;
      }
      removePersonFromPlace(p, person, eventActor, sendNotification);
   }

   public void removePersonFromPlace(Place place, UUID personId, Address eventActor, boolean sendNotification) {
      Person p = personDao.findById(personId);
      if(p == null) {
         return;
      }
      removePersonFromPlace(place, p, eventActor, sendNotification);
   }

   public void removePersonFromPlace(Place place, Person person, Address eventActor, boolean sendNotification) {
      Account account = accountDao.findById(place.getAccount());
      removePersonFromPlace(account, place, person, eventActor, sendNotification);  
   }
   
   public void removePersonFromPlace(Account account, Place place, Person person, Address eventActor, boolean sendNotification) {
	      // check for account null in the case that the account has been deleted concurrent to removing
	      // all of its dependencies or is a login that has no billable account
	      if(account != null && Objects.equal(account.getOwner(), person.getId())) {
	         throw new ErrorEventException("account.owner.deletion", "The account owner cannot be deleted without closing the account.");
	      }


	      List<AuthorizationGrant> grants = grantDao.findForEntity(person.getId());
	      if(grants.stream().noneMatch((g) -> { return g.getPlaceId().equals(place.getId()); })) {
	         throw new ErrorEventException(Errors.CODE_NOT_FOUND, "person " + person.getId() + " not found at " + place.getId());
	      }

	      preferencesDao.delete(person.getId(), place.getId());

	      // check if this is the final place the person is removed from.  - 1 because grants is prior
	      // to removal.  because hobbits can only be at one place this covers both guests and hobbits
	      if(grants.size() - 1 == 0) {
	         grantDao.removeGrantsForEntity(person.getId());
	         personDao.delete(person);
	         // TODO:  ITWO-5930:  Send email that their login has been removed if person.hasLogin() == true
	         emitDeletedEvent(person, place.getId(), place.getPopulation(), true, eventActor);
	         if(sendNotification) {
	       	  	sendNotification(person, place.getId(), place.getPopulation(), account, eventActor);
	         }
	         return;
	      }else {
	    	  //This is not the last place for this person
	    	  grantDao.removeGrant(person.getId(), place.getId());
	          clearPinForPlace(place.getId(), person, grants);
	          if(sendNotification) {
	        	  sendNotification(person, place.getId(), place.getPopulation(), account, eventActor);
	          }
	          
	          emitAuthorizationRemoved(person, place.getId(), place.getPopulation());
	          emitDeletedEvent(person, place.getId(), place.getPopulation(), false, eventActor);
	      }      
	   }

   private void sendNotification(Person context, UUID placeId, String population, Account account, Address eventActor) {
	   Person actor = null;
	   if(eventActor != null) {
		   actor = InvitationHandlerHelper.getActorFromAddress(eventActor, personDao);
	   }
	   boolean personRemovedSelf = false;
	   if(actor != null && Objects.equal(actor.getId(), context.getId())) {
		   personRemovedSelf = true;
	   }
	   if(personRemovedSelf) {
		   EmailRecipient recipient = new EmailRecipient();
		   recipient.setEmail(context.getEmail());
		   recipient.setFirstName(context.getFirstName());
		   recipient.setLastName(context.getLastName());
		   Notifications.sendEmailNotification(bus, recipient, placeId.toString(), population, Notifications.FullAccessPersonRemoved.KEY, ImmutableMap.<String, String>of(), Address.platformService(PlatformConstants.SERVICE_PLACES));		   
	   }else if(actor != null && placeId != null) {
		   Notifications.sendEmailNotification(bus, actor.getId().toString(), placeId.toString(), population, Notifications.PersonRemovedByOther.KEY, 
				   ImmutableMap.<String, String>of(Notifications.PersonRemovedByOther.PARAM_REMOVED_FIRSTNAME, Notifications.ensureNotNull(context.getFirstName()),
						   Notifications.PersonRemovedByOther.PARAM_REMOVED_LASTNAME, Notifications.ensureNotNull(context.getLastName())), 
				   Address.platformService(PlatformConstants.SERVICE_PLACES));		   		   
	   }
      if(account != null &&  (actor != null && !Objects.equal(actor.getId(), account.getOwner())) ) {
         //send notification to account owner
    	  if(personRemovedSelf) {
    		  Notifications.sendEmailNotification(bus, account.getOwner().toString(), placeId.toString(), population, Notifications.PersonLeft.KEY,
    	               ImmutableMap.<String, String>of(Notifications.PersonLeft.PARAM_SECONDARY_FIRSTNAME, Notifications.ensureNotNull(context.getFirstName())
    	                     ,Notifications.PersonLeft.PARAM_SECONDARY_LASTNAME, Notifications.ensureNotNull(context.getLastName())),
    	               Address.platformService(PlatformConstants.SERVICE_PLACES));
    	  }else {
    		  Notifications.sendEmailNotification(bus, account.getOwner().toString(), placeId.toString(), population, Notifications.PersonRemovedByOtherNotifyOwner.KEY,
   	               ImmutableMap.<String, String>of(Notifications.PersonRemovedByOtherNotifyOwner.PARAM_REMOVED_FIRSTNAME, Notifications.ensureNotNull(context.getFirstName())
   	                     ,Notifications.PersonRemovedByOtherNotifyOwner.PARAM_REMOVED_LASTNAME, Notifications.ensureNotNull(context.getLastName())
   	                     ,Notifications.PersonRemovedByOtherNotifyOwner.PARAM_ACTOR_FIRSTNAME, Notifications.ensureNotNull(actor.getFirstName())
   	                     ,Notifications.PersonRemovedByOtherNotifyOwner.PARAM_ACTOR_LASTNAME, Notifications.ensureNotNull(actor.getLastName())),
   	               Address.platformService(PlatformConstants.SERVICE_PLACES));
    	  }
         
      }
   }

   public void clearPinForPlace(UUID placeId, Person person, List<AuthorizationGrant> grants) {
      if(person == null) {
         return;
      }
      if(person.hasPinAtPlace(placeId)) {
         Person updatedPerson = personDao.deletePinAtPlace(person, placeId);
         MessageBody body = MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, ImmutableMap.of(PersonCapability.ATTR_PLACESWITHPIN, updatedPerson.getPlacesWithPin()));
         grants.forEach((g) -> {
            PlatformMessage evt = PlatformMessage.buildBroadcast(body, Address.fromString(updatedPerson.getAddress()))
                  .withPlaceId(g.getPlaceId())
                  .withPopulation(populationCacheMgr.getPopulationByPlaceId(g.getPlaceId()))
                  .create();
            bus.send(evt);
         });
      }
   }

   public void clearPinForPlace(UUID placeId, UUID personId) {
      clearPinForPlace(placeId, personDao.findById(personId), grantDao.findForEntity(personId));
   }
   
   public void clearPinForPlace(UUID placeId, Person person) {
      clearPinForPlace(placeId, person, grantDao.findForEntity(person.getId()));
   }

   private void emitAuthorizationRemoved(Person person, UUID placeId, String population) {
      MessageBody body = PersonCapability.AuthorizationRemovedEvent.builder().withPlaceId(placeId.toString()).build();
      PlatformMessage evt = PlatformMessage.buildBroadcast(body, Address.fromString(person.getAddress()))
            .withPlaceId(placeId)
            .withPopulation(population)
            .create();
      bus.send(evt);
   }

   private void emitDeletedEvent(Person person, UUID placeId, String population, boolean bootSession, Address actor) {
      MessageBody body = MessageBody.buildMessage(Capability.EVENT_DELETED, 
      		ImmutableMap.of("bootSession", bootSession, 
      				PersonCapability.ATTR_HASLOGIN, person.getHasLogin(),
      				PersonCapability.ATTR_OWNER, person.getOwner(),
      				PersonCapability.ATTR_FIRSTNAME, Notifications.ensureNotNull(person.getFirstName()),
      				PersonCapability.ATTR_LASTNAME, Notifications.ensureNotNull(person.getLastName())));
      PlatformMessage evt = PlatformMessage.buildBroadcast(body, Address.fromString(person.getAddress()))
            .withPlaceId(placeId)
            .withPopulation(population)
            .withActor(actor)
            .create();
      bus.send(evt);
   }
}


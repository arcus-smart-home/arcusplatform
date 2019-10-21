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
package com.iris.core.platform.handlers;

import static com.iris.messages.capability.PersonCapability.ATTR_CONSENTOFFERSPROMOTIONS;
import static com.iris.messages.capability.PersonCapability.ATTR_CONSENTSTATEMENT;
import static com.iris.messages.capability.PersonCapability.ATTR_EMAIL;
import static com.iris.messages.capability.PersonCapability.ATTR_FIRSTNAME;
import static com.iris.messages.capability.PersonCapability.ATTR_LASTNAME;
import static com.iris.messages.capability.PersonCapability.ATTR_MOBILENUMBER;
import static com.iris.messages.capability.PlaceCapability.ATTR_TZID;
import static com.iris.messages.capability.PlaceCapability.ATTR_NAME;
import static com.iris.messages.capability.PlaceCapability.ATTR_ZIPCODE;
import static com.iris.messages.capability.PlaceCapability.ATTR_ZIPPLUS4;
import static com.iris.messages.capability.PlaceCapability.ATTR_STATE;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.AuthorizationGrantDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.exception.EmailInUseException;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Account;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.service.AccountService.CreateAccountRequest;
import com.iris.platform.location.TimezonesChangeHelper;
import com.iris.platform.location.TimezonesManager;
import com.iris.security.authz.AuthorizationGrant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class CreateAccountHandler implements ContextualRequestMessageHandler<Account> {

   public static final String EMAIL_IN_USE_ERROR_CODE = "error.signup.emailinuse";
   public static final String EMAIL_IN_USE_ERROR_MSG = "This email address is already in use. Please Sign In.";
   public static final String ARGUMENT_ERROR = "missing.required.argument";
   public static final String ACCOUNT_NOT_NULL = "Account populated. ";
   public static final String MISSING_EMAIL = "Email address is required. ";
   public static final String MISSING_PASSWORD = "Password is required. ";
   public static final String DEFAULT_PLACE_HOME = "My Home";
   
   private static final Set<String> WRITE_ALLOWED_ATTRIBUTES_FOR_PLACE = ImmutableSet.of(
      ATTR_NAME,
      ATTR_ZIPCODE,
      ATTR_ZIPPLUS4,
      ATTR_TZID,
      ATTR_STATE,
      Capability.ATTR_TAGS);

   private static final Set<String> WRITE_ALLOWED_ATTRIBUTES_FOR_PERSON = ImmutableSet.of(
      ATTR_FIRSTNAME,
      ATTR_LASTNAME,
      ATTR_EMAIL,
      ATTR_MOBILENUMBER,
      ATTR_CONSENTOFFERSPROMOTIONS,
      ATTR_CONSENTSTATEMENT,
      Capability.ATTR_TAGS);
   
   private static final String ACCOUNT_RET = "account";
   private static final String PERSON_RET = "person";
   private static final String PLACE_RET = "place";

   private final AccountDAO accountDao;
   private final PersonDAO personDao;
   private final PlaceDAO placeDao;
   private final AuthorizationGrantDAO grantDao;
   private final BeanAttributesTransformer<Account> accountTransformer;
   private final BeanAttributesTransformer<Person> personTransformer;
   private final BeanAttributesTransformer<Place> placeTransformer;
   private final PlatformMessageBus bus;
   private final TimezonesChangeHelper timezoneHelper;

   @Inject(optional = true) @Named("default.service.level")
   private String defaultServiceLevel = "basic";

   @Inject
   public CreateAccountHandler(
         AccountDAO accountDao,
         PersonDAO personDao,
         PlaceDAO placeDao,
         AuthorizationGrantDAO grantDao,
         BeanAttributesTransformer<Account> accountTransformer,
         BeanAttributesTransformer<Person> personTransformer,
         BeanAttributesTransformer<Place> placeTransformer,
         PlatformMessageBus bus,
         TimezonesManager timezonesMgr) {

      this.accountDao = accountDao;
      this.personDao = personDao;
      this.placeDao = placeDao;
      this.grantDao = grantDao;
      this.accountTransformer = accountTransformer;
      this.personTransformer = personTransformer;
      this.placeTransformer = placeTransformer;
      this.bus = bus;
      this.timezoneHelper = new TimezonesChangeHelper(timezonesMgr);
   }

   @Override
   public String getMessageType() {
      return MessageConstants.MSG_CREATE_ACCOUNT;
   }

   @Override
   public MessageBody handleRequest(Account context, PlatformMessage msg) {
		return ErrorEvent.fromCode(ARGUMENT_ERROR, ACCOUNT_NOT_NULL);
   }

   @Override
   public MessageBody handleStaticRequest(PlatformMessage msg) {
      MessageBody request = msg.getValue();
      
      String email = CreateAccountRequest.getEmail(request);
      String password = CreateAccountRequest.getPassword(request);
      Boolean optin = Boolean.parseBoolean(CreateAccountRequest.getOptin(request));
      Map<String, Object> personAttributes = CreateAccountRequest.getPerson(request);
      Map<String, Object> placeAttributes = CreateAccountRequest.getPlace(request);

      if (Strings.isNullOrEmpty(email)) {
         throw new ErrorEventException(Errors.fromCode(ARGUMENT_ERROR, MISSING_EMAIL));
      }

      // FIXME:  check password and return an error event if it does not match requirements.  need
      // to have password checking algorithm in place first
      if (Strings.isNullOrEmpty(password)) {
         throw new ErrorEventException(Errors.fromCode(ARGUMENT_ERROR, MISSING_PASSWORD));
      }
      
      validatePersonAttributes(personAttributes);
      validatePlaceAttributes(placeAttributes);

      UUID placeId = UUID.randomUUID();
      UUID accountId= UUID.randomUUID();

      Person person = new Person();
      person.setAccountId(accountId);
      person.setCurrPlace(placeId);
      person.setEmail(email);
      person.setPrivacyPolicyAgreed(new Date());
      person.setTermsAgreed(new Date());
      person.setHasLogin(true);
      person.setOwner(true);
      if (optin) {
         person.setConsentOffersPromotions(new Date());
      }
      if(personAttributes != null && !personAttributes.isEmpty()) {
         personTransformer.merge(person, personAttributes);
      }

      try {
         person = personDao.create(person, password);
      }
      catch(EmailInUseException e) {
         throw new ErrorEventException( Errors.fromCode(EMAIL_IN_USE_ERROR_CODE, EMAIL_IN_USE_ERROR_MSG) );
      }

      boolean succeeded = false;
      Account account = null;
      Place place = null;
      try {
         account = new Account();
         account.setId(accountId);
         account.setState(Account.AccountState.SIGN_UP_1);
         account.setBillable(true);

         Set<UUID> placeIDs = new HashSet<>();
         placeIDs.add(placeId);
         account.setPlaceIDs(placeIDs);
         account.setOwner(person.getId());
         account = accountDao.create(account);

         place = new Place();
         place.setId(placeId);
         place.setName(DEFAULT_PLACE_HOME);
         if(placeAttributes != null && !placeAttributes.isEmpty()) {
            timezoneHelper.processTimeZoneChanges(placeAttributes);
            placeTransformer.merge(place, placeAttributes);            
         }
         place.setServiceLevel(ServiceLevel.fromString(defaultServiceLevel));
         place.setAccount(account.getId());
         place.setPrimary(true);
         place = placeDao.create(place);

         AuthorizationGrant defaultGrant = new AuthorizationGrant();
         defaultGrant.setAccountId(account.getId());
         defaultGrant.setAccountOwner(true);
         defaultGrant.setEntityId(person.getId());
         defaultGrant.setPlaceId(place.getId());
         defaultGrant.setPlaceName(place.getName());
         defaultGrant.addPermissions("*:*:*");

         grantDao.save(defaultGrant);
         emitEvent(account, place);
         succeeded = true;
      }
      finally {
         if(!succeeded) {
            personDao.delete(person);
            if(place != null && place.isPersisted()) {
               placeDao.delete(place);
            }
            if(account != null && account.isPersisted()) {
               accountDao.delete(account);
            }
         }
      }

      Map<String,Object> response = new HashMap<>();
      response.put(ACCOUNT_RET, accountTransformer.transform(account));
      response.put(PERSON_RET, personTransformer.transform(person));
      response.put(PLACE_RET, placeTransformer.transform(place));
      return MessageBody.buildResponse(request, response);
   }
   
   private void validatePlaceAttributes(Map<String, Object> placeAttributes)
   {
      validateWriteAllowedAttributes(placeAttributes, WRITE_ALLOWED_ATTRIBUTES_FOR_PLACE);
   }

   private void validatePersonAttributes(Map<String, Object> personAttributes)
   {
      validateWriteAllowedAttributes(personAttributes, WRITE_ALLOWED_ATTRIBUTES_FOR_PERSON);      
   }
   
   private void validateWriteAllowedAttributes(Map<String, Object> attributes, Set<String> writeAllowedAttributes)
   {
      if(attributes != null && !attributes.isEmpty()) {
         attributes.keySet().forEach(attribName -> {
           if(!writeAllowedAttributes.contains(attribName)) {
              throw new ErrorEventException(Errors.invalidParam(attribName));
           }
        });
      }      
   }

   //TODO - why we do not emit owner added event?
   private void emitEvent(Account account, Place place) {
	   //emit added event for the account
	   MessageBody body = MessageBody.buildMessage(Capability.EVENT_ADDED, accountTransformer.transform(account));
	   PlatformMessage event = PlatformMessage.buildBroadcast(body, Address.fromString(account.getAddress()))
            .withPlaceId(place.getId())
            .withPopulation(place.getPopulation())
            .create();
	   bus.send(event);		
	   
	   //emit added event for the place
	   MessageBody eventBodyForPlace = MessageBody.buildMessage(Capability.EVENT_ADDED, placeTransformer.transform(place));
	   PlatformMessage eventForPlace = PlatformMessage.buildBroadcast(eventBodyForPlace, Address.fromString(place.getAddress()))
            .withPlaceId(place.getId())
            .withPopulation(place.getPopulation())
            .create();
	   bus.send(eventForPlace);
   }

   
}


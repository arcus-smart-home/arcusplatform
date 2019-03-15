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
package com.iris.common.subsystem.doorsnlocks;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.iris.capability.util.Addresses;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DoorLockCapability;
import com.iris.messages.capability.DoorsNLocksSubsystemCapability;
import com.iris.messages.capability.DoorsNLocksSubsystemCapability.PersonAuthorizedEvent;
import com.iris.messages.capability.DoorsNLocksSubsystemCapability.PersonDeauthorizedEvent;
import com.iris.messages.capability.HubChimeCapability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.AccountModel;
import com.iris.messages.model.serv.PersonModel;
import com.iris.messages.model.subs.DoorsNLocksSubsystemModel;
import com.iris.messages.type.DoorChimeConfig;
import com.iris.messages.type.LockAuthorizationOperation;
import com.iris.messages.type.LockAuthorizationState;
import com.iris.util.TypeMarker;

class DoorsNLocksContextAdapter {

   private static final long TIMEOUT = 2 * 60 * 1000;
   // random number between 0 and this is added to the timeout to create some jitter in the key when
   // looking up an operation on a scheduled event to prevent getting stuck in the PENDING state
   private static final int TIMEOUT_JITTER = 2000;

   static final String OPDATA_PERSON = "person";
   static final String OPDATA_LOCK = "lock";
   static final String OPDATA_OPERATION = "op";
   static final String OPDATA_OPERATION_DELAY_AUTHORIZATION = "DELAY_AUTHORIZATION";
   static final String PERSON_ADDRESS_PREFIX = "SERV:" + PersonCapability.NAMESPACE + ":";

   private final SubsystemContext<DoorsNLocksSubsystemModel> context;
   private final Random rand = new Random();

   DoorsNLocksContextAdapter(SubsystemContext<DoorsNLocksSubsystemModel> context) {
      this.context = context;
   }

   Logger logger() {
      return context.logger();
   }

   String getPlaceId() {
      return context.getPlaceId().toString();
   }

   void deauthorize(String personId, Model lock) {
      deauthorize(personId, lock, false);
   }

   void deauthorize(String personId, Model lock, boolean timeout) {
      context.request(lock.getAddress(), DoorLockCapability.DeauthorizePersonRequest.builder().withPersonId(personId).build());
      context.setVariable(createOpKey(personId, lock), LockAuthorizationOperation.OPERATION_DEAUTHORIZE);
      if(timeout) {
         long timeoutMs = TIMEOUT + rand.nextInt(TIMEOUT_JITTER);
         Date timeoutTime = SubsystemUtils.setTimeout(timeoutMs, context, createTimeoutKey(personId, lock));
         context.setVariable(String.valueOf(timeoutTime.getTime()), createOpData(personId, lock, LockAuthorizationOperation.OPERATION_DEAUTHORIZE));
      }
   }

   void authorize(String person, Model lock) {
      context.request(lock.getAddress(), DoorLockCapability.AuthorizePersonRequest.builder().withPersonId(person).build());
      context.setVariable(createOpKey(person, lock), LockAuthorizationOperation.OPERATION_AUTHORIZE);
      long timeoutMs = TIMEOUT + rand.nextInt(TIMEOUT_JITTER);
      Date timeoutTime = SubsystemUtils.setTimeout(timeoutMs, context, createTimeoutKey(person, lock));
      context.setVariable(String.valueOf(timeoutTime.getTime()), createOpData(person, lock, LockAuthorizationOperation.OPERATION_AUTHORIZE));
   }

   void emitAuthorized(String person, Model lock) {
      MessageBody body = PersonAuthorizedEvent.builder()
            .withLock(lock.getAddress().getRepresentation())
            .withPerson(person)
            .build();

      context.broadcast(body);
   }

   void emitDeauthorized(String person, Model lock) {
      MessageBody body = PersonDeauthorizedEvent.builder()
            .withLock(lock.getAddress().getRepresentation())
            .withPerson(person)
            .build();

      context.broadcast(body);
   }

   private String createTimeoutKey(String personId, Model lock) {
      return lock.getAddress().getRepresentation() + "." + PERSON_ADDRESS_PREFIX + personId;
   }

   private String createOpKey(String personId, Model lock) {
      return "pending." + lock.getAddress().getRepresentation() + "." + PERSON_ADDRESS_PREFIX + personId;
   }

   private Map<String,String> createOpData(String personId, Model lock, String op) {
      return ImmutableMap.of(OPDATA_PERSON, PERSON_ADDRESS_PREFIX + personId, OPDATA_LOCK, lock.getAddress().getRepresentation(), OPDATA_OPERATION, op);
   }

   Map<String,String> removeOpData(long timeoutTime) {
      Map<String,String> opData = context.getVariable(String.valueOf(timeoutTime)).as(TypeMarker.mapOf(String.class));
      if(opData != null) {
         context.setVariable(String.valueOf(timeoutTime), null);
      }
      return opData;
   }

   String getOp(String person, Model lock) {
      return context.getVariable(createOpKey(person, lock)).as(TypeMarker.string());
   }

   String removeOp(String person, Model lock) {
      String op = getOp(person, lock);
      if(op != null) {
         context.setVariable(createOpKey(person, lock), null);
      }
      return op;
   }

   void clearPins(Model lock) {
      context.request(lock.getAddress(), DoorLockCapability.ClearAllPinsRequest.instance());
   }

   void chime(Model chime) {
      context.request(chime.getAddress(), getChimeMessage(chime));
   }

   void clear() {
      context.model().setAvailable(Boolean.FALSE);
      context.model().setChimeConfig(ImmutableSet.<Map<String,Object>>of());
      context.model().setState(SubsystemCapability.STATE_ACTIVE);
      clearDevices();
      clearPeople();
   }

   void clearDevices() {
      context.model().setContactSensorDevices(ImmutableSet.<String>of());
      context.model().setLockDevices(ImmutableSet.<String>of());
      context.model().setMotorizedDoorDevices(ImmutableSet.<String>of());
      context.model().setOfflineContactSensors(ImmutableSet.<String>of());
      context.model().setOfflineLocks(ImmutableSet.<String>of());
      context.model().setOfflineMotorizedDoors(ImmutableSet.<String>of());
      context.model().setOpenContactSensors(ImmutableSet.<String>of());
      context.model().setOpenMotorizedDoors(ImmutableSet.<String>of());
      context.model().setUnlockedLocks(ImmutableSet.<String>of());
      context.model().setWarnings(ImmutableMap.<String,String>of());
   }

   void clearPeople() {
      context.model().setAllPeople(ImmutableSet.<String>of());
      context.model().setAuthorizationByLock(ImmutableMap.<String,Set<Map<String,Object>>>of());
   }

   void commit() {
      context.commit();
   }

   DoorsNLocksSubsystemModel getModel() {
      return context.model();
   }

   Iterable<Model> getDevices() {
      return Iterables.filter(context.models().getModels(), DoorsNLocksPredicates.IS_DOORLOCK_DEVICE);
   }

   Iterable<Model> getLocks() {
      return Iterables.filter(context.models().getModels(), DoorsNLocksPredicates.IS_DOORLOCK);
   }

   Iterable<Model> getChimeDevices() {
      return Iterables.filter(context.models().getModels(), DoorsNLocksPredicates.IS_CHIMING_DEVICE);
   }

   boolean containsLock(String address) {
      return context.model().getLockDevices().contains(address);
   }

   Set<String> getContactSensorAddresses() {
      return new HashSet<>(context.model().getContactSensorDevices());
   }

   Model getAccountOwner() {
      Model account = Iterables.getFirst(context.models().getModelsByType(AccountModel.NAMESPACE), null);
      if(account == null) {
         logger().warn("Unable to determine account owner");
         return null;
      }

      Address ownerAddress = Address.fromString(Addresses.toObjectAddress(PersonCapability.NAMESPACE, AccountModel.getOwner(account)));
      return context.models().getModelByAddress(ownerAddress);
   }

   Iterable<Model> getPeople() {
      return Iterables.filter(context.models().getModels(), new Predicate<Model>() {
         @Override
         public boolean apply(Model arg0) {
            return DoorsNLocksPredicates.IS_PERSON.apply(arg0) &&
                  PersonModel.getPlacesWithPin(arg0) != null &&
                  PersonModel.getPlacesWithPin(arg0).contains(getPlaceId());
         }
      });
   }

   boolean addPerson(Model m) {
      return addToAddressSet(DoorsNLocksSubsystemCapability.ATTR_ALLPEOPLE, m.getAddress());
   }

   boolean removePerson(Model m) {
      return removeFromAddressSet(DoorsNLocksSubsystemCapability.ATTR_ALLPEOPLE, m.getAddress());
   }


   Set<String> getAllPeople() {
      return new HashSet<>(context.model().getAllPeople());
   }

   Model getModel(Address address) {
      return context.models().getModelByAddress(address);
   }

   Model getModel(String address) {
      return getModel(Address.fromString(address));
   }

   void updateAvailable() {
      boolean isAvailable =
            context.model().getLockDevices().size() > 0 ||
            context.model().getContactSensorDevices().size() > 0 ||
            context.model().getMotorizedDoorDevices().size() > 0 ||
            context.model().getPetDoorDevices().size() > 0;

      context.model().setAvailable(isAvailable);
   }

   boolean deviceExists(Address address) {
      String addr = address.getRepresentation();
      return context.model().getLockDevices().contains(addr) ||
             context.model().getContactSensorDevices().contains(addr) ||
             context.model().getMotorizedDoorDevices().contains(addr);
   }

   boolean addTotal(Model m) {
      String attribute = getTotalAttributeFor(m);
      return addToAddressSet(attribute, m.getAddress());
   }

   boolean removeTotal(Model m) {
      String attribute = getTotalAttributeFor(m);
      return removeFromAddressSet(attribute, m.getAddress());
   }

   boolean updateOffline(Model m) {
      if(DoorsNLocksPredicates.IS_ONLINE.apply(m)) {
         return removeOffline(m);
      }
      return addOffline(m);
   }

   boolean addOffline(Model m) {
      String attribute = getOfflineAttributeFor(m);
      return addToAddressSet(attribute, m.getAddress());
   }

   boolean removeOffline(Model m) {
      String attribute = getOfflineAttributeFor(m);
      return removeFromAddressSet(attribute, m.getAddress());
   }

   boolean updateOpen(Model m) {
      Predicate<Model> isOpen = DoorsNLocksPredicates.getOpenPredicateFor(m);
      if(isOpen.apply(m)) {
         return addOpen(m);
      }
      return removeOpen(m);
   }

   boolean addOpen(Model m) {
      String attribute = getOpenAttributeFor(m);
      return addToAddressSet(attribute, m.getAddress());
   }

   boolean removeOpen(Model m) {
      String attribute = getOpenAttributeFor(m);
      return removeFromAddressSet(attribute, m.getAddress());
   }

   void updateWarning(Model m, String warning) {
      String address = m.getAddress().getRepresentation();
      Map<String,String> warnings = new HashMap<>(context.model().getWarnings());
      String curWarning = warnings.get(address);
      if(!StringUtils.equals(warning, curWarning)) {
         if(warning == null) {
            warnings.remove(address);
         } else {
            warnings.put(address, warning);
         }

         context.model().setWarnings(Collections.unmodifiableMap(warnings));
      }
   }

   void removeWarning(Model m) {
      Map<String,String> warnings = new HashMap<>(context.model().getWarnings());
      if(warnings.remove(m.getAddress().getRepresentation()) != null) {
         context.model().setWarnings(Collections.unmodifiableMap(warnings));
      }
   }

   Set<LockAuthorizationState> getLockAuthorizations(Model m) {
      Map<String,Set<Map<String,Object>>> authorizationState = context.model().getAuthorizationByLock();
      Set<Map<String,Object>> authorizations = authorizationState.get(m.getAddress().getRepresentation());
      return transformAuthorizationState(authorizations);
   }

   void updateLockAuthorizations(Model lock, Set<LockAuthorizationState> authorizations) {
      Set<Map<String,Object>> transformed = transformAuthorizationStateToMaps(authorizations);
      Map<String,Set<Map<String,Object>>> authorizationState = new HashMap<>(context.model().getAuthorizationByLock());
      authorizationState.put(lock.getAddress().getRepresentation(), Collections.unmodifiableSet(transformed));
      context.model().setAuthorizationByLock(Collections.unmodifiableMap(authorizationState));
   }

   void removeLockAuthorizations(Address lockAddress) {
      Map<String,Set<Map<String,Object>>> authByLock = new HashMap<>(context.model().getAuthorizationByLock());
      if(authByLock.remove(lockAddress.getRepresentation()) != null) {
         context.model().setAuthorizationByLock(Collections.unmodifiableMap(authByLock));
      }
   }

   private Set<LockAuthorizationState> transformAuthorizationState(Set<Map<String,Object>> states) {
      Set<LockAuthorizationState> transform = new HashSet<>();
      if(states == null) {
         return transform;
      }
      for(Map<String,Object> state : states) {
         transform.add(new LockAuthorizationState(state));
      }
      return transform;
   }

   private Set<Map<String,Object>> transformAuthorizationStateToMaps(Set<LockAuthorizationState> states) {
      Set<Map<String,Object>> transform = new HashSet<>();
      if(states == null) {
         return transform;
      }
      for(LockAuthorizationState state : states) {
         transform.add(state.toMap());
      }
      return transform;
   }

   Set<DoorChimeConfig> getChimeConfig() {
      Set<DoorChimeConfig> config = new HashSet<>();
      for(Map<String,Object> cfg : context.model().getChimeConfig()) {
         config.add(new DoorChimeConfig(cfg));
      }
      return config;
   }

   DoorChimeConfig getChimeConfig(Model m) {
      Set<DoorChimeConfig> configs = getChimeConfig();
      for(DoorChimeConfig cfg : configs) {
         if(cfg.getDevice().equals(m.getAddress().getRepresentation())) {
            return cfg;
         }
      }
      return null;
   }

   void setChimeConfig(Set<DoorChimeConfig> config) {
      Set<Map<String,Object>> newConfig = new HashSet<>();
      for(DoorChimeConfig cfg : config) {
         newConfig.add(cfg.toMap());
      }
      context.model().setChimeConfig(Collections.unmodifiableSet(newConfig));
   }

   void addChimeConfig(DoorChimeConfig chimeConfig) {
      Set<Map<String,Object>> currentConfig = new HashSet<>(context.model().getChimeConfig());
      for(Map<String,Object> cfg : currentConfig) {
         if(chimeConfig.getDevice().equals(cfg.get(DoorChimeConfig.ATTR_DEVICE))) {
            return;
         }
      }
      currentConfig.add(chimeConfig.toMap());
      context.model().setChimeConfig(Collections.unmodifiableSet(currentConfig));
   }

   void removeChimeConfig(Address doorAddress) {
      Set<Map<String,Object>> currentConfig = new HashSet<>(context.model().getChimeConfig());
      Map<String,Object> toRemove = null;
      for(Map<String,Object> config : currentConfig) {
         if(doorAddress.getRepresentation().equals(config.get(DoorChimeConfig.ATTR_DEVICE))) {
            toRemove = config;
            break;
         }
      }

      if(toRemove != null) {
         currentConfig.remove(toRemove);
         context.model().setChimeConfig(Collections.unmodifiableSet(currentConfig));
      }
   }

   private boolean addToAddressSet(String attribute, Address address) {
      return addToAddressSet(attribute, address.getRepresentation());
   }

   private boolean addToAddressSet(String attribute, String address) {
      Set<String> addressSet = getAddressSet(attribute);
      if(addressSet.add(address)) {
         context.model().setAttribute(attribute, Collections.unmodifiableSet(addressSet));
         return true;
      }
      return false;
   }

   private boolean removeFromAddressSet(String attribute, Address address) {
      return removeFromAddressSet(attribute, address.getRepresentation());
   }

   private boolean removeFromAddressSet(String attribute, String address) {
      Set<String> addressSet = getAddressSet(attribute);
      if(addressSet.remove(address)) {
         context.model().setAttribute(attribute, Collections.unmodifiableSet(addressSet));
         return true;
      }
      return false;
   }

   @SuppressWarnings("unchecked")
   private Set<String> getAddressSet(String attribute) {
      Set<String> addressSet = new HashSet<>((Collection<String>)context.model().getAttribute(attribute));
      return addressSet;
   }

   private String getTotalAttributeFor(Model m) {
      if(DoorsNLocksPredicates.IS_DOORLOCK.apply(m)) {
         return DoorsNLocksSubsystemCapability.ATTR_LOCKDEVICES;
      }
      if(DoorsNLocksPredicates.IS_CONTACT.apply(m)) {
         return DoorsNLocksSubsystemCapability.ATTR_CONTACTSENSORDEVICES;
      }
      if(DoorsNLocksPredicates.IS_MOTORIZEDDOOR.apply(m)) {
         return DoorsNLocksSubsystemCapability.ATTR_MOTORIZEDDOORDEVICES;
      }
      throw new IllegalStateException("Doors & Locks does not support " + m);
   }

   private String getOfflineAttributeFor(Model m) {
      if(DoorsNLocksPredicates.IS_DOORLOCK.apply(m)) {
         return DoorsNLocksSubsystemCapability.ATTR_OFFLINELOCKS;
      }
      if(DoorsNLocksPredicates.IS_CONTACT.apply(m)) {
         return DoorsNLocksSubsystemCapability.ATTR_OFFLINECONTACTSENSORS;
      }
      if(DoorsNLocksPredicates.IS_MOTORIZEDDOOR.apply(m)) {
         return DoorsNLocksSubsystemCapability.ATTR_OFFLINEMOTORIZEDDOORS;
      }
      throw new IllegalStateException("Doors & Locks does not support " + m);

   }

   private String getOpenAttributeFor(Model m) {
      if(DoorsNLocksPredicates.IS_DOORLOCK.apply(m)) {
         return DoorsNLocksSubsystemCapability.ATTR_UNLOCKEDLOCKS;
      }
      if(DoorsNLocksPredicates.IS_CONTACT.apply(m)) {
         return DoorsNLocksSubsystemCapability.ATTR_OPENCONTACTSENSORS;
      }
      if(DoorsNLocksPredicates.IS_MOTORIZEDDOOR.apply(m)) {
         return DoorsNLocksSubsystemCapability.ATTR_OPENMOTORIZEDDOORS;
      }
      throw new IllegalStateException("Doors & Locks does not support " + m);
   }

   private MessageBody getChimeMessage(Model m) {
      if(DoorsNLocksPredicates.IS_HUB_CHIME.apply(m)) {
         return HubChimeCapability.chimeRequest.instance();
      }
      return KeyPadCapability.ChimeRequest.instance();
   }


   /**
      * Authorize the person with a delay
      * @param id
      * @param lock
      * @param delayMs
      */
   	void authorizeWithDelay(String person, Model lock, long delayMs) {
  		//String correlationId = context.request(lock.getAddress(), DoorLockCapability.AuthorizePersonRequest.builder().withPersonId(person).build());
   	    //context.setVariable(correlationId, person);
   	    Date timeoutTime = SubsystemUtils.setTimeout(delayMs, context, createTimeoutKey(person, lock));
   	    context.setVariable(String.valueOf(timeoutTime.getTime()), createOpData(person, lock, OPDATA_OPERATION_DELAY_AUTHORIZATION));
   	}

   	/**
   	 * Strip DoorsNLocksContextAdapter.PERSON_ADDRESS_PREFIX from person String and return the substring
   	 * @param person
   	 * @return
   	 */
   	String removePersonAddressPrefix(String person) {
   		if(StringUtils.isNoneBlank(person) && person.indexOf(PERSON_ADDRESS_PREFIX) > -1) {
   			return person.substring(PERSON_ADDRESS_PREFIX.length());
   		}else {
   			return person;
   		}
   	}

}


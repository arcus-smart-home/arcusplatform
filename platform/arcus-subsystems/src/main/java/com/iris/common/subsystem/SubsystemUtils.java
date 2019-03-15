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
package com.iris.common.subsystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.model.serv.AccountModel;
import com.iris.messages.model.serv.PlaceModel;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.model.subs.SubsystemModel;
import com.iris.model.predicate.Predicates;
import com.iris.util.IrisCollections;
import com.iris.util.TypeMarker;

public class SubsystemUtils {
   private static final Predicate<Model> isHub = Predicates.isA(HubCapability.NAMESPACE);
   public static final TypeMarker<Set<String>> TYPE_STRING_SET = TypeMarker.setOf(String.class);
   public static final String VAR_TIMEOUT = "timeout";

   public static final long TIMEOUT_JITTER_MS = 500;

   public static final String supportsNamespaceQueryString(String namespace) {
      Preconditions.checkNotNull(namespace);
      return "dev:caps contains '" + namespace + "'";
   }

   public static final String supportsAnyNamespaceQueryString(Collection<String> namespaces) {
      // make sure all entries are unique and non-null
      namespaces = IrisCollections.copyOfSet(namespaces);
      Preconditions.checkArgument(!namespaces.isEmpty(), "namespaces may not be empty or all null");
      return "dev:caps contains '" + StringUtils.join(namespaces, "' OR dev:caps contains '") + "'";
   }

   public static boolean isMatchingTimeout(ScheduledEvent event, SubsystemContext<?> context) {
      return isMatchingTimeout(event, context, VAR_TIMEOUT);
   }

   public static boolean isMatchingTimeout(ScheduledEvent event, SubsystemContext<?> context, String name) {
      Date expected = context.getVariable(name).as(Date.class);
      boolean matches = isMatchingTimeout(event, expected);
      if(matches){
         clearTimeout(context, name);
      }
      return matches;
   }

   /**
    * Attempts to determine if the given timeout is close enough to be considered
    * triggered by the given scheduled event.  This may allow a timeout to be triggered
    * slightly early.
    * @param event
    * @param scheduledTimestamp
    * @return
    */
   public static boolean isMatchingTimeout(ScheduledEvent event, Date expected) {
      if(event == null || expected == null) {
         return false;
      }
      long expectedTimestamp = expected.getTime();
      long actualTimestamp = event.getScheduledTimestamp();
      if(expectedTimestamp < actualTimestamp) {
         return true;
      }
      if(Math.abs(actualTimestamp - expectedTimestamp) < TIMEOUT_JITTER_MS) {
         return true;
      }
      return false;
   }

   public static Optional<Date> getTimeout(SubsystemContext<?> context) {
      return getTimeout(context, VAR_TIMEOUT);
   }

   public static Optional<Date> getTimeout(SubsystemContext<?> context, String key) {
      return Optional.fromNullable( context.getVariable(key).as(Date.class) );
   }
   
   public static Optional<Date> restoreTimeout(SubsystemContext<?> context) {
   	return restoreTimeout(context, VAR_TIMEOUT);
   }
   
   public static Optional<Date> restoreTimeout(SubsystemContext<?> context, String key) {
   	Optional<Date> timeout = getTimeout(context, key);
   	if(timeout.isPresent()) {
   		context.wakeUpAt(timeout.get());
   	}
   	return timeout;
   }

   public static Date setTimeout(long delayMs, SubsystemContext<?> context) {
      return setTimeout(delayMs, context, VAR_TIMEOUT);
   }

   public static Date setTimeout(long timeoutMs, SubsystemContext<?> context, String key) {
      Date wakeUp = new Date(System.currentTimeMillis() + timeoutMs);
      context.setVariable(key, wakeUp);
      context.wakeUpAt(wakeUp);
      return wakeUp;
   }
   public static Date setTimeout(Date wakeUp, SubsystemContext<?> context, String key) {
      context.setVariable(key, wakeUp);
      context.wakeUpAt(wakeUp);
      return wakeUp;
   }

   public static void clearTimeout(SubsystemContext<?> context) {
      // TODO also cancel the in-memory ref...
      clearTimeout(context, VAR_TIMEOUT);
   }

   public static void clearTimeout(SubsystemContext<?> context, String key) {
      context.setVariable(key, null);
   }

   @Nullable
	public static Address getAccountOwnerAddress(SubsystemContext<AlarmSubsystemModel> context) {
		Model account = context.models().getModelByAddress(Address.platformService(context.getAccountId(), AccountCapability.NAMESPACE));
		String ownerId = AccountModel.getOwner(account);
		if(StringUtils.isEmpty(ownerId)) {
			return null;
		}
		return Address.platformService(ownerId, PersonCapability.NAMESPACE);
		
	}
   
   @Nullable 
   public static String getAccountOwnerId(SubsystemContext<AlarmSubsystemModel> context) {
      Model account = context.models().getModelByAddress(Address.platformService(context.getAccountId(), AccountCapability.NAMESPACE));
      String ownerId = AccountModel.getOwner(account);
      if(StringUtils.isEmpty(ownerId)) {
         return null;
      }
      return ownerId;
   }
	
   public static void refreshTimeZoneOnContext(SubsystemContext<?> context){
      refreshTimeZoneOnContext(context, TimeZone.getDefault());
   }

   public static void refreshTimeZoneOnContext(SubsystemContext<?> context, TimeZone defaultTz)  {
      Model model = context.models().getModelByAddress(Address.platformService(context.getPlaceId(), PlaceCapability.NAMESPACE));
      String tzId = PlaceModel.getTzId(model);
      TimeZone tz = defaultTz;
      if (!StringUtils.isEmpty(tzId)) {
         try {
            tz = TimeZone.getTimeZone(tzId);
            context.setTimeZone(tz);
         }
         catch(Exception e) {
            context.logger().warn("Unable to load timezone [{}]", tzId, e);
         }
      }
   }
   public static boolean addStringToVariableList(String address, String variable, SubsystemContext<? extends SubsystemModel>context) {
      List<String>addressList=context.getVariable(variable).as(TypeMarker.listOf(String.class),new ArrayList<String>());
      if(addressList.contains(address)){
         return false;
      }
      List<String>newList=new ArrayList<String>(addressList);
      newList.add(address);
      context.setVariable(variable, newList);
      return true;
   }
   
   public static boolean removeStringFromVariableList(String address, String variable, SubsystemContext<? extends SubsystemModel>context) {
      List<String>list=context.getVariable(variable).as(TypeMarker.listOf(String.class),new ArrayList<String>());
      if(!list.contains(address)){
         return false;
      }
      List<String>newList=new ArrayList<String>(list);
      boolean removed = newList.remove(address);
      if(removed){
         context.setVariable(variable, newList);
      }
      return removed;
   }
   
   public static boolean isStringInVariableList(String address, String variable, SubsystemContext<? extends SubsystemModel>context) {
      List<String>list=context.getVariable(variable).as(TypeMarker.listOf(String.class),new ArrayList<String>());
      return list.contains(address);
   }
   
	public static Set<String> getSet(Model model, String attributeName) {
		return model.getAttribute(TYPE_STRING_SET, attributeName).or(ImmutableSet.<String>of());
	}
   
	public static Set<String> getAddresses(SubsystemContext<?> context, Predicate<? super Model> predicate) {
		Set<String> addresses = new HashSet<>();
		for(Model m: context.models().getModels(predicate)) {
			addresses.add(m.getAddress().getRepresentation());
		}
		return addresses;
	}
 
   public static boolean addToSet(Model model, String attributeName, String value) {
   	if(value == null) {
   		return false;
   	}
   	
   	Set<String> oldValues = getSet(model, attributeName);
      if(oldValues.contains(value)) {
         return false;
      }
      
      Set<String> updated = new HashSet<>(oldValues);
      updated.add(value);
      model.setAttribute(attributeName, updated);
      return true;
   }
   
   public static boolean removeFromSet(Model model, String attributeName, String value) {
      if(value == null) {
         return false;
      }
      
   	Set<String> oldValues = getSet(model, attributeName);
      if(!oldValues.contains(value)) {
         return false;
      }
      
      Set<String> updated = new HashSet<>(oldValues);
      updated.remove(value);
      model.setAttribute(attributeName, updated);
      return true;
   }

   public static <V> Map<String, V> getEditableMap(Map<String, V> value) {
      if(value == null) {
         return new HashMap<>();
      }
      else {
         return new HashMap<>(value);
      }
   }
   
   public static Map<String, Address> sendToHub(
         SubsystemContext<?> context,
         MessageBody message
   ) {
      return sendToHub(context, message, Optional.<Integer>absent());
   }

   public static Map<String, Address> sendToHub(
         SubsystemContext<?> context,
         MessageBody message,
         Optional<Integer> timeToLive
   ) {
      return sendTo(context, isHub, message, timeToLive);
   }

   /**
    * Sends the given message to all models which match
    * the given predicate.
    * @param context
    * @param p
    * @param message
    */
   public static Map<String, Address> sendTo(
         SubsystemContext<?> context,
         Predicate<Model> p, 
         MessageBody message
   ) {
      return sendTo(context, p, message, Optional.<Integer>absent());
   }
   
   public static Map<String, Address> sendTo(
         SubsystemContext<?> context,
         Predicate<Model> p, 
         MessageBody message,
         Optional<Integer> timeToLive
   ) {
      Map<String, Address> requests = new HashMap<String, Address>();
      for(Model model: context.models().getModels(p)) {
         String correlationId;
         if(timeToLive.isPresent()) {
            correlationId = context.request(model.getAddress(), message, timeToLive.get());
         }
         else {
            correlationId = context.request(model.getAddress(), message);
         }
         requests.put(correlationId, model.getAddress());
      }
      return requests;
   }

	public static boolean setIfNull(Model model, String attribute, Object value) {
      Object currentValue = model.getAttribute(attribute);
      if(currentValue == null) {
         model.setAttribute(attribute, value);
         return true;
      }
      else {
         return false;
      }
   }

	public static String getServiceLevel(SubsystemContext<?> context) {
		Object serviceLevel = context.models().getAttributeValue(Address.platformService(context.getPlaceId(), PlaceCapability.NAMESPACE), PlaceCapability.ATTR_SERVICELEVEL);
		if(serviceLevel == null) {
			return PlaceCapability.SERVICELEVEL_BASIC;
		}
		else {
			return String.valueOf(serviceLevel);
		}
	}
	
	public static Model getPlace(SubsystemContext<?> context) {
      return context.models().getModelByAddress(Address.platformService(context.getPlaceId(), PlaceCapability.NAMESPACE));
   }
	
	public static boolean isPremium(SubsystemContext<?> context) {
		return ServiceLevel.isPremiumOrPromon(getServiceLevel(context));
	}
	
	public static boolean isProMon(SubsystemContext<?> context) {
		return ServiceLevel.isPromon(getServiceLevel(context));
	}

}

